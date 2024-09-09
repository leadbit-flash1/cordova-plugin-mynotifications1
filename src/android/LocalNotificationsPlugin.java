package com.acrobaticgames.mynotifications1;

import androidx.work.*;
import java.util.concurrent.TimeUnit;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.InputStream;
import java.io.IOException;

import com.acrobaticgames.mynotifications1.NotificationWorker;

public class LocalNotificationsPlugin extends CordovaPlugin {

    private static final String CHANNEL_ID = "MyNotificationsChannel";
    private static final int PERMISSION_REQUEST_CODE = 123;
	private CallbackContext permissionCallbackContext;
	
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("hasPermission")) {
			this.hasPermission(callbackContext);
			return true;
		} else if (action.equals("requestPermission")) {
			this.requestPermission(callbackContext);
			return true;
		} else if (action.equals("schedule")) {
			this.schedule(args.getJSONObject(0), callbackContext);
			return true;
		} else if (action.equals("clearAll")) {
			this.clearAll(callbackContext);
			return true;
		}
		return false;
	}
	
	private void clearAll(CallbackContext callbackContext) {
		try {
			// Cancel all scheduled work
			WorkManager.getInstance(cordova.getActivity().getApplicationContext()).cancelAllWork();

			// Remove all notifications
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(cordova.getActivity());
			notificationManager.cancelAll();

			callbackContext.success(1);
		} catch (Exception e) {
			callbackContext.error("Failed to clear notifications: " + e.getMessage());
		}
	}

    private void hasPermission(CallbackContext callbackContext) {
        boolean granted = NotificationManagerCompat.from(cordova.getActivity()).areNotificationsEnabled();
        callbackContext.success(granted ? 1 : 0);
    }

	private void requestPermission(CallbackContext callbackContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.permissionCallbackContext = callbackContext;
            cordova.requestPermission(this, PERMISSION_REQUEST_CODE, Manifest.permission.POST_NOTIFICATIONS);
        } else {
            callbackContext.success(1);
        }
    }

	private void schedule(JSONObject options, CallbackContext callbackContext) throws JSONException {
        String title = options.getString("title");
        int seconds = options.getJSONObject("trigger").getInt("in");
        String contentText = options.optString("text", null);
        int notificationId = options.optInt("id", 0);
        String smallIcon = options.optString("smallIcon", DEFAULT_ICON);
        String largeIcon = options.optString("icon", null);

        createNotificationChannel();

        Data inputData = new Data.Builder()
            .putString("title", title)
            .putString("text", contentText)
            .putInt("notificationId", notificationId)
            .putString("smallIcon", smallIcon)
            .putString("largeIcon", largeIcon)
            .build();

        OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(NotificationWorker.class)
            .setInitialDelay(seconds, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build();

        WorkManager.getInstance(cordova.getActivity().getApplicationContext()).enqueue(notificationWork);

        callbackContext.success(1);
    }
	
	private Bitmap getLargeIcon(JSONObject options) {
		String icon = options.optString("icon", null);
		if (icon == null) return null;

		try {
			if (icon.startsWith("res://")) {
				String resourceName = icon.substring(6); // Remove "res://"
				resourceName = resourceName.substring(0, resourceName.lastIndexOf('.')); // Remove file extension
				int resourceId = cordova.getActivity().getResources().getIdentifier(resourceName, "drawable", cordova.getActivity().getPackageName());
				if (resourceId == 0) {
					resourceId = cordova.getActivity().getResources().getIdentifier(resourceName, "mipmap", cordova.getActivity().getPackageName());
				}
				if (resourceId != 0) {
					return BitmapFactory.decodeResource(cordova.getActivity().getResources(), resourceId);
				}
			} else {
				Uri uri = Uri.parse(icon);
				return getIconFromUri(uri);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static final String DEFAULT_ICON = "res://ic_launcher.png";

	private int getSmallIcon(JSONObject options) {
		String icon = options.optString("smallIcon", DEFAULT_ICON);
		
		if (icon.startsWith("res://")) {
			icon = icon.substring(6); // Remove "res://"
		}
		
		int resId = cordova.getActivity().getResources().getIdentifier(icon, "drawable", cordova.getActivity().getPackageName());

		if (resId == 0) {
			resId = cordova.getActivity().getResources().getIdentifier("ic_launcher", "mipmap", cordova.getActivity().getPackageName());
		}

		if (resId == 0) {
			resId = android.R.drawable.ic_popup_reminder;
		}

		return resId;
	}

	private Bitmap getIconFromUri(Uri uri) throws IOException {
		InputStream input = cordova.getActivity().getContentResolver().openInputStream(uri);
		if (input == null) return null;
		Bitmap bitmap = BitmapFactory.decodeStream(input);
		input.close();
		return bitmap;
	}

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Notifications Channel";
            String description = "Channel for My Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = cordova.getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

	@Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && this.permissionCallbackContext != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.permissionCallbackContext.success(1);
            } else {
				this.permissionCallbackContext.success(0);
                this.permissionCallbackContext.error("Permission denied");
            }
            this.permissionCallbackContext = null;
        }
    }
}
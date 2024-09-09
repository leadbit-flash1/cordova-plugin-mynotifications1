package com.acrobaticgames.mynotifications1;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.io.InputStream;

public class NotificationWorker extends Worker {
    private static final String CHANNEL_ID = "MyNotificationsChannel";
    private static final String DEFAULT_ICON = "res://ic_launcher.png";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = getInputData().getString("title");
        String text = getInputData().getString("text");
        int notificationId = getInputData().getInt("notificationId", 0);
        String smallIconName = getInputData().getString("smallIcon");
        String largeIconName = getInputData().getString("largeIcon");

        // Create an intent to open the app when the notification is clicked
        Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getApplicationContext().getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(getSmallIcon(smallIconName))
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        Bitmap largeIcon = getLargeIcon(largeIconName);
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(notificationId, builder.build());

        return Result.success();
    }

    private int getSmallIcon(String icon) {
        if (icon == null) icon = DEFAULT_ICON;
        
        if (icon.startsWith("res://")) {
            icon = icon.substring(6); // Remove "res://"
        }
        
        int resId = getApplicationContext().getResources().getIdentifier(icon, "drawable", getApplicationContext().getPackageName());

        if (resId == 0) {
            resId = getApplicationContext().getResources().getIdentifier("ic_launcher", "mipmap", getApplicationContext().getPackageName());
        }

        if (resId == 0) {
            resId = android.R.drawable.ic_popup_reminder;
        }

        return resId;
    }

    private Bitmap getLargeIcon(String icon) {
        if (icon == null) return null;

        try {
            if (icon.startsWith("res://")) {
                String resourceName = icon.substring(6); // Remove "res://"
                resourceName = resourceName.substring(0, resourceName.lastIndexOf('.')); // Remove file extension
                int resourceId = getApplicationContext().getResources().getIdentifier(resourceName, "drawable", getApplicationContext().getPackageName());
                if (resourceId == 0) {
                    resourceId = getApplicationContext().getResources().getIdentifier(resourceName, "mipmap", getApplicationContext().getPackageName());
                }
                if (resourceId != 0) {
                    return BitmapFactory.decodeResource(getApplicationContext().getResources(), resourceId);
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

    private Bitmap getIconFromUri(Uri uri) throws IOException {
        InputStream input = getApplicationContext().getContentResolver().openInputStream(uri);
        if (input == null) return null;
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        input.close();
        return bitmap;
    }
}
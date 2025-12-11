package com.acrobaticgames.mynotifications1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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
        Context context = getApplicationContext();
        
        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                return Result.failure();
            }
        }
        
        // Create channel before posting
        createNotificationChannel(context);

        String title = getInputData().getString("title");
        String text = getInputData().getString("text");
        int notificationId = getInputData().getInt("notificationId", 0);
        String smallIconName = getInputData().getString("smallIcon");
        String largeIconName = getInputData().getString("largeIcon");

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
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

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());

        return Result.success();
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Notifications Channel";
            String description = "Channel for My Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private int getSmallIcon(String icon) {
        if (icon == null) icon = DEFAULT_ICON;
        
        if (icon.startsWith("res://")) {
            icon = icon.substring(6);
        }
        
        // Remove file extension
        if (icon.contains(".")) {
            icon = icon.substring(0, icon.lastIndexOf('.'));
        }
        
        int resId = getApplicationContext().getResources().getIdentifier(icon, "drawable", getApplicationContext().getPackageName());

        if (resId == 0) {
            resId = getApplicationContext().getResources().getIdentifier(icon, "mipmap", getApplicationContext().getPackageName());
        }

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
                String resourceName = icon.substring(6);
                if (resourceName.contains(".")) {
                    resourceName = resourceName.substring(0, resourceName.lastIndexOf('.'));
                }
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
package org.apache.cordova.firebase;

import com.google.firebase.messaging.RemoteMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Map;

public class InfodiggMessagingService extends FirebasePluginMessagingService {

    private static final String TAG = "InfoDiggFCMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
		Log.d(TAG, "Infodigg Is Handling Messaging Service");
        if (remoteMessage.getNotification() != null) {
            super.onMessageReceived(remoteMessage);
        } else {
            SecureRandom random = new SecureRandom();
            int requestCode = random.nextInt();

            Bundle bundle = new Bundle();
            Map<String, String> data = remoteMessage.getData();
            for (String key : data.keySet()) {
                bundle.putString(key, data.get(key));
            }

            Intent intent = new Intent(this, OnNotificationOpenReceiver.class);
            intent.putExtras(bundle);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            String title = data.get("title");
            String body = data.get("message");
            String channelId = this.getStringResource("default_notification_channel_id");
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            notificationBuilder
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            //Sound
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                String sound = data.get("message");
                // Sound
                if (sound == null) {
                    Log.d(TAG, "Sound: none");
                } else if (sound.equals("default")) {
                    notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    Log.d(TAG, "Sound: default");
                } else {
                    Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/" + sound);
                    Log.d(TAG, "Sound: custom=" + sound + "; path=" + soundPath.toString());
                    notificationBuilder.setSound(soundPath);
                }
            }

            //Icon
            int iconID = getResources().getIdentifier("notify", "drawable", getPackageName());
            if (iconID != 0) {
                notificationBuilder.setSmallIcon(iconID);
            } else {
                notificationBuilder.setSmallIcon(getApplicationInfo().icon);
            }

            // Color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
		int colorID = getResources().getIdentifier("my_accent_color", "color", getPackageName());
		if (colorID == 0) {
			colorID = getResources().getIdentifier("accent", "color", getPackageName());
		}
                notificationBuilder.setColor(getResources().getColor(colorID, null));
            }

            //Image
            if(data.get("image") != null){
                Bitmap bitmap = getBitmapFromURL(data.get("image"));
                if(bitmap != null){
                    notificationBuilder.setLargeIcon(bitmap);
                }
            }

            //Badge
            if(data.get("badge") != null) {
                try {
                    notificationBuilder.setNumber(Integer.parseInt(data.get("badge")));
                } catch (Exception e) {
                    Log.d(TAG, "Error on set number notification");
                }
            }

            // Visibility
            int iVisibility = NotificationCompat.VISIBILITY_PUBLIC;
            Log.d(TAG, "Visibility: " + iVisibility);
            notificationBuilder.setVisibility(iVisibility);

            // Priority
            int iPriority = NotificationCompat.PRIORITY_MAX;
            Log.d(TAG, "Priority: " + iPriority);
            notificationBuilder.setPriority(iPriority);

            // Build notification
            Notification notification = notificationBuilder.build();

            // Display notification
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Log.d(TAG, "show notification: "+notification.toString());
            notificationManager.notify(requestCode, notification);

            // Send to plugin
            FirebasePlugin.sendMessage(bundle, this.getApplicationContext());
        }
    }

    public Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getStringResource(String name) {
        return this.getString(this.getResources().getIdentifier(name, "string", this.getPackageName()));
    }

}

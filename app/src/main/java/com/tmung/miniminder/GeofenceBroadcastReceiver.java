package com.tmung.miniminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "geofence_channel";
    private static final int NOTIFICATION_ID = 123; // Unique ID for the notification

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            String errorMessage = GeofenceErrorMessages.getErrorString(context, errorCode);
            // display error
            displayNotification(context, "Geofence Error", errorMessage);
        } else {
            for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                String requestId = geofence.getRequestId();
                // display notification (or send new coodrinates to server)
                displayNotification(context, "Geofence Triggered", "Geofence " +
                        "triggered for: " + requestId);
            }
        }

        // Handle Geofence Transitions:
        // Could put this in the 'else' statement above
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // The child has entered the geofenced area
            Toast.makeText(context, "Child entered the geofenced area", Toast.LENGTH_SHORT).show();
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // The child has exited the geofenced area
            Toast.makeText(context, "Child has exited the geofenced area", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Geofence Channel", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}

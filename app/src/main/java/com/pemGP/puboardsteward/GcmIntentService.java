package com.pemGP.puboardsteward;

/**
 * Created by Florian on 09.08.2014.
 */
import static com.pemGP.puboardsteward.CommonUtilities.displayMessage;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GcmIntentService extends IntentService {

    @SuppressWarnings("hiding")
    private static final String TAG = "PuboardSteward.GCMIntentService";

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;


    public GcmIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
               onError(getApplicationContext(), extras.toString());

            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                onDeletedMessages(getApplicationContext(),extras.toString());

                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                onMessage(getApplicationContext(),extras);
            }

        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }


    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.

    protected void onMessage(Context context, Bundle extras) {
        Log.i(TAG, "Received message");

        String message = extras.getString("message");
        if (message == null)
            message = getString(R.string.gcm_message);
        // data needs push into main activity to do further processing

        //TODO: no refresh if JSON object inside
        displayMessage(context, message);
        // notifies user (if needed)
        // todo: switch using options
        if (message.startsWith("[{"))
            sendNotification("Incoming Transaction");
        else
            sendNotification(message);
    }

    protected void onDeletedMessages(Context context, String extra) {
        Log.i(TAG, "Received deleted messages notification");
        String message = "Deleted messages on server: "+extra;
        //Refresh
        displayMessage(context, message);
        // notifies user
        sendNotification(message);
    }

    public void onError(Context context, String errorId) {
        Log.i(TAG, "Received error: " + errorId);
        // Refresh
        displayMessage(context, getString(R.string.gcm_error, errorId));
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    private void sendNotification(String msg) {
        // get sound to play upon gcm
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if(alarmSound == null){
            alarmSound = RingtoneManager.getDefaultUri(Notification.DEFAULT_SOUND);
            if(alarmSound == null){
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, NavDrawActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_gcm)
                        .setSound(alarmSound)
                        .setContentTitle("PuboardSteward")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}

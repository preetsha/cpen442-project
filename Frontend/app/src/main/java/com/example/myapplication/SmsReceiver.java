package com.example.myapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashSet;
import java.util.Set;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG =
            SmsReceiver.class.getSimpleName();
    // Get the object of SmsManager
    final SmsManager sms = SmsManager.getDefault();
    public static final String NOTIFICATION_CHANNEL_ID = "10001";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {

            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                SharedPreferences pref = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
                if (pref.getString("UUID", "").isEmpty() || pref.getString("SECRET", "").isEmpty()) {
                    return;
                }

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getOriginatingAddress();
                    String displayName = SMSContacts.getContactbyPhoneNumber(context, phoneNumber);

                    if (displayName.isEmpty()) {
                        displayName = phoneNumber;
                    }

                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReceiver", "senderNum: " + senderNum + "; message: " + message);

                    //TODO: read settings here
                    //TODO: improve logic here
                    SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
                    String cachedValue = SMSContacts.getCachedValue(preferences, senderNum);

                    ContactDataModel.Level level;
                    if (SMSContacts.isInternetAvailable(context)) {
                        if (!cachedValue.isEmpty()) {
                            level = SMSContacts.getLevelFromCachedValue(cachedValue);
                        } else {
                            // TODO: check if key is valid
                            if (!preferences.getString("UUID", "").isEmpty() && !preferences.getString("SESSION_KEY", "").isEmpty()) {
                                level = SMSContacts.computeTrustScore(context, phoneNumber);
                            } else {
                                level = ContactDataModel.Level.REGULAR;
                            }
                            level = SMSContacts.getLevelFromCachedValue(cachedValue);
                        }
                    } else {
                        if (!cachedValue.isEmpty()) {
                            level = SMSContacts.getLevelFromCachedValue(cachedValue);
                        } else {
                            level = ContactDataModel.Level.REGULAR;

                            Set<String> regularList = preferences.getStringSet(SMSContacts.cacheRegularKey, new HashSet<>());
                            regularList.add(phoneNumber);
                            preferences.edit().remove(SMSContacts.cacheRegularKey).apply();
                            preferences.edit().putStringSet(SMSContacts.cacheRegularKey, regularList).apply();
                        }
                    }

                    if (level == ContactDataModel.Level.SPAM) {
                        return;
                    }


                    // Create an explicit intent for an Activity in your app
                    Intent splashIntent = new Intent(context.getApplicationContext(), SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, splashIntent, 0);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(displayName)
                            .setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            // Set the intent that will fire when the user taps the notification
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context.getApplicationContext());

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify((int) SystemClock.uptimeMillis(), builder.build());

                } // end for loop
            } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" + e);

        }
    }
}

package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class SplashActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: Check for condition we are in (check for existing shared secret or cache) otherwise prompt registration
        // TODO: Establish session key otherwise prompt registration
        int permissionCheckReadSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        int permissionCheckReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);

        if (permissionCheckReadSms == PackageManager.PERMISSION_GRANTED && permissionCheckReadContacts == PackageManager.PERMISSION_GRANTED) {
            SMSContacts.setContactList(populateSMSGroups());

            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS}, 100);
        }
        // TODO: Recalculate data structures on incoming notifications
        finish();
    }

    public String getContactbyPhoneNumber(Context c, String phoneNumber) {

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = c.getContentResolver().query(uri, projection, null, null, null);
        String name = "";

        if (cursor == null) {
            return name;
        } else {
            try {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                }

            } finally {
                cursor.close();
            }
            return name;
        }
    }

    public ArrayList<ContactDataModel> populateSMSGroups() {
        ContentResolver cr = getApplicationContext().getContentResolver();

        // TODO: Can probably query for distinct address to save time
        Cursor cur = cr.query(Uri.parse("content://sms"),
                new String[]{"DISTINCT thread_id", "address", "person", "body", "date"}, "thread_id IS NOT NULL) GROUP BY (thread_id", null, Telephony.Sms.DEFAULT_SORT_ORDER);
        ArrayList<ContactDataModel> contacts = new ArrayList<>();
        ArrayList<String> seenThreads = new ArrayList<>();

        try {
            while (cur.moveToNext()) {
                //String snippet = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET));
                String threadId = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID));
                if (seenThreads.contains(threadId)) {
                    continue;
                }
                String address = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                long dateLong = cur.getLong(cur.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String body = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.BODY));
                //int person = cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.PERSON));
                if (body.length() > 45) {
                    body = body.substring(0, 42) + "...";
                }

                ContactDataModel contact = new ContactDataModel(address, threadId, body, dateLong);
                String displayName = getContactbyPhoneNumber(getApplicationContext(), address);
                if (!displayName.isEmpty()) {
                    contact.setDisplayName(displayName);
                    contact.setPriority(ContactDataModel.Level.PRIORITY);
                } else {
                    contact.setPriority(ContactDataModel.Level.REGULAR);// TODO: CALCULATE PRIORITY HERE using server or if known contact
                }

                contacts.add(contact);
                seenThreads.add(threadId);

            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return contacts;
    }
}

package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SMSContacts {
    public static ArrayList<ContactDataModel> contactList;

    public SMSContacts() {
    }

    public static List<ContactDataModel> getContactList() {
        return contactList;
    }

    public static List<ContactDataModel> getContactsByInbox(ContactDataModel.Level level) {
        return contactList.stream().filter(c -> c.getPriority() == level).collect(Collectors.toList());
    }

    public static void setContactList(ArrayList<ContactDataModel> contactList) {
        SMSContacts.contactList = contactList;
    }

    public static String getContactbyPhoneNumber(Context c, String phoneNumber) {

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

    public static int getContactIndexByThread(String thread) {
        for (int i = 0; i < contactList.size(); i++) {
            final ContactDataModel c = contactList.get(i);
            if (c.getThreadId().equals(thread)) {
                return i;
            }
        }
        return -1;
    }

    public static ArrayList<ContactDataModel> populateSMSGroups(Context context) {
        ContentResolver cr = context.getContentResolver();

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

                ContactDataModel contact = new ContactDataModel(address, threadId, body, dateLong);
                String displayName = SMSContacts.getContactbyPhoneNumber(context, address);
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

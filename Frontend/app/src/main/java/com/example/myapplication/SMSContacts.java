package com.example.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

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
}

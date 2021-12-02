package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SMSContacts {
    public static ArrayList<ContactDataModel> contactList;
    public static final String cacheTrustedKey = "trustedList";
    public static final String cacheRegularKey = "regularList";
    public static final String cacheSpamKey = "spamList";

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

    public static int getContactIndexByNumber(String thread) {
        for (int i = 0; i < contactList.size(); i++) {
            final ContactDataModel c = contactList.get(i);
            if (c.getThreadId().equals(thread)) {
                return i;
            }
        }
        return -1;
    }

    public static void populateSMSGroups(Context context) {
        ContentResolver cr = context.getContentResolver();

        SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        Cursor cur = cr.query(Uri.parse("content://sms"),
                new String[]{"DISTINCT thread_id", "address", "person", "body", "date"}, "thread_id IS NOT NULL) GROUP BY (thread_id", null, Telephony.Sms.DEFAULT_SORT_ORDER);
        contactList = new ArrayList<>();
        ArrayList<String> seenThreads = new ArrayList<>();
        Set<String> trustedList = new HashSet<>();
        Set<String> regularList = new HashSet<>();
        Set<String> spamList = new HashSet<>();

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
                String cachedValue = SMSContacts.getCachedValue(preferences, address);

                if (!displayName.isEmpty()) {
                    contact.setDisplayName(displayName);
                }

                seenThreads.add(threadId);

                if (isInternetAvailable(context)) {
                    if (!cachedValue.isEmpty()) { // In cache
                        String serverExpectedValue = "UNKNOWN"; // TODO: THOMAS (returns "TRUSTED", "SPAM", or "UNKNOWN")
                        if (!displayName.isEmpty()) { // If is contact
                            if (!cachedValue.equals("TRUSTED")) { // number has been changed to contact offline
                                markAsTrusted(address, context);
                            }
                            contact.setPriority(ContactDataModel.Level.PRIORITY);
                        } else { // Non-contact number
                            if (!serverExpectedValue.equals(cachedValue)) {
                                // Number's inbox has been changed offline
                                if (cachedValue.equals("TRUSTED")) {
                                    markAsTrusted(address, context);
                                } else {
                                    markAsSpam(address, context);
                                }
                            }
                            contact.setPriority(getLevelFromCachedValue(cachedValue)); // Inbox = cachedValue
                        }
                    } else { // Not in cache
                        contact.setPriority(computeTrustScore(preferences, address));
                    }
                } else {
                    if (!displayName.isEmpty()) {
                        contact.setPriority(ContactDataModel.Level.PRIORITY);
                    } else if (!cachedValue.isEmpty()) {
                        contact.setPriority(getLevelFromCachedValue(cachedValue));
                    } else {
                        contact.setPriority(ContactDataModel.Level.REGULAR);
                    }
                }

                contactList.add(contact);
                seenThreads.add(threadId);

                switch (contact.getPriority()) {
                    case PRIORITY:
                        trustedList.add(address);
                        break;
                    case REGULAR:
                        regularList.add(address);
                        break;
                    case SPAM:
                        spamList.add(address);
                        break;
                }

            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        preferences.edit().remove(cacheTrustedKey).apply();
        preferences.edit().remove(cacheRegularKey).apply();
        preferences.edit().remove(cacheSpamKey).apply();
        preferences.edit().putStringSet(cacheTrustedKey, trustedList).apply();
        preferences.edit().putStringSet(cacheRegularKey, regularList).apply();
        preferences.edit().putStringSet(cacheSpamKey, spamList).apply();
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private static String getCachedValue(SharedPreferences sharedPreferences, String number) {
        if (sharedPreferences.getStringSet(cacheTrustedKey, new HashSet<String>()).contains(number)) {
            return "TRUSTED";
        } else if (sharedPreferences.getStringSet(cacheRegularKey, new HashSet<String>()).contains(number)) {
            return "UNKNOWN";
        } else if (sharedPreferences.getStringSet(cacheSpamKey, new HashSet<String>()).contains(number)) {
            return "SPAM";
        } else {
            return "";
        }
    }

    private static ContactDataModel.Level getLevelFromCachedValue(String cachedValue) {
        if (cachedValue.equals("TRUSTED")) {
            return ContactDataModel.Level.PRIORITY;
        } else if (cachedValue.equals("SPAM")) {
            return ContactDataModel.Level.SPAM;
        }
        return ContactDataModel.Level.REGULAR;
    }

    private static ContactDataModel.Level computeTrustScore(SharedPreferences sharedPreferences, String number) {
        //TODO: getScore() which returns Level
        return ContactDataModel.Level.REGULAR;
    }

    public static void markAsTrusted(String phoneNumber, Context context) {
        String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
        String route = "/user/trust";
        String url = root + route;
        JSONObject jsonBody = new JSONObject();
        SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        try {
            jsonBody.put("uuid", preferences.getString("UUID", ""));
            jsonBody.put("phone", phoneNumber);
        } catch (Exception e) {
            Log.d("Mark as trusted", "JSON body put error");
        }
        // Request a string response from the provided URL.
        String requestBody = jsonBody.toString();
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Mark as trusted", "onErrorResponse: ", error);
                Toast.makeText(context, "Could not communicate with server", Toast.LENGTH_SHORT).show();
            }
        }
        );

        // Add the request to the RequestQueue.
        QueueSingleton.getInstance(context).addToRequestQueue(jsonRequest);
    }

    public static void markAsSpam(String phoneNumber, Context context) {
        String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
        String route = "/user/spam";
        String url = root + route;
        JSONObject jsonBody = new JSONObject();
        SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        try {
            jsonBody.put("uuid", preferences.getString("UUID", ""));
            jsonBody.put("phone", phoneNumber);
        } catch (Exception e) {
            Log.d("Mark as spam", "JSON body put error");
        }
        // Request a string response from the provided URL.
        String requestBody = jsonBody.toString();
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Mark as spam", "onErrorResponse: ", error);
                Toast.makeText(context, "Could not communicate with server", Toast.LENGTH_SHORT).show();
            }
        }
        );

        // Add the request to the RequestQueue.
        QueueSingleton.getInstance(context).addToRequestQueue(jsonRequest);
    }

}

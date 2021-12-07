package com.example.myapplication;

import static okhttp3.internal.Internal.logger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class SMSContacts {
    public static ArrayList<ContactDataModel> contactList;
    public static final String cacheTrustedKey = "trustedList";
    public static final String cacheRegularKey = "regularList";
    public static final String cacheSpamKey = "spamList";
    public static String responseVar;
    public static final OkHttpClient client = new OkHttpClient.Builder().addNetworkInterceptor(new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Request request = chain.request();

            long t1 = System.nanoTime();
            logger.info(String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));

            okhttp3.Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            logger.info(String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            return response;
        }
    }).build();

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
        boolean internet = isInternetAvailable(context);

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

                if (internet) {
                    if (!cachedValue.isEmpty()) { // In cache
                        String serverExpectedValue = getServerExpectedValue(address, context);
                        Log.d("SERVERVALUE", serverExpectedValue);
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
                                } else if (cachedValue.equals("SPAM")) {
                                    markAsSpam(address, context);
                                }
                            }
                            contact.setPriority(getLevelFromCachedValue(cachedValue)); // Inbox = cachedValue
                        }
                    } else { // Not in cache
                        contact.setPriority(computeTrustScore(context, address));
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

    public static String getCachedValue(SharedPreferences sharedPreferences, String number) {
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

    public static ContactDataModel.Level getLevelFromCachedValue(String cachedValue) {
        if (cachedValue.equals("TRUSTED")) {
            return ContactDataModel.Level.PRIORITY;
        } else if (cachedValue.equals("SPAM")) {
            return ContactDataModel.Level.SPAM;
        }
        return ContactDataModel.Level.REGULAR;
    }

    public static ContactDataModel.Level computeTrustScore(Context context, String number) {
        final CountDownLatch latchA = new CountDownLatch(1);
        final ContactDataModel.Level[] result = {ContactDataModel.Level.REGULAR};
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ComputeScoreTask threadA = new ComputeScoreTask(number, context);
                try {
                    String json = threadA.execute().get();
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    if (obj.has("message")) {
                        if (obj.get("message").toString().equals("TRUSTED")) {
                            result[0] = ContactDataModel.Level.PRIORITY;
                        } else {
                            result[0] = ContactDataModel.Level.SPAM;
                        }
                    } else {
                        double score = obj.get("score").getAsDouble();
                        if (score < 0) {
                            result[0] = ContactDataModel.Level.SPAM;
                        }
                    }
                    latchA.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            latchA.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result[0];
    }

    private static JSONObject getMessageObject(String endpoint, String uuid, String payloadParam) {
        JSONObject message = new JSONObject();
        String[] parts = payloadParam.split(" ");
        String payloadParamKey = parts[0];
        String payloadParamValue = parts[1];
        try {
            message.put("uuid", uuid);
            message.put("endpoint", endpoint);
            message.put("timestamp", System.currentTimeMillis());
            message.put(payloadParamKey, payloadParamValue);
        } catch (Exception e) {
            Log.e("createMessageObject", "JSON body put error", e);
        }

        return message;
    }

    private static String getMessageHash(JSONObject message) {
        String encoded = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(message.toString().getBytes(StandardCharsets.UTF_8));
            encoded = Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            Log.e("getMessageHash", "Error trying to compute hash", e);
        }

        return encoded;
    }

    /*
    * @param nonce - added into the unencrypted body of the json object unless it has a length of 0
    * @param endpoint - the route or endpoint a particular request is hitting, i.e. everything after the root url
    * @param hasSessionKey - should be true unless we are generating a new session key, in which case set as false
    * @param payloadParam - a parameter to give to the encrypted payload of our requests. It should take the form
    *                       of "key value". Will not work properly if the space is not there
    *
    * @returns - the json body of our requests to the server in accordance with the recent standardization
    * */
    private static JSONObject getJsonBody(String endpoint, String payloadParam, boolean hasSessionKey,
                                          String nonce, SharedPreferences preferences) {
        JSONObject jsonBody = new JSONObject();
        String key = "";
        String uuid = preferences.getString("UUID", "");

        JSONObject encryptedPayload = new JSONObject();
        JSONObject message = getMessageObject(endpoint, uuid, payloadParam); // phoneNumber.replaceAll("^\\w", "")

        if (hasSessionKey) {
            // use session key after generate session key
            key = preferences.getString("SESSION_KEY", "");
        } else {
            // use shared secret after registration and before we have a session key
            key = preferences.getString("SECRET", "");
        }
        try {
            encryptedPayload.put("message", message);
            encryptedPayload.put("hash", getMessageHash(message));
            jsonBody.put("uuid", uuid);
            if (nonce.length() > 0) {
                jsonBody.put("nonce", nonce);
            }
            jsonBody.put("e_payload", new AESManager().encrypt(encryptedPayload.toString(), key));
        } catch (Exception e) {
            Log.d("getJsonBody", "JSON body put error");
        }

        return jsonBody;
    }

    public static void markAsTrusted(String phoneNumber, Context context) {
        SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);

        String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
        String route = "/user/trust";
        String url = root + route;

        JSONObject jsonBody = getJsonBody(route, "phone " + phoneNumber.replaceAll("[^a-zA-Z0-9]", ""), true, "", preferences);
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
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
        SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);

        String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
        String route = "/user/spam";
        String url = root + route;

        JSONObject jsonBody = getJsonBody(route, "phone " + phoneNumber.replaceAll("[^a-zA-Z0-9]", ""), true, "", preferences);
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
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

    private static String getServerExpectedValue(String phoneNumber, Context context) {
        final CountDownLatch latchA = new CountDownLatch(1);
        final String[] result = {"UNKNOWN"};
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ExpectedValueTask threadA = new ExpectedValueTask(phoneNumber, context);
                try {
                    String json = threadA.execute().get();
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    result[0] = obj.get("message").toString();
                    latchA.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            latchA.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result[0];
    }

    private static class ExpectedValueTask extends AsyncTask<Void, Void, String> {
        private final String number;
        private final Context context;

        ExpectedValueTask(String number, Context context) {
            this.number = number;
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... requestFutures) {
            String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
            String route = "/user/known";
            String url = root + route;
            SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
            JSONObject jsonBody = getJsonBody(route, "phone " + this.number.replaceAll("[^a-zA-Z0-9]", ""), true, "", preferences);

            final okhttp3.MediaType JSON
                    = okhttp3.MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, jsonBody.toString());
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            String result = null;
            try {
                okhttp3.Response response = client.newCall(request).execute();
                result = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    private static class ComputeScoreTask extends AsyncTask<Void, Void, String> {
        private final String number;
        private final Context context;

        ComputeScoreTask(String number, Context context) {
            this.number = number;
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... requestFutures) {
            String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
            String route = "/user/trust";
            String url = root + route;
            SharedPreferences preferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
            JSONObject jsonBody = getJsonBody(route, "phone " + this.number.replaceAll("[^a-zA-Z0-9]", ""), true, "", preferences);

            final okhttp3.MediaType JSON
                    = okhttp3.MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, jsonBody.toString());
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            String result = null;
            try {
                okhttp3.Response response = client.newCall(request).execute();
                result = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    public static void generateSessionKey(SharedPreferences preferences, Context context) {
        initGetSessionKey(preferences, context);
    }

    private static void initGetSessionKey(SharedPreferences preferences, Context context) {

        String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
        String route = "/user/initgetkey";
        String url = root + route;

        BigInteger g = new BigInteger("5");
        BigInteger p = new BigInteger("23");
        int a = new Random(System.currentTimeMillis()).nextInt(p.intValue() - (2 * g.intValue())) + g.intValue();
        BigInteger a2 = new BigInteger(Integer.toString(a));
        String nonce = Integer.toString(new Random(System.currentTimeMillis()).nextInt());
        String keyhalf = String.format("0x%2s", g.modPow(a2, p).toString(16)).replace(' ', '0');

        JSONObject jsonBody = getJsonBody(route, "keyhalf " + keyhalf, false, nonce, preferences);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String key = preferences.getString("SECRET", "");
                            String responseNonce = Integer.toString(response.getInt("nonce"));
                            JSONObject responsePayload = new JSONObject(new AESManager().decrypt(response.getString("payload"), key));
                            String responseChallengeNonce = responsePayload.getString("nonce");
                            String responseKeyhalf = responsePayload.getString("keyhalf");

                            if (nonce.contentEquals(responseChallengeNonce)) {
                                // make next request and store session key
                                finGetSessionKey(responseKeyhalf, a2, p, preferences, responseNonce, root, context);
                            } else {
                                throw new Exception("Could not verify server identity");
                            }
                        } catch (Exception e) {
                            Log.e("initGetSessionKey response", "JSON body get error", e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("generateSessionKey - initGetSessionKey", "onErrorResponse: ", error);
                Toast.makeText(
                        context,
                        "Failed to establish a session key, please try again later",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
        );

        // Add the request to the RequestQueue.
        QueueSingleton.getInstance(context).addToRequestQueue(jsonRequest);
    }

    private static void finGetSessionKey(String responseKeyhalf, BigInteger a2, BigInteger p,
                                         SharedPreferences preferences, String responseNonce, String rootUrl,
                                         Context context) {
        // make json body
        String route = "/user/fingetkey";
        JSONObject jsonBody = getJsonBody(route, "keyhalf " + responseKeyhalf, false, "", preferences);
        String url = rootUrl + route;

        try {

            JsonObjectRequest jsonRequestFin = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // store session key
                            BigInteger gbmodp = new BigInteger(responseKeyhalf, 16);
                            String sessionKeyInHex = gbmodp.modPow(a2, p).toString(16);
                            String paddedSessionKey = String.format("%2s", sessionKeyInHex).replace(' ', '0');
                            preferences.edit().putString("SESSION_KEY", paddedSessionKey).apply();

                            Intent mainIntent = new Intent(context, MainActivity.class);
                            context.startActivity(mainIntent);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("generateSessionKey - finGetSessionKey", "onErrorResponse: ", error);
                            Toast.makeText(
                                    context,
                                    "Failed to establish a session key, please try again later",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
            // Add the request to the RequestQueue.
            QueueSingleton.getInstance(context).addToRequestQueue(jsonRequestFin);
        } catch (Exception e) {
            Log.e("generateSessionKey - finGetSessionKey", "JSON body put error");
        }
    }
}
package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Random;

public class SplashActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        QueueSingleton.getInstance(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
        //TODO: This will be an optional input in the bundle which will be added when notification for new message arrives
        nextActivity = Chat or Main depending on "chat_number" argument
        */

        int permissionCheckReadSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        int permissionCheckReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        int permissionCheckSendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        int permissionCheckReceiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        int permissionCheckInternet = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);

        if (permissionCheckReadSms != PackageManager.PERMISSION_GRANTED ||
                permissionCheckReadContacts != PackageManager.PERMISSION_GRANTED ||
                permissionCheckSendSms != PackageManager.PERMISSION_GRANTED ||
                permissionCheckReceiveSms != PackageManager.PERMISSION_GRANTED ||
                permissionCheckInternet != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.INTERNET}, 100);
            permissionCheckReadSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
            permissionCheckReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
            permissionCheckSendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
            permissionCheckReceiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
            permissionCheckInternet = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
            if (permissionCheckReadSms != PackageManager.PERMISSION_GRANTED ||
                    permissionCheckReadContacts != PackageManager.PERMISSION_GRANTED ||
                    permissionCheckSendSms != PackageManager.PERMISSION_GRANTED ||
                    permissionCheckReceiveSms != PackageManager.PERMISSION_GRANTED ||
                    permissionCheckInternet != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "This App requires these permissions to run!", Toast.LENGTH_SHORT);
                finish();
            }
        }

        SharedPreferences pref = getApplicationContext().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        Intent registrationIntent = new Intent(SplashActivity.this, RegistrationActivity.class);
        if (SMSContacts.isInternetAvailable()) {
            if (pref.getString("UUID", "").isEmpty() || pref.getString("SECRET", "").isEmpty()) {
                startActivity(registrationIntent);
            } else {
                //TODO: call backend enpoint and use existing session key
                /*
                if (sk exists in cache):
                    f() which calls Thomas's endpoint to check if
                    can continue
                 else:
                    generateSessionKey(); // update session key in cache
                 */
                generateSessionKey(pref, getApplicationContext());
                finish();
            }
        } else {
            if (pref.getString("UUID", "").isEmpty() || pref.getString("SECRET", "").isEmpty()) {
                startActivity(registrationIntent);
            } else {
                SMSContacts.setContactList(SMSContacts.populateSMSGroups(getApplicationContext()));
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }

        finish();
    }

    private void generateSessionKey(SharedPreferences preferences, Context context) {
        // TODO: Generate new session key
        boolean isKeyEstablished = initGetSessionKey(preferences, context);
        if (isKeyEstablished) {
            SMSContacts.setContactList(SMSContacts.populateSMSGroups(getApplicationContext()));
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(mainIntent);
        } else {
            Log.e("generateSessionKey", "Could not establish session key");
            Toast.makeText(
                    context,
                    "Failed to establish a session key, please try again later",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private boolean initGetSessionKey(SharedPreferences preferences, Context context) {
        final boolean[] successful = {false};

        String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
        String route = "/user/trust";
        String url = root + route;
        BigInteger g = new BigInteger("5");
        BigInteger p = new BigInteger("23");;
        int a = new Random(System.currentTimeMillis()).nextInt(p.intValue() - (2 * g.intValue())) + g.intValue();
        BigInteger a2 = new BigInteger(Integer.toString(a));
        String nonce = Integer.toString(new Random(System.currentTimeMillis()).nextInt());
        String keyhalf = g.modPow(a2, p).toString();
        JSONObject jsonBody = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            payload.put("uuid", preferences.getString("UUID", ""));
            payload.put("keyhalf", keyhalf);
            jsonBody.put("uuid", preferences.getString("UUID", ""));
            jsonBody.put("nonce", nonce);
            jsonBody.put("payload", payload.toString());
        } catch (Exception e) {
            Log.e("generateSessionKey", "JSON body put error");
        }
        // Request a string response from the provided URL.
        String requestBody = jsonBody.toString();
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String responseNonce = response.getString("nonce");
                        JSONObject responsePayload = response.getJSONObject("payload");
                        String responseChallengeNonce = responsePayload.getString("nonce");
                        String responseKeyhalf = responsePayload.getString("keyhalf");

                        if (nonce.contentEquals(responseChallengeNonce)) {
                            // make next request and store session key
                            successful[0] = finGetSessionKey(responseKeyhalf, a2, p, preferences, responseNonce, url, context) || successful[0];
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
        return successful[0];
    }

    private boolean finGetSessionKey(String responseKeyhalf, BigInteger a2, BigInteger p,
                                  SharedPreferences preferences, String responseNonce, String url,
                                  Context context) {
        // make json body
        JSONObject jsonBody = new JSONObject();
        JSONObject payload = new JSONObject();

        final boolean[] successful = {false};

        try {
            payload.put("uuid", preferences.getString("UUID", ""));
            payload.put("nonce", responseNonce);
            jsonBody.put("uuid", preferences.getString("UUID", ""));
            jsonBody.put("payload", payload);

            JsonObjectRequest jsonRequestFin = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // store session key
                        BigInteger gbmodp = new BigInteger(responseKeyhalf);
                        String sessionKey = gbmodp.modPow(a2, p).toString();
                        preferences.edit().putString("SESSION_KEY", sessionKey).apply();
                        successful[0] = true;
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

        return successful[0];
    }
}

package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplashActivity extends Activity {

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.INTERNET, Manifest.permission.READ_CONTACTS, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_SMS};
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 100;


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

        try {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkPermissions();

                    SharedPreferences pref = getApplicationContext().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
                    Intent registrationIntent = new Intent(SplashActivity.this, RegistrationActivity.class);

                    if (SMSContacts.isInternetAvailable(getApplicationContext())) {
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
                            SMSContacts.generateSessionKey(pref, SplashActivity.this);
                            //                finish();
                        }
                    } else {
                        if (pref.getString("UUID", "").isEmpty() || pref.getString("SECRET", "").isEmpty()) {
                            startActivity(registrationIntent);
                        } else {
                            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(mainIntent);
                            finish();
                        }
                    }

                    finish();

                }
            }, 2000);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                break;
        }
    }
}

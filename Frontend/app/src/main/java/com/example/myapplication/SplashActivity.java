package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
        // TODO: This should effectively ensure a user is authenticated or not
        int permissionCheckReadSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        int permissionCheckReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        int permissionCheckSendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        if (permissionCheckReadSms == PackageManager.PERMISSION_GRANTED && permissionCheckReadContacts == PackageManager.PERMISSION_GRANTED && permissionCheckSendSms == PackageManager.PERMISSION_GRANTED) {
            SMSContacts.setContactList(SMSContacts.populateSMSGroups(getApplicationContext()));

            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        } else {
            Intent registrationIntent = new Intent(SplashActivity.this, RegistrationActivity.class);
            startActivity(registrationIntent);
        }
        // TODO: Recalculate data structures on incoming notifications
        finish();
    }
}

package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

        SharedPreferences pref = this.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        Intent registrationIntent = new Intent(SplashActivity.this, RegistrationActivity.class);
        if (SMSContacts.isInternetAvailable()) {
            if (pref.getString("UUID", "").isEmpty() || pref.getString("SECRET", "").isEmpty()) {
                startActivity(registrationIntent);
            } else {
                //TODO: call backend enpoint and use existing session key
                generateSessionKey();
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

    private void generateSessionKey() {
        // TODO: Generate new session key
        SMSContacts.setContactList(SMSContacts.populateSMSGroups(getApplicationContext()));
        Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(mainIntent);
    }
}

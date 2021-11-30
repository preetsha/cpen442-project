package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.myapplication.databinding.ActivityRegistrationBinding;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "Security Exception";
    private SharedPreferences sharedPreferences;
    private AppBarConfiguration appBarConfiguration;
    private ActivityRegistrationBinding binding;

    private final String preferencesName = "SharedPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_registration);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();

        initEncryptedSharedPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_registration);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void initEncryptedSharedPreferences() {
        // Step 1: Create or retrieve the Master Key for encryption/decryption and get context
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            Context context = getApplicationContext();

            // Step 2: Initialize/open an instance of EncryptedSharedPreferences
            sharedPreferences = EncryptedSharedPreferences.create(
                    preferencesName,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.d(TAG, "initEncryptedSharedPreferences: ", e);
        }

    }
}
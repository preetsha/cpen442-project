package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.myapplication.databinding.FragmentEnterOtpBinding;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class EnterOTPFragment extends Fragment {

    private FragmentEnterOtpBinding binding;
    private String otp;
    private String phoneNumber;
    private final int OTP_CODE_LENGTH = 6;
    private SharedPreferences sharedPreferences;
    private String sharedSecret;
    private SecretKey secretKey;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentEnterOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = getActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        readPhoneNumber();
        try {
            // create new key
            secretKey = KeyGenerator.getInstance("AES").generateKey();
            // get base64 encoded version of the key
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            sharedSecret = encodedKey;
            // decode the base64 encoded string
            // byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
            // rebuild key using SecretKeySpec
            // SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            Log.d("SECRET", sharedSecret);
        } catch (NoSuchAlgorithmException e) {
            Log.e("sharedSecret generation", "onViewCreated: ", e);
        }

        // Update otp whenever the value in the otp text box changes
        binding.enterOTPContainer.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                otp = s.toString().trim();
                setOTPInputStatus(binding.enterOTPContainer, otp);
            }
        });

        binding.buttonOtpBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(EnterOTPFragment.this)
                        .navigate(R.id.action_EnterOTPFragment_to_RegistrationFragment);
            }
        });

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: API Call to Resend Token
                NavHostFragment.findNavController(EnterOTPFragment.this)
                        .navigate(R.id.action_EnterOTPFragment_to_RegistrationFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setOTPInputStatus(TextInputLayout til, String s) {
        if (s.length() < OTP_CODE_LENGTH) {
            til.setError("OTP code is too short");
        } else if (s.length() > OTP_CODE_LENGTH) {
            til.setError("OTP code is too long");
        } else {
            til.setError(null);
            // TODO: Make API Call to Verify
            // TODO: Save token/secret if verification is successful
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(getContext());
            String root ="https://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
            String route ="/users/finreg";
            String url = root + route;
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("phone_number", phoneNumber);
                jsonBody.put("shared_secret", sharedSecret);
                jsonBody.put("one_time_pass", s);
            } catch (Exception e) {
                Log.d("FIN REGISTRATION", "JSON body put error");
            }
            // Request a string response from the provided URL.
            String requestBody = jsonBody.toString();
            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        try {
                            String salt = response.getString("salt");
                            sharedPreferences.edit()
                                    .putString("SALT", salt)
                                    .apply();
                            sharedPreferences.edit()
                                    .putString("SECRET", sharedSecret)
                                    .apply();
                        } catch (JSONException je) {
                            Log.e("FIN REGISTRATION", "onResponse: ", je);
                        }
                        Toast.makeText(getContext(), "Response is: "+ response.toString().substring(0,500), Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Move this to onResponse when API is ready, add toast back
                        Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                        startActivity(mainIntent);
                        // Toast.makeText(getContext(), "Please try again later", Toast.LENGTH_SHORT).show();
                    }
                }
            );

            // Add the request to the RequestQueue.
            queue.add(jsonRequest);
        }
    }

    private void readPhoneNumber() {
        phoneNumber = sharedPreferences.getString("PHONE_NUMBER", "");
        String text = getString(R.string.otp_code_sent, phoneNumber.substring(6));
        binding.textviewOtpCodeSent.setText(text);
        System.out.println(text);
    }

}
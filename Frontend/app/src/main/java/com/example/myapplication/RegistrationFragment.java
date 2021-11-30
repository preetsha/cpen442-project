package com.example.myapplication;

import android.content.Context;
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
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.myapplication.databinding.FragmentRegistrationBinding;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

public class RegistrationFragment extends Fragment {

    private FragmentRegistrationBinding binding;
    private String phoneNumber;
    private final int PHONE_NUMBER_LENGTH = 10;
    private SharedPreferences sharedPreferences;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = getActivity().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);

        // Update phoneNumber whenever the value in the phone number text box changes
        binding.editTextPhoneContainer.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                phoneNumber = s.toString().trim();
                setPhoneNumberInputStatus(binding.editTextPhoneContainer, phoneNumber);
            }
        });

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!SMSContacts.isInternetAvailable()) {
                    Toast.makeText(getContext(), "Please try again when you are connected to the internet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (phoneNumber.length() == PHONE_NUMBER_LENGTH && isValidPhoneNumber(phoneNumber)) {
                    // Display Error Dialog if Server is Unavailable
                    // Instantiate the RequestQueue.
                    String root = "http://ec2-54-241-2-134.us-west-1.compute.amazonaws.com:8080";
                    String route = "/user/initreg";
                    String url = root + route;
                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("phone_number", phoneNumber);
                    } catch (Exception e) {
                        Log.d("INIT REGISTRATION", "JSON body put error");
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
                                Log.e("INIT REGISTRATION", "onErrorResponse: ", error);
                                Toast.makeText(getContext(), "That didn't work", Toast.LENGTH_SHORT).show();
                            }
                        }
                    );

                    // Add the request to the RequestQueue.
                    QueueSingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(jsonRequest);

                    savePhoneNumber(phoneNumber);
                    NavHostFragment.findNavController(RegistrationFragment.this)
                            .navigate(R.id.action_RegistrationFragment_to_EnterOTPFragment);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setPhoneNumberInputStatus(TextInputLayout til, String s) {
        if (s.length() < PHONE_NUMBER_LENGTH) {
            til.setError("Phone number too short");
        } else if (s.length() > PHONE_NUMBER_LENGTH) {
            til.setError("Phone number too long");
        } else if (!isValidPhoneNumber(s)) {
            til.setError("Please only enter numerical digits");
        } else {
            til.setError(null);
        }
    }

    private boolean isValidPhoneNumber(String s) {
        return !s.matches(".*\\D");
    }

    private void savePhoneNumber(String phoneNumber) {
        sharedPreferences.edit()
            .putString("PHONE_NUMBER", phoneNumber)
            .apply();
    }

}
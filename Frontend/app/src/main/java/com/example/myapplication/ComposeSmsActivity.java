package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityComposeSmsBinding;
import com.google.android.material.textfield.TextInputLayout;

public class ComposeSmsActivity extends AppCompatActivity {
    private ActivityComposeSmsBinding binding;
    private EditText textInput;
    private String phoneNumber;
    private final int PHONE_NUMBER_LENGTH = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityComposeSmsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }

    @Override
    protected void onResume() {
        super.onResume();

        textInput = binding.editGchatMessage;

        binding.editRecipient.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                phoneNumber = s.toString().trim();
                setPhoneNumberInputStatus(binding.editRecipient, phoneNumber);
            }
        });

        binding.buttonGchatSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!textInput.getText().toString().isEmpty()
                        && phoneNumber.length() == PHONE_NUMBER_LENGTH
                        && isValidPhoneNumber(phoneNumber)) {
                    sendMessage();
                    Intent mainIntent = new Intent(ComposeSmsActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textInput = null;
        binding = null;
        phoneNumber = "";
    }

    public void sendMessage() {
        String msgBody = textInput.getText().toString();

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, msgBody, null, null);
        Toast.makeText(getApplicationContext(), "SMS sent.",
                Toast.LENGTH_LONG).show();

        textInput.setText("");
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
}

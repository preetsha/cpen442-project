package com.example.myapplication;


import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView mMessageRecycler;
    private SMSListAdapter mMessageAdapter;
    private String threadId = "";
    private String name = "";
    private String address = "";
    private final List<SMSMessage> messageList = new ArrayList<>();
    private EditText textInput;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        threadId = extras.getString("threadId");
        name = extras.getString("name");
        address = extras.getString("address");

        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        textInput = findViewById(R.id.edit_gchat_message);
        sendButton = findViewById(R.id.button_gchat_send);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!textInput.getText().toString().isEmpty()) {
                    sendMessage();
                }
            }
        });

        populateMessageList();

        mMessageRecycler = findViewById(R.id.recycler_gchat);
        mMessageAdapter = new SMSListAdapter(this, messageList);
        mMessageRecycler.setAdapter(mMessageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);
        mMessageRecycler.setLayoutManager(llm);
        mMessageRecycler.scrollToPosition(0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void populateMessageList() {
        ContentResolver cr = getApplicationContext().getContentResolver();
        Cursor cur = cr.query(Uri.parse("content://sms"),
                new String[]{"thread_id", "person", "body", "date"}, "thread_id=" + threadId, null, Telephony.Sms.DEFAULT_SORT_ORDER);

        try {
            while (cur.moveToNext()) {
                long dateLong = cur.getLong(cur.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String body = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.BODY));
                int person = cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.PERSON));
                // TODO: buttons, sending

                boolean sent = false;
                if (person == 0) {
                    sent = true;
                }

                SMSMessage message = new SMSMessage(body, dateLong, sent);
                messageList.add(message);
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    public void sendMessage() {
        String msgBody = textInput.getText().toString();
        SMSMessage newMsg = new SMSMessage(msgBody, System.currentTimeMillis(), true);
        messageList.add(newMsg);

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(address, null, msgBody, null, null);
        Toast.makeText(getApplicationContext(), "SMS sent.",
                Toast.LENGTH_LONG).show();

        textInput.setText("");
        mMessageRecycler.invalidate();
    }
}

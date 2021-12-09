package com.example.myapplication;


import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView mMessageRecycler;
    private SMSListAdapter mMessageAdapter;
    private String threadId = "";
    private String name = "";
    private String address = "";
    private int priority = 0;
    private List<SMSMessage> messageList;
    private EditText textInput;
    private Button sendButton;

    public static final String cacheTrustedKey = "trustedList";
    public static final String cacheRegularKey = "regularList";
    public static final String cacheSpamKey = "spamList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        threadId = extras.getString("threadId");
        name = extras.getString("name");
        address = extras.getString("address");
        priority = extras.getInt("priority", 0);

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
        llm.setReverseLayout(false);
        llm.setStackFromEnd(true);
        mMessageRecycler.setLayoutManager(llm);
        mMessageRecycler.scrollToPosition(messageList.size() - 1);

        mMessageRecycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom || bottom > oldBottom) {
                    mMessageRecycler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMessageRecycler.smoothScrollToPosition(
                                    mMessageRecycler.getAdapter().getItemCount() - 1);
                        }
                    }, 100);
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void populateMessageList() {
        ContentResolver cr = getApplicationContext().getContentResolver();
        Cursor cur = cr.query(Uri.parse("content://sms"),
                new String[]{"thread_id", "person", "body", "date", "address"}, "thread_id=" + threadId, null, "date ASC");

        messageList = new ArrayList<>();
        Set<String> seenDates = new HashSet<>();
        try {
            while (cur.moveToNext()) {
                long dateLong = cur.getLong(cur.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String body = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.BODY));
                int person = cur.getInt(cur.getColumnIndexOrThrow(Telephony.Sms.PERSON));
                String address = cur.getString(cur.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));


                boolean sent = false;
                boolean isFirstDate = false;
                if (address.equals(this.address)) {
                    sent = true;
                }

                String date = DateUtils.formatDateTime(this, dateLong, DateUtils.FORMAT_SHOW_DATE);
                if (!seenDates.contains(date)) {
                    isFirstDate = true;
                    seenDates.add(date);
                }

                SMSMessage message = new SMSMessage(body, dateLong, sent, isFirstDate);
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
        SMSMessage newMsg = new SMSMessage(msgBody, System.currentTimeMillis(), true, true);
        messageList.add(newMsg);

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(address, null, msgBody, null, null);
        Toast.makeText(getApplicationContext(), "SMS sent.",
                Toast.LENGTH_LONG).show();

        textInput.setText("");

        final int idx = SMSContacts.getContactIndexByThread(threadId);
        ContactDataModel c = SMSContacts.getContactList().get(idx);
        c.setSnippet(msgBody);
        SMSContacts.contactList.set(idx, c);

        mMessageAdapter = new SMSListAdapter(this, messageList);
        mMessageRecycler.setAdapter(mMessageAdapter);
        mMessageRecycler.scrollToPosition(messageList.size() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_chat_header, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuTrust = menu.findItem(R.id.mark_trusted_action);
        MenuItem menuSpam = menu.findItem(R.id.mark_spam_action);

        if (priority == 1) {
            menuTrust.setVisible(false);
        } else if (priority == -1) {
            menuSpam.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();
        final int idx = SMSContacts.getContactIndexByThread(threadId);
        ContactDataModel c = SMSContacts.getContactList().get(idx);

        if (id == R.id.mark_trusted_action) {

            System.out.println("MARK TRUSTED");
            c.setPriority(ContactDataModel.Level.PRIORITY);
            priority = 1;
            SMSContacts.contactList.set(idx, c);
            SMSContacts.markAsTrusted(c.getNumber(), getApplicationContext());

            SharedPreferences preferences = getApplicationContext().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);

            Set<String> trustedList = preferences.getStringSet(cacheTrustedKey, new HashSet<>());
            Set<String> spamList = preferences.getStringSet(cacheSpamKey, new HashSet<>());
            Set<String> regularList = preferences.getStringSet(cacheRegularKey, new HashSet<>());
            trustedList.add(c.getNumber());
            spamList.remove(c.getNumber());
            regularList.remove(c.getNumber());
            preferences.edit().remove(cacheTrustedKey).apply();
            preferences.edit().remove(cacheRegularKey).apply();
            preferences.edit().remove(cacheSpamKey).apply();
            preferences.edit().putStringSet(cacheTrustedKey, trustedList).apply();
            preferences.edit().putStringSet(cacheRegularKey, regularList).apply();
            preferences.edit().putStringSet(cacheSpamKey, spamList).apply();

            onBackPressed();
            return true;
        } else if (id == R.id.mark_spam_action) {
            System.out.println("MARK SPAM");

            c.setPriority(ContactDataModel.Level.SPAM);
            priority = -1;
            SMSContacts.contactList.set(idx, c);
            SMSContacts.markAsSpam(c.getNumber(), getApplicationContext());

            SharedPreferences preferences = getApplicationContext().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);

            Set<String> trustedList = preferences.getStringSet(cacheTrustedKey, new HashSet<>());
            Set<String> spamList = preferences.getStringSet(cacheSpamKey, new HashSet<>());
            Set<String> regularList = preferences.getStringSet(cacheRegularKey, new HashSet<>());
            trustedList.remove(c.getNumber());
            spamList.add(c.getNumber());
            regularList.remove(c.getNumber());
            preferences.edit().remove(cacheTrustedKey).apply();
            preferences.edit().remove(cacheRegularKey).apply();
            preferences.edit().remove(cacheSpamKey).apply();
            preferences.edit().putStringSet(cacheTrustedKey, trustedList).apply();
            preferences.edit().putStringSet(cacheRegularKey, regularList).apply();
            preferences.edit().putStringSet(cacheSpamKey, spamList).apply();

            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

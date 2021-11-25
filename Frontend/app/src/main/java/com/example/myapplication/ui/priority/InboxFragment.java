package com.example.myapplication.ui.priority;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.ChatActivity;
import com.example.myapplication.ContactAdapter;
import com.example.myapplication.ContactDataModel;
import com.example.myapplication.SMSContacts;
import com.example.myapplication.databinding.FragmentInboxBinding;

import java.util.ArrayList;
import java.util.List;

public class InboxFragment extends Fragment {

    private FragmentInboxBinding binding;
    ListView listView;
    private static ContactAdapter adapter;
    private static List<ContactDataModel> contactList = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentInboxBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        listView = binding.contactList;

        updateAdapter();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ContactDataModel dataModel = contactList.get(position);

                Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
                chatIntent.putExtra("threadId", dataModel.getThreadId());
                chatIntent.putExtra("name", dataModel.getDisplayName());
                chatIntent.putExtra("address", dataModel.getNumber());
                chatIntent.putExtra("priority", dataModel.getPriorityInt());
                startActivity(chatIntent);
            }
        });

        return root;

    }

    @Override
    public void onResume() {
        super.onResume();
        updateAdapter();
    }

    private void updateAdapter() {
        String priorityLevelStr = this.getArguments().getString("type");
        ContactDataModel.Level priorityLevel = ContactDataModel.Level.REGULAR;
        if (priorityLevelStr.equals("priority")) {
            priorityLevel = ContactDataModel.Level.PRIORITY;
        } else if (priorityLevelStr.equals("spam")) {
            priorityLevel = ContactDataModel.Level.SPAM;
        }

        contactList = SMSContacts.getContactsByInbox(priorityLevel);
        adapter = new ContactAdapter(contactList, getContext());

        listView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
package com.example.myapplication.ui.priority;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.ContactAdapter;
import com.example.myapplication.ContactDataModel;
import com.example.myapplication.SMSContacts;
import com.example.myapplication.databinding.FragmentInboxBinding;

import java.util.ArrayList;

public class InboxFragment extends Fragment {

    private FragmentInboxBinding binding;
    ListView listView;
    private static ContactAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentInboxBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = (ListView) binding.contactList;
        ArrayList<ContactDataModel> contactList = SMSContacts.getContactList();

        adapter = new ContactAdapter(contactList, getContext());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ContactDataModel dataModel = contactList.get(position);

                // TODO: jump to new chat view
            }
        });

        //TODO: .observe(getViewLifecycleOwner(), new Observer<String>() to detect changes maybe?

        return root;

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
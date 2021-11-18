package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SMSContacts {
    public static ArrayList<ContactDataModel> contactList;

    public SMSContacts() {
    }

    public static List<ContactDataModel> getContactList() {
        return contactList;
    }

    public static List<ContactDataModel> getContactsByInbox(ContactDataModel.Level level) {
        return contactList.stream().filter(c -> c.getPriority() == level).collect(Collectors.toList());
    }

    public static void setContactList(ArrayList<ContactDataModel> contactList) {
        SMSContacts.contactList = contactList;
    }
}

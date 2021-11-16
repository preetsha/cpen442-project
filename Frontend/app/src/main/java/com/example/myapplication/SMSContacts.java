package com.example.myapplication;

import java.util.ArrayList;

public class SMSContacts {
    public static ArrayList<ContactDataModel> contactList;

    public SMSContacts() {
    }

    public static ArrayList<ContactDataModel> getContactList() {
        return contactList;
    }

    public static void setContactList(ArrayList<ContactDataModel> contactList) {
        SMSContacts.contactList = contactList;
    }
}

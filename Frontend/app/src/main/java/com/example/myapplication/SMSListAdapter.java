package com.example.myapplication;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SMSListAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private final List<SMSMessage> mMessageList;

    public SMSListAdapter(Context mContext, List<SMSMessage> mMessageList) {
        this.mContext = mContext;
        this.mMessageList = mMessageList;
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        SMSMessage message = mMessageList.get(position);

        return message.isSent() ? 1 : 0;
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == 1) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_sender, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_receiver, parent, false);
            return new ReceivedMessageHolder(view);
        }

        return null;
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SMSMessage message = mMessageList.get(position);

        switch (holder.getItemViewType()) {
            case 1:
                ((SentMessageHolder) holder).bind(message);
                break;
            case 0:
                ((ReceivedMessageHolder) holder).bind(message);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, dateText;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_gchat_message_me);
            timeText = itemView.findViewById(R.id.text_gchat_timestamp_me);
            dateText = itemView.findViewById(R.id.text_gchat_date_me);
        }

        void bind(SMSMessage message) {
            messageText.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.
            if (message.isFirstMessageAtDate()) {
                dateText.setText(DateUtils.formatDateTime(itemView.getContext(), message.getTime(), DateUtils.FORMAT_ABBREV_MONTH));
            } else {
                dateText.setText("");
            }
            timeText.setText(DateUtils.formatDateTime(itemView.getContext(), message.getTime(), DateUtils.FORMAT_SHOW_TIME));

        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, dateText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_gchat_message_other);
            timeText = itemView.findViewById(R.id.text_gchat_timestamp_other);
            dateText = itemView.findViewById(R.id.text_gchat_date_other);
        }

        void bind(SMSMessage message) {
            messageText.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.
            // Format the stored timestamp into a readable String using method.
            if (message.isFirstMessageAtDate()) {
                dateText.setText(DateUtils.formatDateTime(itemView.getContext(), message.getTime(), DateUtils.FORMAT_ABBREV_MONTH));
            } else {
                dateText.setText("");
            }
            timeText.setText(DateUtils.formatDateTime(itemView.getContext(), message.getTime(), DateUtils.FORMAT_SHOW_TIME));

        }
    }

}

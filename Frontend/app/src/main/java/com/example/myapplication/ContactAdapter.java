package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ContactAdapter extends ArrayAdapter<ContactDataModel> implements View.OnClickListener {

    private ArrayList<ContactDataModel> data;
    Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView contactPreview;
        TextView snippetPreview;
        TextView timePreview;
    }

    public ContactAdapter(ArrayList<ContactDataModel> data, Context context) {
        super(context, R.layout.content_summary, data);
        this.data = data;
        this.mContext = context;

    }

    @Override
    public void onClick(View v) {

        int position = (Integer) v.getTag();
        Object object = getItem(position);
        ContactDataModel dataModel = (ContactDataModel) object;

        //TODO: Open chat
    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        ContactDataModel dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.content_summary, parent, false);
            viewHolder.contactPreview = (TextView) convertView.findViewById(R.id.contactPreview);
            viewHolder.snippetPreview = (TextView) convertView.findViewById(R.id.snippetPreview);
            viewHolder.timePreview = (TextView) convertView.findViewById(R.id.timePreview);

            result = convertView;

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        result.startAnimation(animation);
        lastPosition = position;

        viewHolder.snippetPreview.setText(dataModel.getSnippet());
        viewHolder.contactPreview.setText(dataModel.getDisplayName());
        viewHolder.timePreview.setText(dataModel.getDisplayTime());
        // Return the completed view to render on screen
        return convertView;
    }
}

package com.example.myvideoviewer.contents;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.example.myvideoviewer.R;

import java.util.ArrayList;

class ContentsAdapter extends BaseAdapter {
    private final ArrayList<ContentsItem> itemList = new ArrayList<>();
    private Listener LinkListener;

    public void add(ContentsItem item) {
        itemList.add(item);
    }

    public void clear() {
        itemList.clear();
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public ContentsItem getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setOnLinkListener(Listener listener) {
        LinkListener = listener;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.jav247_item, parent, false);
        }
        ContentsItem item = getItem(position);
        ((TextView) convertView.findViewById(R.id.title)).setText(item.title);
        ((TextView) convertView.findViewById(R.id.meta)).setText(item.meta);
        Glide.with(convertView)
                .load(item.thumbnail)
                .into((ImageView) convertView.findViewById(R.id.thumbnail));
        convertView.setOnClickListener((v)->{
            if (LinkListener != null) {
                LinkListener.onClick(item);
            }
        });
        return convertView;
    }

    interface Listener {
        void onClick(ContentsItem item);
    }
}
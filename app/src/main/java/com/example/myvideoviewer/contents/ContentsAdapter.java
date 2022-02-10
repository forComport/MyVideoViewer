package com.example.myvideoviewer.contents;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.example.myvideoviewer.R;

import java.io.File;
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
        if ("file".equals(item.thumbnail) && item.pageUrl != null) {
            File file = new File(Uri.parse(item.pageUrl).getPath());
            Glide.with(convertView)
                    .load(file)
                    .into((ImageView) convertView.findViewById(R.id.thumbnail));
        } else {
            Glide.with(convertView)
                    .load(item.thumbnail)
                    .into((ImageView) convertView.findViewById(R.id.thumbnail));
        }
        convertView.setOnClickListener((v)->{
            if (LinkListener != null) {
                LinkListener.onClick(item);
            }
        });
        convertView.setOnLongClickListener((v)->{
            if (LinkListener != null) {
                LinkListener.onLongClick(item);
                return true;
            } else {
                return false;
            }
        });
        return convertView;
    }

    interface Listener {
        void onClick(ContentsItem item);
        void onLongClick(ContentsItem item);
    }
}
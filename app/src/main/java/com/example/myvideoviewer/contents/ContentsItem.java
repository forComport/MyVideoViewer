package com.example.myvideoviewer.contents;

import android.os.Parcel;
import android.os.Parcelable;

import org.jsoup.nodes.Element;

public class ContentsItem implements Parcelable {
    public int id = 0;
    public String thumbnail;
    public String pageUrl;
    public String title;
    public String meta;

    public ContentsItem(String thumbnail, String pageUrl, String title, String meta) {
        this.thumbnail = thumbnail;
        this.pageUrl = pageUrl;
        this.title = title;
        this.meta = meta;
    }

    ContentsItem(Parcel in) {
        this.id = in.readInt();
        this.thumbnail = in.readString();
        this.pageUrl = in.readString();
        this.title = in.readString();
        this.meta = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(thumbnail);
        dest.writeString(pageUrl);
        dest.writeString(title);
        dest.writeString(meta);
    }

    public static final Parcelable.Creator<ContentsItem> CREATOR = new Parcelable.Creator<ContentsItem>() {

        @Override
        public ContentsItem createFromParcel(Parcel source) {
            return new ContentsItem(source);
        }

        @Override
        public ContentsItem[] newArray(int size) {
            return new ContentsItem[size];
        }
    };
}
package com.example.myvideoviewer.provider;

import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.myvideoviewer.contents.ContentsItem;
import com.example.myvideoviewer.contents.ContentsLoader;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class YoutubeLoader extends ContentsLoader {

    public static final String KEY = "Youtube";

    @Override
    public void init() {
        try {
            YoutubeDL.getInstance().init(context);
        } catch (YoutubeDLException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public ContentsItem makeItem(String url) {
        return new ContentsItem("", url, "", "");
    }

    @Override
    public void search(String keyword) {
    }

    public void loadList() {
    }

    public void loadDetail(ContentsItem item) {
        try {
            YoutubeDLRequest request = new YoutubeDLRequest(item.pageUrl);
            request.addOption("-f", "best");
            VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
            String url = streamInfo.getUrl();
            if (listener != null) {
                listener.onVideoLoad(url);
            }
        } catch (YoutubeDLException | InterruptedException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}

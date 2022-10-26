package com.example.myvideoviewer.provider;

import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.myvideoviewer.contents.ContentsItem;
import com.example.myvideoviewer.contents.ContentsLoader;
import com.example.myvideoviewer.contents.DetailActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HanimeLoader extends ContentsLoader {

    public static final String KEY = "Hanime";
    private String url = "https://search.htv-services.com/";

    @Override
    public void init() {
        page = 0;
        loading = false;
    }

    @Override
    public void search(String keyword) {
    }

    public void loadList() {
        loading = true;
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, (res)-> {
            try {
                JSONObject obj = new JSONObject(res);
                String hits = obj.getString("hits");
                JSONArray arr = new JSONArray(hits);
                ArrayList<ContentsItem> contents = new ArrayList<>();
                for(int i = 0;i<arr.length();i++) {
                    JSONObject item = arr.getJSONObject(i);
                    String thumbnail = item.getString("poster_url");
                    String title = item.getString("name");
                    String pageUrl = "https://hanime.tv/api/v8/video?id=" + item.getString("slug");
                    String meta = item.getString("tags");
                    ContentsItem content = new ContentsItem(thumbnail, pageUrl, title, meta);
                    contents.add(content);
                }
                listListener.onListLoad(contents);
                page += 1;
                loading = false;
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }, (err)-> {
            Toast.makeText(context, err.toString(), Toast.LENGTH_LONG).show();
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> header = new HashMap<>();
                header.put("Content-Type", "application/json;charset=UTF-8");
                return header;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                String body = "{\"search_text\":\"\",\"tags\":[],\"tags_mode\":\"AND\",\"brands\":[],\"blacklist\":[],\"order_by\":\"created_at_unix\",\"ordering\":\"desc\"," +
                        "\"page\":"+page+"}";
                return body.getBytes();
            }
        };
        queue.add(stringRequest);
    }

    public void loadDetail(ContentsItem item) {
        Log.d(TAG, item.pageUrl);
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, item.pageUrl, (res)-> {
            try {
                String videoUrl = null;
                JSONObject obj = new JSONObject(res);
                JSONArray servers = obj.getJSONObject("videos_manifest")
                        .getJSONArray("servers");
                for(int i=0;i<servers.length();i++) {
                    JSONObject server = servers.getJSONObject(i);
                    JSONArray streams = server.getJSONArray("streams");
                    for(int j=0;j<streams.length();j++) {
                        JSONObject stream = streams.getJSONObject(j);
                        String url = stream.getString("url");
                        if(!"".equals(url)) {
                            videoUrl = url;
                        }
                        if(url.contains("480p")) {
                            videoUrl = url;
                        }
                        Log.d(TAG, stream.getString("filename"));
                    }
                }

                JSONArray series = obj.getJSONArray("hentai_franchise_hentai_videos");
                ArrayList<ContentsItem> contents = new ArrayList<>();
                for(int i=0;i<series.length();i++) {
                    JSONObject _item = series.getJSONObject(i);
                    String thumbnail = _item.getString("poster_url");
                    String title = _item.getString("name");
                    String pageUrl = "https://hanime.tv/api/v8/video?id=" + _item.getString("slug");
                    String meta = "";
                    ContentsItem content = new ContentsItem(thumbnail, pageUrl, title, meta);
                    contents.add(content);
                }
                if (detailListener != null) {
                    detailListener.onVideoLoad(videoUrl, null);
                    detailListener.onRelativeListLoad(contents);
                }
            }catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }, (err)-> {
            Toast.makeText(context, err.toString(), Toast.LENGTH_LONG).show();
            Log.d(TAG, err.toString());
        });
        queue.add(stringRequest);
    }

    @Override
    public void onLongClick(ContentsItem item) {}

}

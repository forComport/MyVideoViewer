package com.example.myvideoviewer.provider;

import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.myvideoviewer.contents.ContentsItem;
import com.example.myvideoviewer.contents.ContentsLoader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class XVideoLoader extends ContentsLoader {

    public static final String KEY = "XVideo";
    private String url = "https://www.xvideos.com/";
    private String url2 = "https://www.xvideos.com/new/";

    @Override
    public void init() {
        url = "https://www.xvideos.com/";
        url2 = "https://www.xvideos.com/new/";
        page = 0;
        loading = false;
    }

    @Override
    public void search(String keyword) {
        page = 0;
        url = "https://www.xvideos.com/?k=" + keyword;
        url2 = "https://www.xvideos.com/?k=" + keyword + "&p=";
        loadList();
    }

    public void loadList() {
        loading = true;
        String url;
        if (page == 0) {
            url = this.url;
        } else {
            url = url2+page;
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, (res)-> {
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("div.mozaique > div.thumb-block");
            ArrayList<ContentsItem> arr = new ArrayList<>();
            for(Element element : elements) {
                String thumbnail = element.select("img").attr("data-src");
                String title = element.select("p.title > a").text();
                String meta = element.select("p.metadata").text();
                String pageUrl = "https://www.xvideos.com" + element.select("a").attr("href");
                ContentsItem item = new ContentsItem(thumbnail, pageUrl, title, meta);
                arr.add(item);
            }
            listListener.onListLoad(arr);
            page += 1;
            loading = false;
        }, (err)-> {
            Toast.makeText(context, err.toString(), Toast.LENGTH_LONG).show();
            Log.d(TAG, err.toString());
        });
        queue.add(stringRequest);
    }

    public void loadDetail(ContentsItem item) {
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, item.pageUrl, (res)-> {
            String video = res.split("html5player\\.setVideoUrlHigh\\('")[1].split("'\\);")[0];
//            String video = res.split("html5player\\.setVideoUrlLow\\('")[1].split("'\\);")[0];
            if (detailListener != null) {
                detailListener.onVideoLoad(video);
            }
        }, (err)-> {
            Toast.makeText(context, err.toString(), Toast.LENGTH_LONG).show();
            Log.d(TAG, err.toString());
        });
        queue.add(stringRequest);
    }
}

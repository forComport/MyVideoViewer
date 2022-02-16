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

public class HentaizLoader extends ContentsLoader {

    public static final String KEY = "Hentaiz.top";
    private String url = "https://hentaiz.top/hentai-vietsub/page/";

    @Override
    public void init() {
        page = 1;
        loading = false;
    }

    @Override
    public void search(String keyword) {

    }


    public void loadList() {
        loading = true;
        String url = this.url + this.page + '/';

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, (res)-> {

            Document doc = Jsoup.parse(res);
            Elements elements = doc.select(".card");
            ArrayList<ContentsItem> arr = new ArrayList<>();
            for(Element element : elements) {
                String thumbnail = "https://hentaiz.top" + element.select(".card-img").attr("src");
                String title = element.select(".stretched-link").text();
                String meta = "";
                String pageUrl = element.select(".stretched-link").attr("href");
                ContentsItem item = new ContentsItem(thumbnail, pageUrl, title, meta);
                arr.add(item);
            }
            listListener.onListLoad(arr);
            page += 1;
            loading = false;
        }, err->{
            Log.d(TAG, err.toString());
        });
        queue.add(stringRequest);
    }

    @Override
    public void loadDetail(ContentsItem item) {
        Log.d(TAG, item.pageUrl);
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, item.pageUrl, (res)-> {
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("iframe");
            String iframe = elements.get(0).attr("src");
            String url = iframe.split("\\?url=")[1];


            Elements elements2 = doc.select(".card");
            ArrayList<ContentsItem> arr = new ArrayList<>();
            for(Element element : elements2) {
                String thumbnail = "https://hentaiz.top" + element.select(".card-img").attr("src");
                String title = element.select(".stretched-link").text();
                String meta = "";
                String pageUrl = element.select(".stretched-link").attr("href");
                ContentsItem item2 = new ContentsItem(thumbnail, pageUrl, title, meta);
                arr.add(item2);
            }

            if (url != null) {
                detailListener.onVideoLoad(url);
                detailListener.onRelativeListLoad(arr);
            } else {
                Toast.makeText(context, "링크 획득 실패", Toast.LENGTH_SHORT).show();
            }


        }, err->{});
        queue.add(stringRequest);
    }

    @Override
    public void onLongClick(ContentsItem item) {

    }
}

package com.example.myvideoviewer.provider;

import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.myvideoviewer.contents.ContentsItem;
import com.example.myvideoviewer.contents.ContentsLoader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MissavLoader extends ContentsLoader {

    public static final String KEY = "MissAV";
    private String url = "https://missav.com/ko/new?page=";

    @Override
    public void init() {
        url = "https://missav.com/ko/new?page=";
        page = 1;
        loading = false;
    }

    @Override
    public void search(String keyword) {
        page = 1;
//        url = "https://www.xvideos.com/?k=" + keyword.replace(" ", "+");
//        url2 = "https://www.xvideos.com/?k=" + keyword.replace(" ", "+") + "&p=";
        loadList();
    }

    public void loadList() {
        loading = true;
        String url = this.url + page;

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, (res)-> {
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("div.thumbnail");
            ArrayList<ContentsItem> arr = new ArrayList<>();
            for(Element element : elements) {
                String pageUrl = element.select("a").attr("href");
                String thumbnail = element.select("img").attr("data-src");
                String title = element.select("a.text-secondary").text();
                String meta = element.select("span").text();
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
        WebView webview = new WebView(context);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript("(function(){return document.body.innerHTML;})()", (html)->{
                    String piece = html.split("eval")[1].split("const video")[0];
                    String evalScript = "eval"+piece.substring(0, piece.length()-16).replace("\\\\'", "\\'");
                    view.evaluateJavascript("(function() { "+evalScript+"; return source480p; })();", (result)->{
                        String videoUrl = result.substring(1, result.length()-1);
                        Map<String, String> headers = new HashMap<>();
                        headers.put("origin", "https://missav.com");
                        headers.put("referer", item.pageUrl);
                        if (detailListener != null)
                            detailListener.onVideoLoad(videoUrl, headers);
                    });
                });
            }
        });
        webview.loadUrl(item.pageUrl);
    }

    @Override
    public void onLongClick(ContentsItem item) {}
}

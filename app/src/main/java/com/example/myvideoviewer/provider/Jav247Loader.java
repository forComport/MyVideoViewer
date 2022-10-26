package com.example.myvideoviewer.provider;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.myvideoviewer.contents.ContentsItem;
import com.example.myvideoviewer.contents.ContentsLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Jav247Loader extends ContentsLoader {

    public static final String KEY = "Jav247";
    private String url = "https://jav247.net/";
    private String url2 = "https://jav247.net/page/";


    @Override
    public void init() {
        url = "https://jav247.net/";
        url2 = "https://jav247.net/page/";
        page = 0;
        loading = false;
    }

    @Override
    public void search(String keyword) {
        try {
            page = Integer.parseInt(keyword);
            loadList();
        } catch (Exception e) {}
    }

    public void loadList() {
        loading = true;
        String url;
        if (page == 0) {
            url = this.url;
        } else {
            url = url2+page + "/";
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        Log.d(TAG, url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, (res)-> {
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("section.videos-list > div > div");
            ArrayList<ContentsItem> arr = new ArrayList<>();
            for(Element element : elements) {
                String thumbnail = element.select("img").attr("src");
                String title = element.select("h2").text();
                String meta = element.select(".post-meta").text();
                String pageUrl = element.select("a").attr("href");
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
        StringRequest stringRequest = new StringRequest(Request.Method.GET, item.pageUrl, (res)->{
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("iframe");
            String iframe = elements.get(0).attr("src");
            String id = iframe.split("/v/")[1].trim();
            StringRequest stringRequest1 = new StringRequest(Request.Method.POST, "https://jav247.top/api/source/"+id, (res1)-> {
                try {
                    JSONObject obj = new JSONObject(res1);
                    JSONArray data = obj.getJSONArray("data");
                    List<JSONObject> jsonValues = new ArrayList<>();
                    for(int i=0;i<data.length();i++) {
                        jsonValues.add(data.getJSONObject(i));
                    }
                    jsonValues.sort((JSONObject a, JSONObject b) -> {
                        try {
                            String label_a = a.getString("label");
                            int quality_a = Integer.parseInt(label_a.substring(0, label_a.length() - 1));
                            String label_b = b.getString("label");
                            int quality_b = Integer.parseInt(label_b.substring(0, label_b.length() - 1));
                            return quality_a - quality_b;
                        } catch (JSONException ex) {
                            return 0;
                        }
                    });
                    if (jsonValues.size() > 0) {
                        String redirect_url = jsonValues.get(0).getString("file");
                        Log.d(TAG, redirect_url);
                        new AsyncTask<Void, Void, String>() {
                            protected String doInBackground(Void... voids) {
                                try {
                                    URL url = new URL(redirect_url);
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setRequestMethod("GET");
                                    conn.connect();
                                    int status = conn.getResponseCode();

                                    Log.d(TAG, "status - " + status);
                                    Log.d(TAG, "url - " + conn.getURL().toString());
                                    for(String key : conn.getHeaderFields().keySet()) {
                                        Log.d(TAG, "key - " + key + " : " + conn.getHeaderField(key));
                                    }
                                    conn.disconnect();
                                    if (status == 200) {
                                        return conn.getURL().toString();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, e.toString());
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(String url) {
                                if (url != null) {
                                    detailListener.onVideoLoad(url, null);
                                } else {
                                    Toast.makeText(context, "링크 획득 실패", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }.execute();
                    }
                } catch (JSONException ex) {
                    Toast.makeText(context, ex.toString(), Toast.LENGTH_SHORT).show();
                }
            }, (err)-> {
                Toast.makeText(context, err.toString(), Toast.LENGTH_SHORT).show();
            });
                queue.add(stringRequest1);
            }, (err)-> {

        });
        queue.add(stringRequest);
    }

    @Override
    public void onLongClick(ContentsItem item) {}
}

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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

public class JavhdLoader extends ContentsLoader {

    public static final String KEY = "JavHd";
    private String url = "https://www2.javhdporn.net/category/censored/";
    private String url2 = "https://www2.javhdporn.net/category/censored/page/";
    @Override
    public void init() {
        url = "https://www2.javhdporn.net/category/censored/";
        url2 = "https://www2.javhdporn.net/category/censored/page/";
        page = 0;
        loading = false;
    }

    @Override
    public void search(String keyword) {

    }

    @Override
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
            Elements elements = doc.select("article");
            ArrayList<ContentsItem> arr = new ArrayList<>();
            for(Element element : elements) {
                String thumbnail = element.select("img").get(1).attr("src");
                String title = element.select("header").text();
                String meta = element.select(".duration").text();
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
    @Override
    public void loadDetail(ContentsItem item) {
        Log.d(TAG, item.pageUrl);
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, item.pageUrl, (res)->{
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("link[rel='shortlink']");
            String link = elements.get(0).attr("href");
            String number = link.split("p=")[1];
            String token = doc.select("#video-player").get(0).attr("mpu-data");
            String page = parseToken(token, number, "_0x596811");

            StringRequest stringRequest2 = new StringRequest(Request.Method.GET, "https:"+page, (res2)->{
                String token2 = res2.split("f8_0x5add\\('")[1].split("',")[0];
                Document doc2 = Jsoup.parse(res2);
                String number2 = doc2.select("meta").get(2).attr("content");
                String page2 = parseToken(token2, number2, "_0x583715");
                Log.d(TAG, page2);
                String vid = page2.split("/")[page2.split("/").length-1];
                StringRequest stringRequest3 = new StringRequest(Request.Method.POST, "https://mm9844.com/api/source/"+vid, (res3)->{
                    try {
                        JSONObject obj = new JSONObject(res3);
                        JSONArray data = obj.getJSONArray("data");
                        for(int i=0;i<data.length();i++) {
                            JSONObject jitem = data.getJSONObject(i);
                            String redirection_url = jitem.getString("file");
                            getRealUrl(redirection_url);
                            break;
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.toString());
                    }
                },(err)->{
                    Log.d(TAG, err.toString());
                });
                queue.add(stringRequest3);
            },(err)->{
                Log.d(TAG, err.toString());
            });
            queue.add(stringRequest2);

        }, (err)-> {
        });
        queue.add(stringRequest);
    }

    private void getRealUrl(String redirect_url) {
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
                    detailListener.onVideoLoad(url);
                } else {
                    Toast.makeText(context, "링크 획득 실패", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private String parseToken(String token, String number, String key) {

        String encoded = new String(Base64.getEncoder().encode((number+key).getBytes()));
        ArrayList _num = new ArrayList(Arrays.asList(encoded.split("")));
        Collections.reverse(_num);
        String num = String.join("", _num);
        ArrayList arr = new ArrayList<Integer>();
        for(int i=0;i<256;i++) {
            arr.add(i);
        }
        int zero = 0;
        int var1 = 0;
        for (int i = 0; i < 256; i++) {
            zero = (zero + (int)arr.get(i) + Character.codePointAt(num, i % num.length())) % 256;
            var1 = (int)arr.get(i);
            arr.set(i,arr.get(zero));
            arr.set(zero, var1);
        }
        int i = 0;
        zero = 0;

        String result = "";
        byte[] bytes = Base64.getDecoder().decode(token);
        for (int j = 0; j < bytes.length; j++) {
            i = (i + 1) % 256;
            zero = (zero + (int) arr.get(i)) % 256;
            var1 = (int) arr.get(i);
            arr.set(i, arr.get(zero));
            arr.set(zero, var1);
            int charcode = Byte.toUnsignedInt(bytes[j]);
//            Log.d(TAG, (charcode ^ (int) arr.get(((int) arr.get(i) + (int) arr.get(zero)) % 256)) + " : " + charcode + ", " + (int) arr.get(((int) arr.get(i) + (int) arr.get(zero)) % 256));
            result += fromCharCode(charcode ^ (int) arr.get(((int) arr.get(i) + (int) arr.get(zero)) % 256));
        }
        return new String(Base64.getDecoder().decode(result));
    }

    private String fromCharCode(int codePoints) {
        return (char)codePoints + "";
    }
}

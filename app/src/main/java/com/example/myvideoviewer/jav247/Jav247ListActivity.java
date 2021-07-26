package com.example.myvideoviewer.jav247;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.myvideoviewer.R;
import com.example.myvideoviewer.VideoActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Jav247ListActivity extends AppCompatActivity {
    private static final String TAG = "Jav247ListActivityTAG";
    Jav247DbHelper dbHelper;
    private boolean syncing = false;
    private ArrayList<JavItem> javItems = new ArrayList<>();
    CustomAdapter adapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.jav247_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sync:
                if (!syncing) {
                    syncing = true;
                    syncDb();
                } else {
                    Toast.makeText(this, "동기화 중...", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivty_jav247);
        dbHelper = new Jav247DbHelper(this);
        ListView listView = findViewById(R.id.jav247_list);
        adapter = new CustomAdapter(this, dbHelper.read(), dbHelper);
        listView.setAdapter(adapter);
    }

    private void syncDb() {
        Cursor c = dbHelper.read();
        String lastTitle = "";
        if (c.getCount() != 0) {
            c.moveToLast();
            int titleColumn = c.getColumnIndexOrThrow(Jav247DbHelper.Jav247Table.TITLE);
            lastTitle = c.getString(titleColumn);
        }
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://jav247.net";
        String finalLastTitle = lastTitle;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, (res)->{
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("a.page-link");
            String href = elements.get(elements.size()-1).attr("href");
            int last_page = Integer.parseInt(href.split("page/")[1].split("/")[0]);
            read_page(1, last_page, finalLastTitle);
        }, (err)-> {
            Toast.makeText(this, err.toString(), Toast.LENGTH_SHORT).show();
        });
        queue.add(stringRequest);
    }

    private void read_page(int page, int last_page, String lastTitle) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://jav247.net/page/"+page;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, (res)-> {
            Log.d(TAG, url);
            Document doc = Jsoup.parse(res);
            Elements elements = doc.select("article");
            boolean skipNext = false;
            for(Element element : elements) {
                JavItem item = new JavItem();
                item.thumbnail = element.select("img").get(0).attr("src");
                item.page_url = element.select("a").get(0).attr("href");
                item.title = element.select("h2").text();
                item.created = element.select(".post-time").text();
                if (lastTitle.equals(item.title)) {
                    skipNext = true;
                    break;
                }
                javItems.add(item);
            }
            if(page < last_page && !skipNext) {
                read_page(page+1, last_page, lastTitle);
            } else {
                updateDB();
            }
        }, (err)-> {
            Toast.makeText(this, err.toString(), Toast.LENGTH_SHORT).show();
        });
        queue.add(stringRequest);
    }

    private void updateDB() {
        Collections.reverse(javItems);
        for(JavItem item : javItems) {
            dbHelper.insert(item.title, item.thumbnail, item.created, item.page_url);
        }
        javItems.clear();
        syncing = false;
        Toast.makeText(this, "동기화 완료", Toast.LENGTH_LONG).show();
        adapter.loadList(dbHelper.read());
        adapter.notifyDataSetChanged();
    }

    private static class CustomAdapter extends BaseAdapter {
        private ArrayList<JavItem> itemList = new ArrayList<>();
        private Jav247DbHelper dbHelper;
        private Context context;
        public CustomAdapter(Context context, Cursor c, Jav247DbHelper dbHelper) {
            loadList(c);
            this.context = context;
            this.dbHelper = dbHelper;
        }

        public void loadList(Cursor cursor) {
            itemList.clear();
            int idColumn = cursor.getColumnIndexOrThrow(Jav247DbHelper.Jav247Table._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(Jav247DbHelper.Jav247Table.TITLE);
            int thumbnailColumn = cursor.getColumnIndexOrThrow(Jav247DbHelper.Jav247Table.THUMBNAIL);
            int pageUrlColumn = cursor.getColumnIndexOrThrow(Jav247DbHelper.Jav247Table.PAGE_URL);
            int createdColumn = cursor.getColumnIndexOrThrow(Jav247DbHelper.Jav247Table.CREATED);
            int stateColumn = cursor.getColumnIndexOrThrow(Jav247DbHelper.Jav247Table.STATE);
            while(cursor.moveToNext()) {
                JavItem item = new JavItem();
                item.id = cursor.getInt(idColumn);
                item.title = cursor.getString(titleColumn);
                item.thumbnail = cursor.getString(thumbnailColumn);
                item.page_url = cursor.getString(pageUrlColumn);
                item.created = cursor.getString(createdColumn);
                item.state = cursor.getString(stateColumn);
                itemList.add(item);
            }
        }

        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public JavItem getItem(int position) {
            return itemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.jav247_item, parent, false);
            }
            JavItem item = getItem(position);
            TextView titleView = convertView.findViewById(R.id.title);
            ImageView thumbnailView = convertView.findViewById(R.id.thumbnail);
            TextView createdView = convertView.findViewById(R.id.created);
            Button stateView = convertView.findViewById(R.id.state);
            Button linkView = convertView.findViewById(R.id.link);
            linkView.setOnClickListener((v)-> {
                findVideo(item.page_url, (url)->{
                    Toast.makeText(context, url, Toast.LENGTH_SHORT).show();
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("label", url);
                    clipboard.setPrimaryClip(clip);
                });
            });
            titleView.setText(item.title);
            Glide.with(convertView)
                    .load(item.thumbnail)
                    .into(thumbnailView);
            createdView.setText(item.created);
            stateView.setText(item.state);
            stateView.setOnClickListener((v)->{
                if("신규".equals(item.state)) {
                    findVideo(item.page_url, (url)->{
                        dbHelper.update(item.id, "다운중");
                        item.state = "다운중";
                        notifyDataSetChanged();
                        download(url, item.title);
                    });
                } else if ("삭제".equals(item.state)) {
                    dialog(item, "숨김");
                }
            });
            convertView.setOnClickListener((v)->{
                if ("삭제".equals(item.state)) {
                    watchVideo(item);
                } else if("신규".equals(item.state)) {
                    dialog(item, "숨김");
                } else {
                    dialog(item, "신규");
                }
            });
            return convertView;
        }

        public void removeVideo(JavItem item) {
            Cursor c = searchVideo(item);
            if (c == null) {
                return;
            }
            int idColumn = c.getColumnIndex(DownloadManager.COLUMN_ID);
            int id = c.getInt(idColumn);
            DownloadManager dm = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
            dm.remove(id);
        }

        public Cursor searchVideo(JavItem item) {
            DownloadManager dm = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();
            Cursor c = dm.query(query);
            int titleColumn = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
            while (c.moveToNext()) {
                String title = c.getString(titleColumn);
                if (item.title.equals(title)) {
                    return c;
                }
            }
            Toast.makeText(context, "영상을 찾지 못헀습니다.", Toast.LENGTH_LONG).show();
            return null;
        }

        public void watchVideo(JavItem item) {
            Cursor c = searchVideo(item);
            if (c == null) {
                return;
            }
            int columnLocalUri = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
            Intent intent = new Intent(context, VideoActivity.class);
            intent.putExtra("title", item.title);
            intent.putExtra("localUri", c.getString(columnLocalUri));
            context.startActivity(intent);
        }

        public void dialog(JavItem item, String state) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog);
            dialog.setMessage(state + " 상태로 변경하시겠습니까?")
                    .setTitle("상태변경")
                    .setPositiveButton("예",(d, which)->{
                        if ("숨김".equals(state)) {
                            removeVideo(item);
                        }
                        dbHelper.update(item.id, state);
                        item.state = state;
                        notifyDataSetChanged();
                    })
                    .setNegativeButton("아니오", (d,which)-> {
                        d.cancel();
                    })
                    .setCancelable(false)
                    .show();
        }

        public void download(String url, String title) {
            Uri uri = Uri.parse(url);
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(title);
            request.setDescription("다운로드중...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS,
                    uri.getLastPathSegment());
            downloadManager.enqueue(request);
        }

        public void findVideo(String pageUrl, Listener listener) {
            RequestQueue queue = Volley.newRequestQueue(context);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, pageUrl, (res)->{
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
                                return quality_b - quality_a;
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
                                        listener.callback(url);
                                    } else {
                                        Toast.makeText(context, "링크 획득 실패", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }.execute();
//                            StringRequest stringRequest2 = new StringRequest(Request.Method.GET, redirect_url, (res2)->{
//                                Log.d(TAG, res2);
//                            }, (err)-> {
//                                Log.d(TAG, err.toString());
//                                Toast.makeText(context, err.toString(), Toast.LENGTH_SHORT).show();
//                            });
//                            queue.add(stringRequest2);
//                            listener.callback(jsonValues.get(0).getString("file"));
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
    }

    interface Listener {
        void callback(String str);
    }


    private static class JavItem {
        int id;
        String title;
        String thumbnail;
        String created;
        String page_url;
        String state;
    }
}

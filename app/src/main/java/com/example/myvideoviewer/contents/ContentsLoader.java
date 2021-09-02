package com.example.myvideoviewer.contents;

import android.content.Context;

import com.example.myvideoviewer.provider.Jav247Loader;
import com.example.myvideoviewer.provider.XVideoLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class ContentsLoader {
    protected static final String TAG = "ContentsLoaderTAG";
    protected int page = 0;
    protected Context context;
    public boolean loading = false;

    public static Map<String, ContentsLoader> Provider = new HashMap<String, ContentsLoader>() {{
        put(XVideoLoader.KEY,new XVideoLoader());
        put(Jav247Loader.KEY, new Jav247Loader());
    }};
    public interface Listener {
        void onListLoad(ArrayList<ContentsItem> items);
        void onVideoLoad(String url);
    }
    protected Listener listener;

    public ContentsLoader setContext(Context context) {
        this.context = context;
        return this;
    }

    public void setOnListener(Listener listener) {
        this.listener = listener;
    }

    public abstract void search(String keyword);

    public abstract void loadList();

    public abstract void loadDetail(ContentsItem item);

}

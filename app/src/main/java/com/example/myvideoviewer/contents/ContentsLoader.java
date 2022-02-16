package com.example.myvideoviewer.contents;

import android.content.Context;

import com.example.myvideoviewer.provider.HanimeLoader;
import com.example.myvideoviewer.provider.HentaizLoader;
import com.example.myvideoviewer.provider.Jav247Loader;
import com.example.myvideoviewer.provider.JavhdLoader;
import com.example.myvideoviewer.provider.LibraryLoader;
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
        put(HanimeLoader.KEY, new HanimeLoader());
        put(HentaizLoader.KEY, new HentaizLoader());
        put(JavhdLoader.KEY, new JavhdLoader());
        put(LibraryLoader.KEY, new LibraryLoader());
    }};
    public interface ListListener {
        void onListLoad(ArrayList<ContentsItem> items);
    }
    public interface DetailListener {
        void onVideoLoad(String url);
        void onRelativeListLoad(ArrayList<ContentsItem> items);
    }

    protected ListListener listListener;
    protected DetailListener detailListener;

    public ContentsLoader setContext(Context context) {
        this.context = context;
        return this;
    }

    public abstract void init();

    public void setOnListListener(ListListener listener) {
        this.listListener = listener;
    }
    public void setOnDetailListener(DetailListener listener) {
        this.detailListener = listener;
    }

    public abstract void search(String keyword);

    public abstract void loadList();

    public abstract void loadDetail(ContentsItem item);

    public abstract void onLongClick(ContentsItem item);
}

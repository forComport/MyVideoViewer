package com.example.myvideoviewer;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.myvideoviewer.contents.ContentsLoader;
import com.example.myvideoviewer.contents.ListActivity;
//import com.example.myvideoviewer.vr.VRActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout group = findViewById(R.id.buttonGroup);
        for(String key : ContentsLoader.Provider.keySet()) {
            Button button = new Button(this);
            button.setText(key);
            button.setOnClickListener((v)->{
                Intent intent = new Intent(getApplicationContext(), ListActivity.class);
                intent.putExtra("provider", key);
                startActivity(intent);
            });
            group.addView(button);
        }

        Button button = new Button(this);
        button.setText("WebView");
        button.setOnClickListener((v)->{
            Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
            startActivity(intent);
        });
        group.addView(button);

//        Button button2 = new Button(this);
//        button2.setText("VR");
//        button2.setOnClickListener((v)-> {
//            Intent intent = new Intent(getApplicationContext(), VRActivity.class);
//            startActivity(intent);
//        });
//        group.addView(button2);

    }

}
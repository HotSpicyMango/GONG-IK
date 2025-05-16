package com.example.myapplication;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);  // XML 연결!

        TextView textInfo = findViewById(R.id.textInfo);
        try {
            InputStream is = getAssets().open("info.html");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String html = new String(buffer, "UTF-8");

            textInfo.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        } catch (IOException e) {
            e.printStackTrace();
            textInfo.setText("안내문을 불러오지 못했습니다.");
        }

    }
}
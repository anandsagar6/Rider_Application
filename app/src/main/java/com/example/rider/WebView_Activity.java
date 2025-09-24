package com.example.rider;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WebView_Activity extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient()); // Open links in the app

        // Get URL and title from intent
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        setTitle(title);

        if (url != null) {
            webView.loadUrl(url);
        }
    }
}

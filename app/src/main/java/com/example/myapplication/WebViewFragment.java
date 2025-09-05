package com.example.myapplication; // ← 패키지명 꼭 맞춰주세요

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebViewFragment extends Fragment {
    private static final String ARG_FILE_NAME = "file_name";

    public static WebViewFragment newInstance(String fileName) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_NAME, fileName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        WebView webView = new WebView(requireContext());
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WebSettings settings = webView.getSettings();
            settings.setForceDark(WebSettings.FORCE_DARK_AUTO); // 또는 FORCE_DARK_ON
        }

        String fileName = getArguments().getString(ARG_FILE_NAME, "info.html");
        webView.loadUrl("file:///android_asset/" + fileName);

        return webView;
    }
}
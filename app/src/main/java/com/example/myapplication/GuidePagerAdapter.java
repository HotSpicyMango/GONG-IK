package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class GuidePagerAdapter extends FragmentStateAdapter {

    public GuidePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public GuidePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String fileName;
        switch (position) {
            case 0:
                fileName = "info.html";
                break;
            case 1:
                fileName = "info_sick.html";
                break;
            case 2:
                fileName = "info_official.html";
                break;
            case 3:
                fileName = "info_petition.html";
                break;
            case 4:
                fileName = "info_special.html";
                break;
            case 5:
                fileName = "info_etc.html"; // 추가된 부분
                break;
            default:
                fileName = "info.html";
        }
        return WebViewFragment.newInstance(fileName);
    }

    @Override
    public int getItemCount() {
        return 6; // 연가, 병가, 공가, 청원휴가, 특별휴가, 정보
    }
}
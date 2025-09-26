package com.example.myapplication;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide); // XML 파일명과 연결

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int textColor = ContextCompat.getColor(this, android.R.color.white);
        toolbar.setTitleTextColor(textColor);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(textColor);
        }
        getSupportActionBar().setTitle("");

        // 상태바 높이만큼 padding 추가
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // 💡 다크모드일 때 툴바 배경 어둡게 설정
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            toolbar.setBackgroundColor(Color.parseColor("#1f1f1f")); // 어두운 회색
        } else {
            toolbar.setBackgroundColor(Color.parseColor("#1976D2")); // 라이트모드에서 진한 회색
        }

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            getWindow().setStatusBarColor(Color.parseColor("#121212")); // 시스템 UI 색도 어둡게
        } else {
            getWindow().setStatusBarColor(Color.parseColor("#212121")); // 라이트모드에서 진한 회색
        }

        // ViewPager2 + TabLayout 연결
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        // 어댑터 설정
        GuidePagerAdapter adapter = new GuidePagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 탭 제목 연결
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("연가");
                            break;
                        case 1:
                            tab.setText("병가");
                            break;
                        case 2:
                            tab.setText("공가");
                            break;
                        case 3:
                            tab.setText("청원");
                            break;
                        case 4:
                            tab.setText("특별");
                            break;
                        case 5:
                            tab.setText("정보");
                            break;
                    }
                }
        ).attach();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
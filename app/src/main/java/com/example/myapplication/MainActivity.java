package com.example.myapplication;

import static java.util.logging.Level.parse;
import androidx.appcompat.app.AppCompatDelegate;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.myapplication.R;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.EditText;
import android.content.Context;


import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView textViewEnlistDate;

    private Button buttonSelectDate;
    private TextView textViewDischargeDate, textViewDday, textViewProgressPercent;
    private ProgressBar progressBar;

    private Calendar enlistDate = null;
    private Calendar dischargeDate = null;

    private Handler handler = new Handler();
    private Runnable runnable;

    public static class LeaveData {
        private int days;
        private int hours;
        private int minutes;

        public LeaveData(int days, int hours, int minutes) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            normalize();
        }

        public LeaveData() {
            this(0, 0, 0);
        }

        public int getDays() {
            return days;
        }

        public int getHours() {
            return hours;
        }

        public int getMinutes() {
            return minutes;
        }

        public void set(int d, int h, int m) {
            this.days = d;
            this.hours = h;
            this.minutes = m;
            normalize();
        }

        public void subtract(int d, int h, int m) {
            int total = toMinutes();
            int subtract = d * 480 + h * 60 + m;
            total = Math.max(0, total - subtract);
            fromMinutes(total);
        }

        public int toMinutes() {
            return days * 480 + hours * 60 + minutes;
        }

        public void fromMinutes(int totalMinutes) {
            this.days = totalMinutes / 480;
            totalMinutes %= 480;
            this.hours = totalMinutes / 60;
            this.minutes = totalMinutes % 60;
        }

        private void normalize() {
            if (minutes >= 60) {
                hours += minutes / 60;
                minutes %= 60;
            }
            if (hours >= 24) {
                days += hours / 24;
                hours %= 24;
            }
        }

        public String toDisplayString(String label) {
            return label + " 잔여 : " + days + "일 " + hours + "시간 " + minutes + "분";
        }
    }

    // 연가 및 병가 데이터 변수
    private LeaveData leave1Year;
    private LeaveData leave2Year;
    private LeaveData sickLeave;



    // 복무기간 21개월 고정
    private final int SERVICE_MONTHS = 21;

    private void updateLeaveDisplay() {
        textRemaining1.setText(leave1Year.toDisplayString("1년차 연가"));
        textRemaining2.setText(leave2Year.toDisplayString("2년차 연가"));
        textRemaining3.setText(sickLeave.toDisplayString("병가"));
    }

    private TextView textRemaining1, textRemaining2, textRemaining3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                // 라이트 모드일 때만 상태바 아이콘을 어둡게
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                // 다크 모드일 땐 아이콘 색상 플래그 제거 (자동으로 밝게)
                getWindow().getDecorView().setSystemUiVisibility(0);
            }
        }


        // ✅ 1. View 바인딩 먼저
        textViewEnlistDate = findViewById(R.id.textViewDischargeDate2);
        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        textViewDischargeDate = findViewById(R.id.textViewDischargeDate);
        textViewDday = findViewById(R.id.textViewDday);
        textViewProgressPercent = findViewById(R.id.textViewProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(10000);

        textRemaining1 = findViewById(R.id.remaining1);
        textRemaining2 = findViewById(R.id.remaining2);
        textRemaining3 = findViewById(R.id.remaining3);

        EditText editDays1 = findViewById(R.id.editDays1);
        EditText editHours1 = findViewById(R.id.editHours1);
        EditText editMinutes1 = findViewById(R.id.editMinutes1);
        Button buttonMinus1 = findViewById(R.id.buttonMinus1);

        EditText editDays2 = findViewById(R.id.editDays2);
        EditText editHours2 = findViewById(R.id.editHours2);
        EditText editMinutes2 = findViewById(R.id.editMinutes2);
        Button buttonMinus2 = findViewById(R.id.buttonMinus2);

        EditText editDays3 = findViewById(R.id.editDays3);
        EditText editHours3 = findViewById(R.id.editHours3);
        EditText editMinutes3 = findViewById(R.id.editMinutes3);
        Button buttonMinus3 = findViewById(R.id.buttonMinus3);

        ProgressBar progress1 = findViewById(R.id.progress1);
        ProgressBar progress2 = findViewById(R.id.progress2);
        ProgressBar progress3 = findViewById(R.id.progress3);

        Button buttonReset = findViewById(R.id.button2);

        // 휴가 Progress 최대값
        progress1.setMax(15 * 480);
        progress2.setMax(13 * 480);
        progress3.setMax(30 * 480);

        //핸들러 정의
        runnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
                handler.postDelayed(this, 45);
            }
        };

        // ✅ 4. SharedPreferences 복원
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        if (prefs.contains("enlistMillis")) {
            enlistDate = Calendar.getInstance();
            enlistDate.setTimeInMillis(prefs.getLong("enlistMillis", 0));
        }
        if (prefs.contains("dischargeMillis")) {
            dischargeDate = Calendar.getInstance();
            dischargeDate.setTimeInMillis(prefs.getLong("dischargeMillis", 0));
        }

        leave1Year = new LeaveData(
                prefs.getInt("leave1_days", 15),
                prefs.getInt("leave1_hours", 0),
                prefs.getInt("leave1_minutes", 0)
        );
        leave2Year = new LeaveData(
                prefs.getInt("leave2_days", 13),
                prefs.getInt("leave2_hours", 0),
                prefs.getInt("leave2_minutes", 0)
        );
        sickLeave = new LeaveData(
                prefs.getInt("sick_days", 30),
                prefs.getInt("sick_hours", 0),
                prefs.getInt("sick_minutes", 0)
        );

        // ✅ 5. 휴가 UI 갱신
        updateLeaveDisplay();
        progress1.setProgress(leave1Year.toMinutes());
        progress2.setProgress(leave2Year.toMinutes());
        progress3.setProgress(sickLeave.toMinutes());

        // ✅ 6. 날짜 복원 시 진행률 시작
        if (enlistDate != null && dischargeDate != null) {
            updateProgress();
            handler.post(runnable);

            // 🔹 전역일 텍스트 표시 추가
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
            textViewEnlistDate.setText("입영일 : " + sdf.format(enlistDate.getTime()));
            textViewDischargeDate.setText("전역일 : " + sdf.format(dischargeDate.getTime()));

            updateProgress();
            handler.post(runnable);
        }

        if (enlistDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
            textViewEnlistDate.setText("입영일 : " + sdf.format(enlistDate.getTime()));
        }

        // ✅ 7. 버튼 이벤트

        Button btnGuide = findViewById(R.id.btnGuide);
        btnGuide.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GuideActivity.class);
            startActivity(intent);
        });

        buttonMinus1.setOnClickListener(v -> {
            leave1Year.subtract(parse(editDays1), parse(editHours1), parse(editMinutes1));
            updateLeaveDisplay();
            progress1.setProgress(leave1Year.toMinutes());

            editDays1.setText("");
            editHours1.setText("");
            editMinutes1.setText("");
        });

        buttonMinus2.setOnClickListener(v -> {
            leave2Year.subtract(parse(editDays2), parse(editHours2), parse(editMinutes2));
            updateLeaveDisplay();
            progress2.setProgress(leave2Year.toMinutes());

            editDays2.setText("");
            editHours2.setText("");
            editMinutes2.setText("");
        });

        buttonMinus3.setOnClickListener(v -> {
            sickLeave.subtract(parse(editDays3), parse(editHours3), parse(editMinutes3));
            updateLeaveDisplay();
            progress3.setProgress(sickLeave.toMinutes());

            editDays3.setText("");
            editHours3.setText("");
            editMinutes3.setText("");
        });

        buttonReset.setOnClickListener(v -> {
            String[] options = {"전역일 초기화", "연가/병가 초기화", "전체 초기화"};
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("초기화 항목 선택")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: resetDischarge(); break;
                            case 1: resetLeave(); break;
                            case 2:
                                resetDischarge();
                                resetLeave();
                                break;
                        }
                    })
                    .show();
        });

        buttonSelectDate.setOnClickListener(v -> showDatePicker());
    }

    private void resetDischarge() {
        enlistDate = null;
        dischargeDate = null;
        textViewEnlistDate.setText("입영일 : ");
        textViewDischargeDate.setText("전역일 : ");
        textViewDday.setText("D - ");
        textViewProgressPercent.setText("0.0000000%");
        progressBar.setProgress(0);
        handler.removeCallbacks(runnable);

        // ✅ SharedPreferences 값도 삭제
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("enlistMillis");
        editor.remove("dischargeMillis");
        editor.apply();
    }

    private void resetLeave() {
        leave1Year.set(15, 0, 0);
        leave2Year.set(13, 0, 0);
        sickLeave.set(30, 0, 0);
        updateLeaveDisplay();

        ProgressBar progress1 = findViewById(R.id.progress1);
        ProgressBar progress2 = findViewById(R.id.progress2);
        ProgressBar progress3 = findViewById(R.id.progress3);
        progress1.setProgress(leave1Year.toMinutes());
        progress2.setProgress(leave2Year.toMinutes());
        progress3.setProgress(sickLeave.toMinutes());

        // ✅ SharedPreferences 초기화
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("leave1_days");
        editor.remove("leave1_hours");
        editor.remove("leave1_minutes");

        editor.remove("leave2_days");
        editor.remove("leave2_hours");
        editor.remove("leave2_minutes");

        editor.remove("sick_days");
        editor.remove("sick_hours");
        editor.remove("sick_minutes");

        editor.apply();
    }


    private int parse(EditText editText) {
        String str = editText.getText().toString().trim();
        if (str.isEmpty()) return 0;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    enlistDate = Calendar.getInstance();
                    enlistDate.set(year, month, dayOfMonth, 0, 0, 0);
                    enlistDate.set(Calendar.MILLISECOND, 0);

                    dischargeDate = (Calendar) enlistDate.clone();
                    dischargeDate.add(Calendar.MONTH, SERVICE_MONTHS);
                    dischargeDate.add(Calendar.DAY_OF_MONTH, -1);  // 하루 빼기

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
                    textViewEnlistDate.setText("입영일 : " + sdf.format(enlistDate.getTime()));
                    textViewDischargeDate.setText("전역일 : " + sdf.format(dischargeDate.getTime()));

                    updateProgress();

                    handler.removeCallbacks(runnable);
                    handler.post(runnable);
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void updateProgress() {
        if (enlistDate == null || dischargeDate == null) {
            textViewDday.setText("D -");
            textViewProgressPercent.setText("0.0000000%");
            progressBar.setProgress(0);
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long enlistMillis = enlistDate.getTimeInMillis();
        long dischargeMillis = dischargeDate.getTimeInMillis();

        if (nowMillis < enlistMillis) {
            textViewDday.setText("입대 전");
            textViewProgressPercent.setText("0.0000000%");
            progressBar.setProgress(0);
        } else if (nowMillis >= dischargeMillis) {
            textViewDday.setText("전역");
            textViewProgressPercent.setText("100.0000000%");
            progressBar.setProgress(progressBar.getMax());
        } else {
            long diffMillis = dischargeMillis - nowMillis;
            long diffDays = (long) Math.ceil((double) diffMillis / (24 * 60 * 60 * 1000));
            textViewDday.setText(String.format("D - %d", diffDays));


            long elapsedMillis = nowMillis - enlistMillis;
            double progress = (double) elapsedMillis / (dischargeMillis - enlistMillis);
            if (progress < 0) progress = 0;
            if (progress > 1) progress = 1;

            int progressValue = (int) (progress * progressBar.getMax());
            progressBar.setProgress(progressValue);

            String percentStr = String.format(Locale.getDefault(), "%.7f%%", progress * 100);
            textViewProgressPercent.setText(percentStr);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (enlistDate != null) {
            editor.putLong("enlistMillis", enlistDate.getTimeInMillis());
        }

        if (enlistDate != null) {
            editor.putLong("enlistMillis", enlistDate.getTimeInMillis());
        }

        if (dischargeDate != null) {
            editor.putLong("dischargeMillis", dischargeDate.getTimeInMillis());
        }

        // 휴가 정보 저장
        editor.putInt("leave1_days", leave1Year.getDays());
        editor.putInt("leave1_hours", leave1Year.getHours());
        editor.putInt("leave1_minutes", leave1Year.getMinutes());

        editor.putInt("leave2_days", leave2Year.getDays());
        editor.putInt("leave2_hours", leave2Year.getHours());
        editor.putInt("leave2_minutes", leave2Year.getMinutes());

        editor.putInt("sick_days", sickLeave.getDays());
        editor.putInt("sick_hours", sickLeave.getHours());
        editor.putInt("sick_minutes", sickLeave.getMinutes());

        editor.apply();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();

                    // 키보드 숨기기
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
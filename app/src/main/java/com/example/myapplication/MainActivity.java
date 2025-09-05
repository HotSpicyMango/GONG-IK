package com.example.myapplication;

import androidx.appcompat.app.AppCompatDelegate;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.content.Context;
import android.view.Choreographer;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import android.view.ViewGroup;
import android.view.ViewConfiguration;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "app_prefs";
    private static final String PREF_FIRST_RUN = "first_run";

    private TextView textViewEnlistDate;

    private TextView textViewDischargeDate, textViewDday, textViewProgressPercent;
    private ProgressBar progressBar;

    private Calendar enlistDate = null;
    private Calendar dischargeDate = null;

    private Choreographer.FrameCallback frameCallback;
    private boolean isGuideShowing = false;
    // 터치 제스처 판별용 (스크롤 중에는 키보드 유지)
    private float touchDownX, touchDownY;
    private boolean isTapCandidate = false;
    private int touchSlop;
    private int tapTimeout;
    // === 1년차 카드 제어용 UI 필드 ===
    private EditText editDays1, editHours1, editMinutes1;
    private Button buttonMinus1;
    private ProgressBar progress1;
    private LinearLayout card1; // 1년차 카드 루트 레이아웃

    public static class LeaveData {
        private int days;
        private int hours;
        private int minutes;

        // 1일 = 8시간(=480분) 기준으로 환산
        private static final int MINUTES_PER_HOUR = 60;
        private static final int HOURS_PER_DAY = 8;
        private static final int MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR;

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
            int subtract = d * MINUTES_PER_DAY + h * MINUTES_PER_HOUR + m;
            total = Math.max(0, total - subtract);
            fromMinutes(total);
        }

        public int toMinutes() {
            return days * MINUTES_PER_DAY + hours * MINUTES_PER_HOUR + minutes;
        }

        public void fromMinutes(int totalMinutes) {
            this.days = totalMinutes / MINUTES_PER_DAY;
            totalMinutes %= MINUTES_PER_DAY;
            this.hours = totalMinutes / MINUTES_PER_HOUR;
            this.minutes = totalMinutes % MINUTES_PER_HOUR;
        }

        private void normalize() {
            if (minutes >= MINUTES_PER_HOUR) {
                hours += minutes / MINUTES_PER_HOUR;
                minutes %= MINUTES_PER_HOUR;
            }
            if (hours >= HOURS_PER_DAY) {
                days += hours / HOURS_PER_DAY;
                hours %= HOURS_PER_DAY;
            }
        }

        public String toDisplayString(String label) {
            return label + " : " + days + "일 " + hours + "시간 " + minutes + "분";
        }
    }

    // 연가 및 병가 데이터 변수
    private LeaveData leave1Year;
    private LeaveData leave2Year;
    private LeaveData sickLeave;

    // 복무기간 21개월 고정
    private final int SERVICE_MONTHS = 21;

    private void updateLeaveDisplay() {
        textRemaining1.setText(leave1Year.toDisplayString("총 15일 중 남은 1년차 연가"));
        textRemaining2.setText(leave2Year.toDisplayString("총 13일 중 남은 2년차 연가"));
        textRemaining3.setText(sickLeave.toDisplayString("총 30일 중 남은 병가"));
    }

    private TextView textRemaining1, textRemaining2, textRemaining3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 가장 대중적인 키보드 동작: 레이아웃 리사이즈
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // 터치 슬롭/탭 타임아웃 초기화
        ViewConfiguration vc = ViewConfiguration.get(this);
        touchSlop = vc.getScaledTouchSlop();
        tapTimeout = ViewConfiguration.getTapTimeout();

        // 최초 실행/가이드 결과 리스너 등록
        getSupportFragmentManager().setFragmentResultListener(
                "startDateMs", this, (requestKey, bundle) -> {
                    long startDateMs = bundle.getLong("startDateMs", -1L);
                    if (startDateMs > 0) {
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit().putBoolean(PREF_FIRST_RUN, false).apply();
                        applyStartDate(startDateMs);
                    }
                });

        // 최초 실행 시(또는 아직 입영일 저장 안된 경우) 가이드 표시
        SharedPreferences fr = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean isFirstRun = fr.getBoolean(PREF_FIRST_RUN, true);
        boolean hasEnlistSaved = getSharedPreferences("myPrefs", MODE_PRIVATE).contains("enlistMillis");
        boolean shouldShowGuide = isFirstRun && enlistDate == null && dischargeDate == null && !hasEnlistSaved;
        if (shouldShowGuide) {
            showGuideFlow();
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        insets.setAppearanceLightStatusBars(nightModeFlags == Configuration.UI_MODE_NIGHT_NO);

        // ✅ 1. View 바인딩 먼저
        textViewEnlistDate = findViewById(R.id.textViewDischargeDate2);
        textViewDischargeDate = findViewById(R.id.textViewDischargeDate);
        textViewDday = findViewById(R.id.textViewDday);
        textViewProgressPercent = findViewById(R.id.textViewProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(10000);

        textRemaining1 = findViewById(R.id.remaining1);
        textRemaining2 = findViewById(R.id.remaining2);
        textRemaining3 = findViewById(R.id.remaining3);

//        EditText editDays1 = findViewById(R.id.editDays1);
//        EditText editHours1 = findViewById(R.id.editHours1);
//        EditText editMinutes1 = findViewById(R.id.editMinutes1);
//        Button buttonMinus1 = findViewById(R.id.buttonMinus1);
        editDays1 = findViewById(R.id.editDays1);
        editHours1 = findViewById(R.id.editHours1);
        editMinutes1 = findViewById(R.id.editMinutes1);
        buttonMinus1 = findViewById(R.id.buttonMinus1);
        card1 = findViewById(R.id.card1);
        // 1년차: IME 동작 설정 (NEXT, DONE)
        editDays1.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editHours1.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editMinutes1.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editMinutes1.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                buttonMinus1.performClick();
                return true;
            }
            return false;
        });

        EditText editDays2 = findViewById(R.id.editDays2);
        EditText editHours2 = findViewById(R.id.editHours2);
        EditText editMinutes2 = findViewById(R.id.editMinutes2);
        Button buttonMinus2 = findViewById(R.id.buttonMinus2);
        // 2년차: IME 동작 설정 (NEXT, DONE)
        editDays2.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editHours2.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editMinutes2.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editMinutes2.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                buttonMinus2.performClick();
                return true;
            }
            return false;
        });

        EditText editDays3 = findViewById(R.id.editDays3);
        EditText editHours3 = findViewById(R.id.editHours3);
        EditText editMinutes3 = findViewById(R.id.editMinutes3);
        Button buttonMinus3 = findViewById(R.id.buttonMinus3);
        // 병가: IME 동작 설정 (NEXT, DONE)
        editDays3.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editHours3.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editMinutes3.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editMinutes3.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                buttonMinus3.performClick();
                return true;
            }
            return false;
        });

        ProgressBar progress1 = findViewById(R.id.progress1);
        ProgressBar progress2 = findViewById(R.id.progress2);
        ProgressBar progress3 = findViewById(R.id.progress3);

        Button buttonReset = findViewById(R.id.button2);

        attachClearErrorOnChange(editDays1, editHours1, editMinutes1,
                                 editDays2, editHours2, editMinutes2,
                                 editDays3, editHours3, editMinutes3);

        // 휴가 Progress 최대값
        progress1.setMax(15 * 480);
        progress2.setMax(13 * 480);
        progress3.setMax(30 * 480);

        // 프레임 동기화 콜백 (디스플레이 vsync 기준)
        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                updateProgress();
                Choreographer.getInstance().postFrameCallback(this);
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
        updateFirstYearLockUI();

        // ✅ 6. 날짜 복원 시 진행률 시작
        if (enlistDate != null && dischargeDate != null) {
            updateProgress();
            updateFirstYearLockUI();
            Choreographer.getInstance().postFrameCallback(frameCallback);
            // 🔹 전역일 텍스트 표시 추가
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
            textViewEnlistDate.setText("입영일 : " + sdf.format(enlistDate.getTime()));
            textViewDischargeDate.setText("전역일 : " + sdf.format(dischargeDate.getTime()));
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
            int[] n1 = normalizeInput(editDays1, editHours1, editMinutes1);
            leave1Year.subtract(n1[0], n1[1], n1[2]);
            updateLeaveDisplay();
            progress1.setProgress(leave1Year.toMinutes());

            editDays1.setText("");
            editHours1.setText("");
            editMinutes1.setText("");
            editDays1.setError(null);
            editHours1.setError(null);
            editMinutes1.setError(null);
            saveLeaveToPrefs();
            // 포커스 해제 및 키보드 숨김
            View cf1 = getCurrentFocus();
            if (cf1 != null) cf1.clearFocus();
            hideKeyboard(v);
        });

        buttonMinus2.setOnClickListener(v -> {
            int[] n2 = normalizeInput(editDays2, editHours2, editMinutes2);
            leave2Year.subtract(n2[0], n2[1], n2[2]);
            updateLeaveDisplay();
            progress2.setProgress(leave2Year.toMinutes());

            editDays2.setText("");
            editHours2.setText("");
            editMinutes2.setText("");
            editDays2.setError(null);
            editHours2.setError(null);
            editMinutes2.setError(null);
            saveLeaveToPrefs();
            // 포커스 해제 및 키보드 숨김
            View cf2 = getCurrentFocus();
            if (cf2 != null) cf2.clearFocus();
            hideKeyboard(v);
        });

        buttonMinus3.setOnClickListener(v -> {
            int[] n3 = normalizeInput(editDays3, editHours3, editMinutes3);
            sickLeave.subtract(n3[0], n3[1], n3[2]);
            updateLeaveDisplay();
            progress3.setProgress(sickLeave.toMinutes());

            editDays3.setText("");
            editHours3.setText("");
            editMinutes3.setText("");
            editDays3.setError(null);
            editHours3.setError(null);
            editMinutes3.setError(null);
            saveLeaveToPrefs();
            // 포커스 해제 및 키보드 숨김
            View cf3 = getCurrentFocus();
            if (cf3 != null) cf3.clearFocus();
            hideKeyboard(v);
        });

        buttonReset.setOnClickListener(v -> {
            String[] options = {"연가/병가 초기화", "전체 초기화"};
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("초기화 항목 선택")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: // 연가/병가 초기화
                                resetLeave();
                                break;
                            case 1: // 전체 초기화
                                resetDischarge();
                                resetLeave();
                                getSharedPreferences(PREFS, MODE_PRIVATE)
                                        .edit().putBoolean(PREF_FIRST_RUN, true).apply();
                                showGuideFlow();
                                break;
                        }
                    })
                    .show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (enlistDate != null && dischargeDate != null) {
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
        updateFirstYearLockUI();
    }

    private void resetDischarge() {
        enlistDate = null;
        dischargeDate = null;
        textViewEnlistDate.setText("입영일 : ");
        textViewDischargeDate.setText("전역일 : ");
        textViewDday.setText("D - ");
        textViewProgressPercent.setText("0%");
        progressBar.setProgress(0);
        updateFirstYearLockUI();
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        clearEnlistDischargeFromPrefs();
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
        saveLeaveToPrefs();
        updateFirstYearLockUI();
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

    private void attachClearErrorOnChange(EditText... edits) {
        for (EditText e : edits) {
            e.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { e.setError(null); }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    // 소프트 키보드 숨기기 헬퍼
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // 입력 정규화: 시간(0–7), 분(0–59)로 자동 보정. 음수는 0으로 처리.
    // 반환: 정규화된 {days, hours, minutes}
    private int[] normalizeInput(EditText etDays, EditText etHours, EditText etMinutes) {
        int d = parse(etDays);
        int h = parse(etHours);
        int m = parse(etMinutes);

        // 음수 방지
        if (d < 0) { d = 0; etDays.setError("0 이상이어야 합니다"); }
        else { etDays.setError(null); }
        if (h < 0) { h = 0; etHours.setError("0 이상이어야 합니다"); }
        if (m < 0) { m = 0; etMinutes.setError("0 이상이어야 합니다"); }

        // 분 → 시간 반영 (0–59로 정규화)
        if (m >= 60) {
            h += m / 60;
            m = m % 60;
//            etMinutes.setError("0–59로 자동 정규화됨");
        } else {
            etMinutes.setError(null);
        }

        // 시간 → 일 반영 (0–7로 정규화)
        if (h >= 8) {
            d += h / 8;
            h = h % 8;
//            etHours.setError("0–7로 자동 정규화됨");
        } else {
            etHours.setError(null);
        }

        return new int[]{d, h, m};
    }
    // 1주년 '당일'까지는 사용 가능, 다음날 00:00부터 잠금
    private boolean isFirstYearLocked() {
        if (enlistDate == null) return false; // 입영일 미설정이면 잠그지 않음
        Calendar cutoff = (Calendar) enlistDate.clone();
        cutoff.add(Calendar.YEAR, 1); // 1주년 00:00
        long lockStart = cutoff.getTimeInMillis() + 24L * 60 * 60 * 1000; // 다음날 00:00
        return System.currentTimeMillis() >= lockStart;
    }

    private void updateFirstYearLockUI() {
        boolean locked = isFirstYearLocked();

        if (card1 != null)          card1.setAlpha(locked ? 0.3f : 1.0f);

        if (buttonMinus1 != null) { buttonMinus1.setEnabled(!locked); }

        if (editDays1 != null)   { editDays1.setEnabled(!locked); }
        if (editHours1 != null)  { editHours1.setEnabled(!locked); }
        if (editMinutes1 != null){ editMinutes1.setEnabled(!locked); }

        if (textRemaining1 != null) {
            String base = textRemaining1.getText().toString();
            String tag = "\n\n<1년차 연가 사용 불가>";
            if (locked && !base.contains(tag)) {
                textRemaining1.setText(base + tag);
            } else if (!locked && base.contains(tag)) {
                textRemaining1.setText(base.replace(tag, ""));
            }
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
                    updateFirstYearLockUI();

                    Choreographer.getInstance().removeFrameCallback(frameCallback);
                    Choreographer.getInstance().postFrameCallback(frameCallback);

                    // Immediately persist the dates and clear first-run
                    saveEnlistDischargeToPrefs();
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit().putBoolean(PREF_FIRST_RUN, false).apply();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void updateProgress() {
        if (enlistDate == null || dischargeDate == null) {
            textViewDday.setText("D -");
            textViewProgressPercent.setText("0%");
            progressBar.setProgress(0);
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long enlistMillis = enlistDate.getTimeInMillis();
        long dischargeMillis = dischargeDate.getTimeInMillis();

        if (nowMillis < enlistMillis) {
            textViewDday.setText("입대 전");
            textViewProgressPercent.setText("0%");
            progressBar.setProgress(0);
        } else if (nowMillis >= dischargeMillis) {
            textViewDday.setText("전역");
            textViewProgressPercent.setText("100%");
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
        updateFirstYearLockUI();
    }

    // ── 헬퍼: 입영/전역일 저장 ─────────────────────────────
    private void saveEnlistDischargeToPrefs() {
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (enlistDate != null) {
            editor.putLong("enlistMillis", enlistDate.getTimeInMillis());
        } else {
            editor.remove("enlistMillis");
        }
        if (dischargeDate != null) {
            editor.putLong("dischargeMillis", dischargeDate.getTimeInMillis());
        } else {
            editor.remove("dischargeMillis");
        }
        editor.apply();
    }

    // ── 헬퍼: 입영/전역일 키 제거 ───────────────────────────
    private void clearEnlistDischargeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("enlistMillis");
        editor.remove("dischargeMillis");
        editor.apply();
    }

    // ── 헬퍼: 휴가(연가/병가) 저장 ───────────────────────────
    private void saveLeaveToPrefs() {
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

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
    protected void onPause() {
        super.onPause();

        saveEnlistDischargeToPrefs();
        saveLeaveToPrefs();

        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }

    private void showGuideFlow() {
        // 중복 띄우기 방지: 이미 표시 중이면 return
        if (isGuideShowing) return;
        if (getSupportFragmentManager().findFragmentByTag("guide") != null) return;

        isGuideShowing = true;
        try {
            new GuideBottomSheetDialogFragment().show(getSupportFragmentManager(), "guide");
        } catch (Throwable t) {
            // 표시 실패 시 플래그 해제 후 폴백
            isGuideShowing = false;
            showDatePicker();
            return;
        }

        // 가이드가 닫힐 때 플래그 해제
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
                    if (f instanceof GuideBottomSheetDialogFragment) {
                        isGuideShowing = false;
                        fm.unregisterFragmentLifecycleCallbacks(this);
                    }
                }
            }, false
        );
    }

    private void applyStartDate(long startDateMs) {
        Calendar picked = Calendar.getInstance();
        picked.setTimeInMillis(startDateMs);
        picked.set(Calendar.HOUR_OF_DAY, 0);
        picked.set(Calendar.MINUTE, 0);
        picked.set(Calendar.SECOND, 0);
        picked.set(Calendar.MILLISECOND, 0);

        enlistDate = (Calendar) picked.clone();
        dischargeDate = (Calendar) picked.clone();
        dischargeDate.add(Calendar.MONTH, SERVICE_MONTHS);
        dischargeDate.add(Calendar.DAY_OF_MONTH, -1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        textViewEnlistDate.setText("입영일 : " + sdf.format(enlistDate.getTime()));
        textViewDischargeDate.setText("전역일 : " + sdf.format(dischargeDate.getTime()));

        updateProgress();
        updateFirstYearLockUI();
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        Choreographer.getInstance().postFrameCallback(frameCallback);

        saveEnlistDischargeToPrefs();
    }
    // 좌표가 특정 View 내부인지 확인
    private static boolean isTouchInsideView(View view, int rawX, int rawY) {
        if (view == null) return false;
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        int left = loc[0];
        int top = loc[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        return rawX >= left && rawX <= right && rawY >= top && rawY <= bottom;
    }

    // 현재 터치 지점이 어떤 EditText 위에 있는지 재귀로 검사
    private static boolean isTouchOnAnyEditText(View root, int rawX, int rawY) {
        if (root == null) return false;
        if (root instanceof EditText) {
            return isTouchInsideView(root, rawX, rawY);
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (isTouchOnAnyEditText(child, rawX, rawY)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // 탭 후보 시작
                touchDownX = ev.getRawX();
                touchDownY = ev.getRawY();
                isTapCandidate = true;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (isTapCandidate) {
                    float dx = Math.abs(ev.getRawX() - touchDownX);
                    float dy = Math.abs(ev.getRawY() - touchDownY);
                    if (dx > touchSlop || dy > touchSlop) {
                        // 스크롤/드래그로 판단 → 키보드 유지
                        isTapCandidate = false;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (isTapCandidate) {
                    // 진짜 '탭'으로 간주되는 경우에만 키보드 닫기 판단
                    View focused = getCurrentFocus();
                    if (focused instanceof EditText) {
                        View decor = getWindow().getDecorView();
                        int rx = (int) ev.getRawX();
                        int ry = (int) ev.getRawY();
                        boolean touchingAnyEdit = isTouchOnAnyEditText(decor, rx, ry);
                        if (!touchingAnyEdit) {
                            focused.clearFocus();
                            hideKeyboard(focused);
                        }
                    }
                }
                // 제스처 종료
                isTapCandidate = false;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                isTapCandidate = false;
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import io.noties.markwon.Markwon;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GuideBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String RESULT_KEY = "startDateMs";
    private Long selectedStartDateMs = null; // 날짜 선택 전까지 null

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false); // 뒤로가기/스와이프 닫기 방지
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false); // 바깥터치 닫기 방지
        dialog.setOnShowListener(dlg -> {
            BottomSheetDialog d = (BottomSheetDialog) dlg;
            View bottomSheet = (View) d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(false);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_guide_bottomsheet, container, false);
        TextView markdownView = v.findViewById(R.id.guideMarkdownView);

        try {
            // Markdown 파일 읽기
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(requireContext().getAssets().open("guide.md"))
            );
            StringBuilder mdBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                mdBuilder.append(line).append("\n");
            }
            reader.close();

            // Markwon으로 Markdown 렌더링
            Markwon markwon = Markwon.builder(requireContext())
                    .build();

            markwon.setMarkdown(markdownView, mdBuilder.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 단일 페이지 가이드: 탭/뷰페이저 없음
        Button btnPick = v.findViewById(R.id.btnPickStartDate);
        Button btnOk   = v.findViewById(R.id.btnConfirm);
        btnOk.setEnabled(false);

        btnPick.setOnClickListener(view -> {
            // MaterialDatePicker (UTC millis 반환)
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("복무 시작일 선택")
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                selectedStartDateMs = selection;
                btnOk.setEnabled(true);
                btnPick.setText("선택됨: " + picker.getHeaderText());
            });
            picker.show(getParentFragmentManager(), "datePicker");
        });

        btnOk.setOnClickListener(view -> {
            if (selectedStartDateMs != null) {
                Bundle result = new Bundle();
                result.putLong(RESULT_KEY, selectedStartDateMs);
                getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
                dismiss();
            }
        });

        return v;
    }
}
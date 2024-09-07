package com.example.photothis;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.NumberPicker;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class MonthlyStatActivity extends AppCompatActivity {

    private TextView descriptionTextView;
    private HorizontalBarChart barChart;
    private int selectedYear;
    private DatabaseReference databaseReference;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_stat);

        // Firebase Database reference 초기화
        databaseReference = FirebaseDatabase.getInstance().getReference("diary_entries");

        // 현재 시간 기준으로 연도 초기화
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);

        // TextView 초기화
        descriptionTextView = findViewById(R.id.descriptionTextView);
        descriptionTextView.setText(selectedYear + "년의 일기 작성 횟수"); // 초기 설명 텍스트 설정

        descriptionTextView.setOnClickListener(v -> showYearPickerDialog());

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed()); // 뒤로 가기 버튼 클릭 시 동작

        barChart = findViewById(R.id.barChart);
        configureChart(); // 차트 기본 설정

        fetchDataFromFirebase(); // 데이터 불러오기

    }

    private void showYearPickerDialog() {
        // NumberPicker를 담은 다이얼로그를 띄움
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_year_picker, null);
        NumberPicker numberPicker = view.findViewById(R.id.yearPicker);

        // NumberPicker 범위 설정 (2000년부터 2030년까지)
        numberPicker.setMinValue(2000);
        numberPicker.setMaxValue(2030);
        numberPicker.setValue(selectedYear); // 현재 선택된 연도로 초기화

        builder.setView(view);
        builder.setTitle("년도 선택");
        builder.setPositiveButton("확인", (dialog, which) -> {
            selectedYear = numberPicker.getValue();
            descriptionTextView.setText(selectedYear + "년의 일기 작성 횟수"); // 설명 텍스트 업데이트
            fetchDataFromFirebase(); // 선택한 연도에 맞게 데이터 다시 불러오기
        });
        builder.setNegativeButton("취소", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void fetchDataFromFirebase() {
        String startDate = selectedYear + "-01-01";
        String endDate = selectedYear + "-12-31";

        databaseReference.orderByChild("date").startAt(startDate).endAt(endDate).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<BarEntry> entries = new ArrayList<>();
                int[] monthCounts = new int[12]; // 월별 일기 횟수 초기화
                Set<String> uniqueDates = new HashSet<>(); // 중복된 날짜를 저장할 Set

                // 데이터 카운트를 초기화하여 매번 새롭게 계산
                for (int i = 0; i < monthCounts.length; i++) {
                    monthCounts[i] = 0;
                }

                // Firebase에서 데이터를 순차적으로 읽음
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String date = snapshot.child("date").getValue(String.class);
                    if (date != null) {
                        // 중복된 날짜가 아닌 경우에만 카운트
                        if (uniqueDates.add(date)) {
                            try {
                                // 월을 추출하여 1월부터 12월까지 카운트
                                int month = Integer.parseInt(date.split("-")[1]);
                                monthCounts[month - 1]++; // 데이터 카운팅 (1월이 index 0에 해당)
                            } catch (Exception e) {
                                Log.e("MonthlyStatActivity", "Date parsing error: " + e.getMessage());
                            }
                        }
                    } else {
                        Log.e("MonthlyStatActivity", "Date is null in snapshot: " + snapshot.getKey());
                    }
                }

                // 데이터를 추가하는 순서를 거꾸로 변경
                for (int i = 0; i < monthCounts.length; i++) {
                    Log.d("MonthlyStatActivity", (i + 1) + "월: " + monthCounts[i] + "회");
                    // X값을 거꾸로 순서로 설정하여 차트에 추가
                    entries.add(new BarEntry(i, monthCounts[11 - i])); // 거꾸로 순서를 맞추기 위해 11 - i 사용
                }

                BarDataSet barDataSet = new BarDataSet(entries, null); // 차트에 이름 표시 안 함
                barDataSet.setColor(Color.parseColor("#314383")); // 그래프 색상 설정
                barDataSet.setDrawValues(false);

                BarData barData = new BarData(barDataSet);
                barData.setBarWidth(0.4f); // 막대의 폭 조절

                barChart.setData(barData);
                barChart.invalidate(); // 차트를 다시 그려 데이터 갱신

                configureChart(); // 차트 다시 설정
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MonthlyStatActivity", "Database error: " + databaseError.getMessage());
            }
        });
    }


    private void configureChart() {
        // XAxis 설정 (월 레이블)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축을 아래쪽에 배치
        xAxis.setGranularity(1f); // 1 단위로 눈금 표시
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getMonthLabels())); // 레이블을 "12월", "11월" 형태로 설정
        xAxis.setLabelCount(12); // 레이블 개수 설정
        xAxis.setLabelRotationAngle(0f); // 레이블 회전 각도 조정 (0도)
        xAxis.setAxisMinimum(-0.5f); // X축 최소값 설정
        xAxis.setAxisMaximum(11.5f); // X축 최대값 설정
        xAxis.setDrawGridLines(false); // 그리드 라인 비활성화
        xAxis.setDrawAxisLine(true); // 축 선 표시
        xAxis.setTextSize(12f); // 레이블 텍스트 크기 조정
        xAxis.setXOffset(5f); // 좌우 여백 제거
        xAxis.setYOffset(5f); // 하단 여백 설정
        xAxis.setAvoidFirstLastClipping(false); // 첫 번째 및 마지막 레이블이 잘리지 않도록 설정 해제

        // Y축 왼쪽 설정
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false); // 그리드 라인 비활성화
        leftAxis.setDrawAxisLine(true); // 축 선 표시
        leftAxis.setDrawLabels(true); // 레이블 표시
        leftAxis.setAxisMaximum(31f); // 최대값 설정
        leftAxis.setAxisMinimum(0f); // 최소값 설정
        leftAxis.setTextSize(10f); // 레이블 텍스트 크기 조정
        leftAxis.setLabelCount(4, true); // Y축 레이블 개수 조정
        leftAxis.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // Y축 레이블을 정수로 표시
            }
        });
        leftAxis.setGranularity(5f); // Y축 레이블 단위 설정

        // Y축 오른쪽 비활성화
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false); // 오른쪽 축 비활성화

        // 범례 비활성화
        Legend legend = barChart.getLegend();
        legend.setEnabled(false); // 범례를 비활성화하여 "Description Label" 제거
        // 'Description Label' 제거
        barChart.getDescription().setEnabled(false); // 설명 텍스트 비활성화

        // 차트 설정
        barChart.setDrawValueAboveBar(true);
        barChart.setFitBars(true);
        barChart.setNoDataText("No data available");
        barChart.setDrawBorders(false);
        barChart.setDoubleTapToZoomEnabled(false); // 더블클릭 확대 비활성화
        barChart.setPadding(10, 10, 10, 10);

        // 상하 여백 추가
        barChart.setExtraTopOffset(30f); // 상단 여백 설정
        barChart.setExtraBottomOffset(50f); // 하단 여백 설정

        // 클릭 리스너 설정
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Log.d("ChartValueSelected", "Entry selected: " + e.toString());
                if (e instanceof BarEntry) {
                    BarEntry barEntry = (BarEntry) e;
                    int monthIndex = (int) barEntry.getX();
                    int count = (int) barEntry.getY();
                    String[] monthLabels = getMonthLabels();
                    String monthLabel = monthLabels[monthIndex]; // 데이터가 거꾸로 표시되므로 인덱스 그대로 사용
                    Log.d("ChartValueSelected", "Month: " + monthLabel + ", Count: " + count);
                    Toast.makeText(MonthlyStatActivity.this, monthLabel + " : " + count + "회", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {
                Log.d("ChartValueSelected", "Nothing selected");
            }
        });
    }


    private String[] getMonthLabels() {
        return new String[]{
                "12월", "11월", "10월", "9월", "8월", "7월", "6월", "5월", "4월", "3월", "2월", "1월"
        };
    }
    }
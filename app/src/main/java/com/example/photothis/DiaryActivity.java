package com.example.photothis;

import android.os.Bundle;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DiaryActivity extends AppCompatActivity {

    private GridView calendarGridView;
    private CalendarAdapter adapter;
    private List<String> daysInMonth;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        calendarGridView = findViewById(R.id.calendarGridView);

        calendar = Calendar.getInstance(Locale.KOREA);
        daysInMonth = new ArrayList<>();
        updateCalendar();
    }

    private void updateCalendar() {
        daysInMonth.clear();
        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfMonth = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfMonth; i++) {
            daysInMonth.add("");
        }

        for (int i = 1; i <= maxDay; i++) {
            daysInMonth.add(String.valueOf(i));
        }

        // 현재 월과 연도를 포맷하여 어댑터에 전달
        String currentYearMonth = new SimpleDateFormat("yyyy-MM", Locale.KOREA).format(calendar.getTime());
        adapter = new CalendarAdapter(this, daysInMonth, currentYearMonth);
        calendarGridView.setAdapter(adapter);
    }
}

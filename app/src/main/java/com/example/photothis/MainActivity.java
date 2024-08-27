package com.example.photothis;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView monthYearTextView;
    private GridView calendarGridView;
    private CalendarAdapter adapter;
    private List<String> daysInMonth;
    private Calendar calendar;

    private DatabaseReference diaryRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        diaryRef = FirebaseDatabase.getInstance().getReference().child("diary_entries");

        monthYearTextView = findViewById(R.id.monthYearTextView);
        calendarGridView = findViewById(R.id.calendarGridView);
        calendar = Calendar.getInstance(Locale.KOREA);
        daysInMonth = new ArrayList<>();
        updateCalendar();

        findViewById(R.id.prevMonthBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.MONTH, -1);
                updateCalendar();
            }
        });

        findViewById(R.id.nextMonthBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.MONTH, 1);
                updateCalendar();
            }
        });

        findViewById(R.id.monthYearTextView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        findViewById(R.id.todayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar today = Calendar.getInstance(Locale.KOREA);
                calendar.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
                updateCalendar();
            }
        });

        calendarGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedDay = (String) parent.getItemAtPosition(position);
                if (!selectedDay.isEmpty()) {
                    Calendar selectedCalendar = (Calendar) calendar.clone();
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(selectedDay));
                    String selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(selectedCalendar.getTime());
                    checkDiaryExist(selectedDate);
                }
            }
        });

        // 메뉴 버튼 클릭 리스너 설정
        Button calendarMenuButton = findViewById(R.id.calendarMenuButton);
        Button todoListMenuButton = findViewById(R.id.todoListMenuButton);
        Button myPageMenuButton = findViewById(R.id.myPageMenuButton);

        calendarMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 현재 페이지라서 아무 작업도 하지 않음
            }
        });

        todoListMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TodoListActivity.class);
                startActivity(intent);
            }
        });

        myPageMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showDatePickerDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.number_picker_popup, null);

        final NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);
        final NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        final NumberPicker dayPicker = dialogView.findViewById(R.id.dayPicker);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        Calendar now = Calendar.getInstance(Locale.KOREA);
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;
        int currentDay = now.get(Calendar.DAY_OF_MONTH);

        yearPicker.setMinValue(currentYear - 100);
        yearPicker.setMaxValue(currentYear + 100);
        yearPicker.setValue(currentYear);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(currentMonth);

        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(now.getActualMaximum(Calendar.DAY_OF_MONTH));
        dayPicker.setValue(currentDay);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        final AlertDialog datePickerDialog = builder.create();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.dismiss();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = yearPicker.getValue();
                int month = monthPicker.getValue();
                int day = dayPicker.getValue();
                Calendar selectedCalendar = Calendar.getInstance(Locale.KOREA);
                selectedCalendar.set(year, month - 1, day);
                calendar.set(year, month - 1, day);
                updateCalendar();
                datePickerDialog.dismiss();
            }
        });

        datePickerDialog.show();
    }

    private void checkDiaryExist(final String selectedDate) {
        diaryRef.orderByChild("date").equalTo(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Intent intent = new Intent(MainActivity.this, ViewDiaryActivity.class);
                    intent.putExtra("selected_date", selectedDate);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, WriteDiaryActivity.class);
                    intent.putExtra("selected_date", selectedDate);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "다이어리가 존재하지 않습니다 m00 : " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

        fetchDiaryImages();

        String currentYearMonth = new SimpleDateFormat("yyyy-MM", Locale.KOREA).format(calendar.getTime());
        adapter = new CalendarAdapter(this, daysInMonth, currentYearMonth);
        calendarGridView.setAdapter(adapter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월", Locale.KOREA);
        monthYearTextView.setText(sdf.format(calendar.getTime()));

        // 오늘 날짜 버튼에 날짜 설정
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.KOREA);
        TextView todayButton = findViewById(R.id.todayButton);
        todayButton.setText(dayFormat.format(Calendar.getInstance(Locale.KOREA).getTime()));
    }

    private void fetchDiaryImages() {
        diaryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                HashMap<String, String> diaryImages = new HashMap<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DiaryEntry diaryEntry = snapshot.getValue(DiaryEntry.class);
                    if (diaryEntry != null) {
                        diaryImages.put(diaryEntry.getDate(), diaryEntry.getImageUrl());
                    }
                }
                adapter.setDiaryImages(diaryImages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "다이어리 이미지를 가져오는데 실패했습니다 m01 : " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}


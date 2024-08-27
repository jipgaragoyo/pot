package com.example.photothis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

// 날짜칸에 이미지 띄우는 거
public class CalendarFragment extends Fragment {

    private GridView gridView;
    private CalendarAdapter calendarAdapter;
    private DatabaseReference diaryRef;
    private HashMap<String, String> diaryImages;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        gridView = view.findViewById(R.id.gridView);
        diaryRef = FirebaseDatabase.getInstance().getReference().child("diary_entries");
        diaryImages = new HashMap<>();
        calendar = Calendar.getInstance(Locale.KOREA);

        // Initialize CalendarAdapter
        String currentYearMonth = getCurrentYearMonth();
        calendarAdapter = new CalendarAdapter(getContext(), getDaysInMonth(), currentYearMonth);
        gridView.setAdapter(calendarAdapter);

        loadDiaryImages();

        return view;
    }

    private List<String> getDaysInMonth() {
        List<String> daysInMonth = new ArrayList<>();
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

        return daysInMonth;
    }

    private void loadDiaryImages() {
        diaryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 일기 항목 반복 처리
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DiaryEntry entry = snapshot.getValue(DiaryEntry.class);
                    if (entry != null) {
                        String date = entry.getDate(); // 날짜를 yyyy-mm-dd 형식으로 가정

                        // 날짜가 현재 월과 연도에 속하는지 확인
                        if (date.startsWith(getCurrentYearMonth())) {
                            String imageUrl = entry.getImageUrl();
                            diaryImages.put(date, imageUrl);
                        }
                    }
                }
                calendarAdapter.setDiaryImages(diaryImages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "이미지를 불러오는데 실패했습니다. F00: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getCurrentYearMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.KOREA);
        return sdf.format(calendar.getTime());
    }
}

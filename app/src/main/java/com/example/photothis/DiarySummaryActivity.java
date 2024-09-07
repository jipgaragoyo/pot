package com.example.photothis;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiarySummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_summary);

        // 제목 설정
        TextView titleTextView = findViewById(R.id.titleTextView);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1); // 한 달 전
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월의", Locale.getDefault());
        String lastMonth = sdf.format(calendar.getTime());
        titleTextView.setText(lastMonth + " 일기 요약");

        // 제목 색상 검은색으로 변경
        titleTextView.setTextColor(getResources().getColor(android.R.color.black));

        // 데이터 로드 및 요약 요청 호출 예시
        loadLastMonthDiaryEntries();
    }


    private void loadLastMonthDiaryEntries() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String lastMonth = sdf.format(calendar.getTime());

        DatabaseReference diaryRef = FirebaseDatabase.getInstance().getReference().child("diary_entries");
        diaryRef.orderByChild("date").startAt(lastMonth + "-01").endAt(lastMonth + "-31")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        StringBuilder diaryText = new StringBuilder();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DiaryEntry entry = snapshot.getValue(DiaryEntry.class);
                            if (entry != null) {
                                diaryText.append(entry.getText()).append(" ");
                            }
                        }
                        // 요약 요청
                        summarizeDiaryEntries(diaryText.toString().trim());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(DiarySummaryActivity.this, "데이터를 불러오는 중 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void summarizeDiaryEntries(String diaryText) {
        String apiKey = "abc"; // 발급받은 OpenAI API 키

        OpenAIService openAIService = RetrofitClient.getRetrofitInstance(apiKey)
                .create(OpenAIService.class);

        String prompt = "한달치 일기를 요약해. 사건과 날짜를 나열하지 말고 가장 중요해보이는 사건을 찾아서 공간과 감정 위주로 200자 이내의 글로 요약해봐:\n" + diaryText;
        CompletionRequest request = new CompletionRequest(prompt, 200, 0.7);

        Call<CompletionResponse> call = openAIService.summarizeText(request);
        call.enqueue(new Callback<CompletionResponse>() {
            @Override
            public void onResponse(Call<CompletionResponse> call, Response<CompletionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String summary = response.body().getChoices().get(0).getText();
                    TextView summaryBox = findViewById(R.id.summaryBox);
                    summaryBox.setText(summary);
                } else {
                    Toast.makeText(DiarySummaryActivity.this, "요약을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CompletionResponse> call, Throwable t) {
                Toast.makeText(DiarySummaryActivity.this, "API 호출 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

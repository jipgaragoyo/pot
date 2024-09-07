package com.example.photothis;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import android.text.method.ScrollingMovementMethod;

public class ViewDiaryActivity extends AppCompatActivity {

    private TextView diaryTextView;
    private ImageView diaryImageView;
    private LinearLayout menuLayout;
    private ImageButton menuButton;
    private ImageButton backButton; // Add this member variable for back button
    private Button editButton;
    private Button deleteButton;

    private DatabaseReference diaryRef;
    private String selectedDate;
    private DiaryEntry currentEntry; // Add this member variable to store the DiaryEntry

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);

        diaryTextView = findViewById(R.id.diaryTextView);
        diaryImageView = findViewById(R.id.diaryImageView);
        menuLayout = findViewById(R.id.menuLayout);
        menuButton = findViewById(R.id.menuButton);
        backButton = findViewById(R.id.backButton); // Initialize back button
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);

        // Set movement method for scrolling
        diaryTextView.setMovementMethod(new ScrollingMovementMethod());

        diaryRef = FirebaseDatabase.getInstance().getReference().child("diary_entries");

        selectedDate = getIntent().getStringExtra("selected_date");

        if (selectedDate == null || selectedDate.isEmpty()) {
            Toast.makeText(ViewDiaryActivity.this, "날짜가 선택되지 않았습니다. V00", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDiaryEntry();

        // Menu button click event
        menuButton.setOnClickListener(v -> {
            if (menuLayout.getVisibility() == View.GONE) {
                menuLayout.setVisibility(View.VISIBLE);
            } else {
                menuLayout.setVisibility(View.GONE);
            }
        });

        // Back button click event
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDiaryActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close current activity
        });

        // Edit button click event
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDiaryActivity.this, EditDiaryActivity.class);
            intent.putExtra("selected_date", selectedDate);
            startActivity(intent);
        });

        // Delete button click event
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        // Image click event
        diaryImageView.setOnClickListener(v -> showImageDialog());
    }

    private void loadDiaryEntry() {
        diaryRef.orderByChild("date").equalTo(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        currentEntry = snapshot.getValue(DiaryEntry.class);
                        if (currentEntry != null) {
                            diaryTextView.setText(currentEntry.getText());

                            String imageUrl = currentEntry.getImageUrl();
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Picasso.get()
                                        .load(imageUrl)
                                        .fit()
                                        .centerCrop()
                                        .into(diaryImageView, new com.squareup.picasso.Callback() {
                                            @Override
                                            public void onSuccess() {
                                                diaryImageView.setVisibility(ImageView.VISIBLE);
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                Toast.makeText(ViewDiaryActivity.this, "이미지 로드 실패 V01: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                diaryImageView.setVisibility(ImageView.GONE);
                                            }
                                        });
                            } else {
                                diaryImageView.setVisibility(ImageView.GONE);
                            }
                        }
                    }
                } else {
                    Toast.makeText(ViewDiaryActivity.this, "선택한 날짜에 데이터가 없습니다. V02", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewDiaryActivity.this, "다이어리를 불러오는데 실패했습니다. V03: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("일기를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteDiaryEntry())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteDiaryEntry() {
        diaryRef.orderByChild("date").equalTo(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ViewDiaryActivity.this, "다이어리가 삭제되었습니다. V04", Toast.LENGTH_SHORT).show();
                            updateChartData(); // 일기 삭제 후 차트를 업데이트
                            finish(); // Close the activity after deletion
                        } else {
                            Toast.makeText(ViewDiaryActivity.this, "다이어리 삭제에 실패했습니다. V05", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewDiaryActivity.this, "다이어리 삭제에 실패했습니다. V06: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 차트 데이터를 다시 로드하는 메서드
    private void updateChartData() {
        // Firebase에서 데이터를 다시 읽어와 차트에 반영하는 로직 추가
        // 예시: Firebase 데이터 다시 로드
        FirebaseDatabase.getInstance().getReference().child("diary_entries").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 새로 로드된 데이터를 기반으로 차트 업데이트
                updateChartWithNewData(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewDiaryActivity.this, "차트 업데이트 실패: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Firebase 데이터를 사용하여 차트를 업데이트하는 메서드
    private void updateChartWithNewData(DataSnapshot dataSnapshot) {
        // 차트 데이터 업데이트 로직 구현
        // 데이터 파싱 및 차트 반영
    }


    private void showImageDialog() {
        if (currentEntry == null || currentEntry.getImageUrl() == null || currentEntry.getImageUrl().isEmpty()) {
            Toast.makeText(ViewDiaryActivity.this, "이미지가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog imageDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        imageDialog.setContentView(R.layout.dialog_image_viewer);
        ImageView dialogImageView = imageDialog.findViewById(R.id.dialogImageView);

        Picasso.get()
                .load(currentEntry.getImageUrl())
                .fit()
                .centerInside()
                .into(dialogImageView);

        // Dismiss dialog when touching outside of it
        imageDialog.findViewById(R.id.dialogImageContainer).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                imageDialog.dismiss();
            }
            return true;
        });

        imageDialog.show();
    }
}

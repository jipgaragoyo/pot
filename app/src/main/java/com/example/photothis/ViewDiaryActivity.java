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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


// 일기 보는 페이지 ( 수정, 삭제 버튼 있음 )
public class ViewDiaryActivity extends AppCompatActivity {

    private TextView diaryTextView;
    private ImageView diaryImageView;
    private LinearLayout menuLayout;
    private ImageButton menuButton; // 메뉴 버튼 - 오른쪽 햄버거 아이콘
    private ImageButton backButton; // 이전 화면(메인화면) 버튼 - 왼쪽 상단
    private Button editButton; // 수정
    private Button deleteButton; // 삭제

    private DatabaseReference diaryRef;
    private String selectedDate;
    private DiaryEntry currentEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);

        diaryTextView = findViewById(R.id.diaryTextView);
        diaryImageView = findViewById(R.id.diaryImageView);
        menuLayout = findViewById(R.id.menuLayout);
        menuButton = findViewById(R.id.menuButton);
        backButton = findViewById(R.id.backButton);
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

//        메뉴 버튼 클릭 시 메뉴 레이아웃(수정, 삭제) 나타남
        menuButton.setOnClickListener(v -> {
            if (menuLayout.getVisibility() == View.GONE) {
                menuLayout.setVisibility(View.VISIBLE);
            } else {
                menuLayout.setVisibility(View.GONE);
            }
        });

//       이전 화면 버튼 클릭 시 메인 화면으로 돌아감
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDiaryActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close current activity
        });

//        수정 버튼 클릭 시 일기 수정 페이지로, 삭제 버튼은 팝업 띄운 후 삭제(L.143)
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDiaryActivity.this, EditDiaryActivity.class);
            intent.putExtra("selected_date", selectedDate);
            startActivity(intent);
        });

        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        diaryImageView.setOnClickListener(v -> showImageDialog());
    }

//    파이어베이스에서 선택된 날짜의 일기 데이터 검색 후 표시
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

//    일기 삭제 팝업
    private void showDeleteConfirmationDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("일기를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteDiaryEntry())
                .setNegativeButton("취소", null)
                .show();
    }

//    일기 데이터, 사진 삭제
    private void deleteDiaryEntry() {
        diaryRef.orderByChild("date").equalTo(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DiaryEntry entry = snapshot.getValue(DiaryEntry.class);

                    if (entry != null) {
                        // Firebase Storage에서 이미지 삭제
                        String imageUrl = entry.getImageUrl();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Firebase Storage 참조 생성
                            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

                            // 이미지 삭제
                            storageRef.delete().addOnSuccessListener(aVoid -> {
                                // 이미지 삭제 성공 후 데이터베이스에서 항목 삭제
                                snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ViewDiaryActivity.this, "다이어리가 삭제되었습니다. V04", Toast.LENGTH_SHORT).show();
                                        finish(); // Close the activity after deletion
                                    } else {
                                        Toast.makeText(ViewDiaryActivity.this, "다이어리 삭제에 실패했습니다. V05", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }).addOnFailureListener(exception -> {
                                Toast.makeText(ViewDiaryActivity.this, "이미지 삭제에 실패했습니다. V07: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            // 이미지 URL이 없을 경우 데이터베이스에서 항목만 삭제
                            snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ViewDiaryActivity.this, "다이어리가 삭제되었습니다. V04", Toast.LENGTH_SHORT).show();
                                    finish(); // Close the activity after deletion
                                } else {
                                    Toast.makeText(ViewDiaryActivity.this, "다이어리 삭제에 실패했습니다. V05", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewDiaryActivity.this, "다이어리 삭제에 실패했습니다. V06: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    //    이미지 클릭 시 원본 이미지(전체화면) 뜸, 다른 곳 클릭하면 꺼짐
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

        imageDialog.findViewById(R.id.dialogImageContainer).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                imageDialog.dismiss();
            }
            return true;
        });

        imageDialog.show();
    }
}

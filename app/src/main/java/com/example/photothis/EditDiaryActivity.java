package com.example.photothis;



import android.content.Intent;

import android.graphics.Bitmap;

import android.net.Uri;

import android.os.Bundle;

import android.provider.MediaStore;

import android.text.Editable;
import android.text.TextUtils;

import android.text.TextWatcher;
import android.widget.Button;

import android.widget.EditText;

import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;

import com.google.firebase.database.DatabaseError;

import com.google.firebase.database.DatabaseReference;

import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;

import com.google.firebase.storage.FirebaseStorage;

import com.google.firebase.storage.StorageReference;

import com.squareup.picasso.Picasso;

import java.io.IOException;

// 일기 수정 페이지 (일기 쓰기 페이지와 비슷 )
public class EditDiaryActivity extends AppCompatActivity {

    private EditText diaryEditText;
    private ImageView diaryImageView;
    private Button saveButton;
    private TextView charCountTextView; // 글자수 TextView 추가

    private DatabaseReference diaryRef;
    private StorageReference storageRef;

    private String selectedDate;
    private String existingImageUrl;
    private Uri imageUri;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_CHAR_COUNT = 500; // 최대 글자수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_diary);

        diaryEditText = findViewById(R.id.diaryEditText);
        diaryImageView = findViewById(R.id.diaryImageView);
        saveButton = findViewById(R.id.saveButton);
        charCountTextView = findViewById(R.id.charCountTextView); // 글자수 TextView 초기화

        diaryRef = FirebaseDatabase.getInstance().getReference().child("diary_entries");
        storageRef = FirebaseStorage.getInstance().getReference().child("diary_images");

        selectedDate = getIntent().getStringExtra("selected_date");
        existingImageUrl = getIntent().getStringExtra("image_url");

        if (selectedDate == null || selectedDate.isEmpty()) {
            Toast.makeText(EditDiaryActivity.this, "날짜가 선택되지 않았습니다. E00", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDiaryEntry();

        diaryImageView.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> saveDiaryEntry());

        // 글자수 변경 리스너 추가
        diaryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                charCountTextView.setText(length + " / " + MAX_CHAR_COUNT);

                if (length > MAX_CHAR_COUNT) {
                    charCountTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    charCountTextView.setTextColor(getResources().getColor(android.R.color.black));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });
    }

    private void loadDiaryEntry() {
        diaryRef.orderByChild("date").equalTo(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        DiaryEntry entry = snapshot.getValue(DiaryEntry.class);
                        if (entry != null) {
                            diaryEditText.setText(entry.getText());

                            String imageUrl = entry.getImageUrl();
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Picasso.get()
                                        .load(imageUrl)
                                        .fit()
                                        .centerCrop()
                                        .into(diaryImageView);
                                existingImageUrl = imageUrl;
                            }
                        }
                    }
                } else {
                    Toast.makeText(EditDiaryActivity.this, "선택한 날짜에 대한 다이어리 데이터를 찾을 수 없습니다. E01", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditDiaryActivity.this, "다이어리를 불러오는데 실패했습니다. E02 : " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                diaryImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveDiaryEntry() {
        String newText = diaryEditText.getText().toString().trim();

        if (TextUtils.isEmpty(newText)) {
            Toast.makeText(EditDiaryActivity.this, "텍스트를 입력해주세요. E03", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            // Upload new image
            StorageReference fileReference = storageRef.child(selectedDate + ".jpg");

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String newImageUrl = uri.toString();
                        updateDiaryEntry(newText, newImageUrl);
                    }))
                    .addOnFailureListener(e -> Toast.makeText(EditDiaryActivity.this, "이미지 업로드에 실패했습니다. E04 : " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // No new image, use the existing image URL
            updateDiaryEntry(newText, existingImageUrl);
        }
    }

    private void updateDiaryEntry(String text, String imageUrl) {
        diaryRef.orderByChild("date").equalTo(selectedDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    DiaryEntry updatedEntry = new DiaryEntry(snapshot.getKey(), selectedDate, text, imageUrl);
                    snapshot.getRef().setValue(updatedEntry).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(EditDiaryActivity.this, "다이어리를 수정했습니다. E05", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(EditDiaryActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(EditDiaryActivity.this, "다이어리 수정에 실패했습니다. E06", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditDiaryActivity.this, "다이어리 수정에 실패했습니다. E07: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

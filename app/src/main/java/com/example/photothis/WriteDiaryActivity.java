package com.example.photothis;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

// 일기 쓰는 페이지
public class WriteDiaryActivity extends AppCompatActivity {

    private static final int MAX_CHAR_COUNT = 500; // 최대 글자수
    private ImageView diaryImageView;
    private EditText diaryEditText;
    private TextView charCountTextView;
    private Button saveDiaryBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference diaryRef;
    private StorageReference storageRef;

    private String selectedDate;
    private Uri imageUri;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_diary);

        mAuth = FirebaseAuth.getInstance();
        diaryRef = FirebaseDatabase.getInstance().getReference().child("diary_entries");
        storageRef = FirebaseStorage.getInstance().getReference().child("diary_images");

        diaryImageView = findViewById(R.id.diaryImageView);
        diaryEditText = findViewById(R.id.diaryEditText);
        charCountTextView = findViewById(R.id.charCountTextView);
        saveDiaryBtn = findViewById(R.id.saveDiaryBtn);

        selectedDate = getIntent().getStringExtra("selected_date");

        if (selectedDate == null) {
            Toast.makeText(this, "유효하지 않은 날짜입니다. W00", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        diaryImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        saveDiaryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    saveDiaryEntry();
                }
            }
        });

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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                try {
                    // 이미지뷰의 크기 가져오기
                    int imageViewWidth = diaryImageView.getWidth();
                    int imageViewHeight = diaryImageView.getHeight();

                    // 이미지 압축 및 크기 조정
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    Bitmap scaledBitmap = getScaledBitmap(bitmap, imageViewWidth, imageViewHeight);

                    diaryImageView.setImageBitmap(scaledBitmap);
                    findViewById(R.id.imagePlaceholder).setVisibility(View.GONE); // 숨기기
                } catch (Exception e) {
                    Toast.makeText(this, "이미지 로딩에 실패했습니다. W09: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap getScaledBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // 비율 계산
        float aspectRatio = (float) width / height;
        int scaledWidth, scaledHeight;

        if (width > height) {
            // 이미지뷰의 너비에 맞게 비율 조정
            scaledWidth = targetWidth;
            scaledHeight = (int) (targetWidth / aspectRatio);
        } else {
            // 이미지뷰의 높이에 맞게 비율 조정
            scaledHeight = targetHeight;
            scaledWidth = (int) (targetHeight * aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
    }

    private void saveDiaryEntry() {
        String diaryText = diaryEditText.getText().toString().trim();

        if (diaryText.length() > MAX_CHAR_COUNT) {
            Toast.makeText(this, "글자 수가 너무 많습니다. W07", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            try {
                // 이미지 압축
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // 80% 품질로 압축
                byte[] data = baos.toByteArray();

                // Firebase Storage에 압축된 이미지 업로드
                StorageReference fileReference = storageRef.child(selectedDate + ".jpg");

                fileReference.putBytes(data)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        saveDiaryEntryToDatabase(diaryText, uri.toString());
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(WriteDiaryActivity.this, "이미지 업로드에 실패했습니다. W01: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception e) {
                Toast.makeText(this, "이미지 압축에 실패했습니다. W08: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "이미지를 선택해주세요. W02", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDiaryEntryToDatabase(String text, String imageUrl) {
        String id = diaryRef.push().getKey(); // Generate a unique ID

        DiaryEntry entry = new DiaryEntry(id, selectedDate, text, imageUrl);

        diaryRef.child(id).setValue(entry)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(WriteDiaryActivity.this, "다이어리를 저장했습니다. W03", Toast.LENGTH_SHORT).show();
                        finish(); // 돌아가기
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(WriteDiaryActivity.this, "다이어리 저장에 실패했습니다. W04: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs() {
        String diaryText = diaryEditText.getText().toString().trim();

        if (diaryText.isEmpty()) {
            Toast.makeText(this, "일기 내용을 작성해주세요. W05", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (imageUri == null) {
            Toast.makeText(this, "이미지를 선택해주세요. W06", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}

package com.example.photothis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

// 날짜칸 코드
public class CalendarAdapter extends BaseAdapter {

    private Context context;
    private List<String> daysInMonth;
    private HashMap<String, String> diaryImages; // 날짜와 이미지 URL 매핑
    private String currentYearMonth; // 현재 월과 년도를 저장하는 변수

    public CalendarAdapter(Context context, List<String> daysInMonth, String currentYearMonth) {
        this.context = context;
        this.daysInMonth = daysInMonth;
        this.diaryImages = new HashMap<>();
        this.currentYearMonth = currentYearMonth;
    }

    @Override
    public int getCount() {
        return daysInMonth.size();
    }

    @Override
    public Object getItem(int position) {
        return daysInMonth.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        }

        TextView dayTextView = convertView.findViewById(R.id.dayTextView);
        ImageView imageView = convertView.findViewById(R.id.diaryImageView);

        String day = daysInMonth.get(position);

        if (!day.isEmpty()) {
            dayTextView.setText(day);

            // 날짜 포맷을 yyyy-MM-dd로 수정
            String date = currentYearMonth + "-" + (day.length() == 1 ? "0" + day : day);
            String imageUrl = diaryImages.get(date);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .fit()
                        .centerCrop()
                        .into(imageView);
                imageView.setVisibility(View.VISIBLE);
                // 이미지가 보일 때 텍스트를 숨깁니다.
                dayTextView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.GONE);
                // 이미지가 없을 때 텍스트를 보이게 합니다.
                dayTextView.setVisibility(View.VISIBLE);
            }
        } else {
            dayTextView.setText("");
            imageView.setVisibility(View.GONE);
            dayTextView.setVisibility(View.GONE);
        }

        return convertView;
    }



    public void setDiaryImages(HashMap<String, String> diaryImages) {
        this.diaryImages = diaryImages;
        notifyDataSetChanged();
    }
}

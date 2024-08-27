package com.example.photothis;

// 일기 데이터 기본 구조, 파이어베이스 데이터 전송 및 수신 코드
// ID는 랜덤생성, date는 yyyy-mm-dd 형식, text는 일기 내용, imageUrlsms 일기 이미지
// 파이어베이스에 이런 식으로 저장됨
public class DiaryEntry {
    private String id;
    private String date;
    private String text;
    private String imageUrl;

    // 기본 생성자
    public DiaryEntry() {
    }

    // 매개변수 있는 생성자
    public DiaryEntry(String id, String date, String text, String imageUrl) {
        this.id = id;
        this.date = date;
        this.text = text;
        this.imageUrl = imageUrl;
    }

    // getter 및 setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

package com.example.photothis;

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

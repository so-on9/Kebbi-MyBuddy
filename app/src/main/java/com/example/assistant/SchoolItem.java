package com.example.assistant;

public class SchoolItem {
    private String mSchoolName;
    private int mSchoolImages;

    public SchoolItem(String SchoolName, int SchoolImage) {
        mSchoolName = SchoolName;
        mSchoolImages = SchoolImage;
    }

    public String getSchoolName() {
        return mSchoolName;
    }

    public int getSchoolImage() {
        return mSchoolImages;
    }
}

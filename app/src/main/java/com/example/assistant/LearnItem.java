package com.example.assistant;

public class LearnItem {
    private String mLearnName;
    private int mLearnImages;

    public LearnItem(String LearnName, int LearnImage) {
        mLearnName = LearnName;
        mLearnImages = LearnImage;
    }

    public String getLearnName() {
        return mLearnName;
    }

    public int getLearnImage() {
        return mLearnImages;
    }
}

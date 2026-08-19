package com.example.assistant;

public class PlaceItem {
    private String mPlaceName;
    private int mPlaceImages;

    public PlaceItem(String PlaceName, int PlaceImage) {
        mPlaceName = PlaceName;
        mPlaceImages = PlaceImage;
    }

    public String getPlaceName() {
        return mPlaceName;
    }

    public int getPlaceImage() {
        return mPlaceImages;
    }
}

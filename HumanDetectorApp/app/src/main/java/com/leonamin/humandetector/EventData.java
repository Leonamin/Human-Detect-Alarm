package com.leonamin.humandetector;

public class EventData {
    private long timeStamp;
    private String imagePath;
    private String thumbnailPath;

    public EventData(long timeStamp, String imagePath, String thumbnailPath) {
        this.timeStamp = timeStamp;
        this.imagePath = imagePath;
        this.thumbnailPath = thumbnailPath;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
}

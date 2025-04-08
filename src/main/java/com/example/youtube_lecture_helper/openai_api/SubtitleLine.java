package org.example;

public class SubtitleLine {
    private final Integer start;
    private final String text;

    public SubtitleLine(Integer start, String text) {
//        int sec = (int) Double.parseDouble(start);
//        int hour = sec/3600;
//        int minute = (sec % 3600) / 60;
//        sec = sec % 60;

        //this.start = Integer.toString(hour) + ":"+ Integer.toString(minute) + ":" + Integer.toString(sec);
        this.start = start;
        this.text = text;
    }

    public Integer getStart() {
        return start;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + text + "]";
    }
}

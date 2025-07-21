package com.marianhello.bgloc.util;

public class TimeUtilsCompat {
    public static void formatDuration(long millis, StringBuilder s) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        s.append(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }
}

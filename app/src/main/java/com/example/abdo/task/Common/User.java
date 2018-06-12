package com.example.abdo.task.Common;

public class User {
    private static String lat;
    private static String log;

    public User() {
    }


    public static String getLat() {
        return lat;
    }

    public static void setLat(String lat) {
        User.lat = lat;
    }

    public static String getLog() {
        return log;
    }

    public static void setLog(String log) {
        User.log = log;
    }
}

package com.example.iotProject;

import java.util.Date;

public class TrainingSession {
    private static final long BASE = 31; // A prime number
    private static final long MOD = 1000000007; // A large prime number
    public static long hash(Date date) {
        long timestamp = date.getTime(); // Get the timestamp value of the date

        long hashValue = 0;
        while (timestamp != 0) {
            hashValue = (hashValue * BASE + (timestamp % 10)) % MOD;
            timestamp /= 10;
        }

        return hashValue;
    }

    public static Date reverseHash(long hashValue) {
        long timestamp = 0;
        long multiplier = 1;

        while (hashValue != 0) {
            timestamp = (timestamp + (hashValue % BASE) * multiplier) % MOD;
            hashValue /= BASE;
            multiplier *= 10;
        }

        return new Date(timestamp);
    }
    public TrainingSession(){}

    public TrainingSession(int totalPushUps, String trainingName, int totalSets, float explosiveness, float avgPushUpTime, float rangeOfMotion, Date date) {
        this.totalPushUps = totalPushUps;
        this.trainingName = trainingName;
        this.totalSets = totalSets;
        this.explosiveness = explosiveness;
        this.avgPushUpTime = avgPushUpTime;
        this.date = hash(date);
    }

    public Long getDate(){
        return this.date;
    }
    public void setDate(Long date) { this.date = date; }

    public int totalPushUps;
    public String trainingName;
    public int totalSets;
    public float explosiveness;
    public float avgPushUpTime;
    private Long date;
}

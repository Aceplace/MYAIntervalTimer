package com.coachyeiter.myaintervaltimer;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by AcePl on 1/24/2018.
 */

public class Schedule implements Serializable {

    private ArrayList<Period> periods;

    public Schedule(){
        periods = new ArrayList<>();
    }

    public ArrayList<Period> getPeriods() {
        return periods;
    }

    public void addPeriod(int periodLength){
        periods.add(new Period(periodLength));
    }

    public void addPeriod(int index, int periodLength){
        periods.add(index + 1, new Period(periodLength));
    }

    public void deletePeriod(int index){
        periods.remove(index);
    }

    public boolean movePeriodDown(int index){
        if (index < periods.size() - 1){
            Period temp = periods.get(index);
            periods.set(index, periods.get(index + 1));
            periods.set(index + 1, temp);
            return true;
        }
        return false;
    }

    public boolean movePeriodUp(int index){
        if (index > 0){
            Period temp = periods.get(index);
            periods.set(index, periods.get(index - 1));
            periods.set(index - 1, temp);
            return true;
        }
        return false;
    }

    public int getTotalHours(){
        return getTotalTime() / 3600;
    }

    public int getTotalMinutes(){
        return (getTotalTime() % 3600) / 60;
    }

    public int getTotalSeconds(){
        return getTotalTime() % 60;
    }

    public String getFormattedTime(){
        String hoursString = String.valueOf(getTotalHours());
        String minutesString = getTotalMinutes() < 10 ? "0" + String.valueOf(getTotalMinutes()) : String.valueOf(getTotalMinutes());
        String secondsString = getTotalSeconds() < 10 ? "0" + String.valueOf(getTotalSeconds()) : String.valueOf(getTotalSeconds());
        return String.format("%s:%s:%s", hoursString, minutesString, secondsString);
    }

    private int getTotalTime(){
        int totalTime = 0;
        for (Period period: periods){
            int periodTime = period.getPeriodLength();
            totalTime += periodTime;
        }
        return totalTime;
    }

    public static class Period implements Serializable{
        private int periodLength;

        public Period(){
            periodLength = 30;
        }

        public Period(int periodLength){
            this.periodLength = periodLength;
        }

        public int getPeriodLength() {
            return periodLength;
        }

        public void setPeriodLength(int periodLength) {
            this.periodLength = periodLength;
        }

        public String getPeriodLengthAsString(){
            int minutes = periodLength / 60;
            int seconds = periodLength % 60;
            String secondsString = seconds < 10 ? "0" + String.valueOf(seconds) : String.valueOf(seconds);
            return String.valueOf(minutes) + ":" + secondsString;
        }

        @Override
        public String toString() {
            return getPeriodLengthAsString();
        }
    }
}

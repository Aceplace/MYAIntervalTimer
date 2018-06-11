package com.coachyeiter.myaintervaltimer;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class TimerActivity extends AppCompatActivity {
    //information passed froms schedule
    private Schedule schedule;
    private ArrayList<Integer> paAnouncementTimes;//unit is seconds
    private boolean includePeriodZero;

    private int currentPeriod;
    private CountDownTimer periodTimer;//keep track to cancel previous timer
    boolean playingTimer;

    private SoundPool sounds;
    private int[] idPeriods = new int[41];
    private int[] idRemainingTimes = new int[60];
    private int idBeepThree, idBoxingBell, idFiveCountdown;
    AudioManager audioManager;

    SeekBar sbCountdown;
    Button btnPlayTimer, btnPreviousPeriod, btnNextPeriod;
    TextView tvPeriod, tvTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        //load in schedule and paAnnouncment flags from the schedule editor
        schedule = (Schedule) getIntent().getSerializableExtra("schedule");
        includePeriodZero = getIntent().getBooleanExtra("includePeriodZero", false);
        boolean[] paAnouncmentTimeFlags = getIntent().getBooleanArrayExtra("paAnnouncements");
        paAnouncementTimes = new ArrayList<Integer>();
        for (int i = 0; i < paAnouncmentTimeFlags.length; i++){
            if (paAnouncmentTimeFlags[i]){
                if (i == 0){
                    paAnouncementTimes.add(5); //announcement at zero is an exception
                    //instead the pa announcer will start counting down from 5 seconds
                }
                else {
                    paAnouncementTimes.add(i * 30);
                }
            }
        }

        currentPeriod = 0;

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setUpViews();
        loadSounds();

        updatePeriodTimeTextView(schedule.getPeriods().get(currentPeriod).getPeriodLength());
    }

    private void updatePeriodTimeTextView(int timeRemaining) {
        String period = String.valueOf(getModifiedCurrentPeriod());
        tvPeriod.setText(period);
        tvTime.setText(getTimeAsFormattedString(timeRemaining));
    }

    private static String getTimeAsFormattedString(int time){
        int minutes = time / 60;
        int seconds = time % 60;
        String secondsString = seconds < 10 ? "0" + String.valueOf(seconds) : String.valueOf(seconds);
        return String.valueOf(minutes) + ":" + secondsString;
    }

    private void nextPeriod(boolean startTimerImmediately, boolean announceStartofPeriod){
        if (currentPeriod < schedule.getPeriods().size() - 1){
            currentPeriod++;
            sbCountdown.setProgress(0);
            sbCountdown.setMax(schedule.getPeriods().get(currentPeriod).getPeriodLength());
            updatePeriodTimeTextView(sbCountdown.getMax());
            if (startTimerImmediately)
                startNewCountdownTimer(sbCountdown.getMax() - sbCountdown.getProgress());
            if (announceStartofPeriod)
                announceStartofPeriod();
        }
    }

    private void previousPeriod(boolean startTimerImmediately){
        if (currentPeriod > 0){
            currentPeriod--;
            sbCountdown.setProgress(0);
            sbCountdown.setMax(schedule.getPeriods().get(currentPeriod).getPeriodLength());
            updatePeriodTimeTextView(sbCountdown.getMax());
            if (startTimerImmediately)
                startNewCountdownTimer(sbCountdown.getMax() - sbCountdown.getProgress());
        }
    }

    private void startNewCountdownTimer(int time){
        if (periodTimer != null)
            periodTimer.cancel();
            periodTimer = new CountDownTimer(((time) * 1000) + 100, 1000) {
            @Override
            public void onTick(long l) {
                sbCountdown.setProgress(sbCountdown.getProgress() + 1);
                updatePeriodTimeTextView((int)(l / 1000));
                makePaAnnouncement((int)(l / 1000));
                fadeMusicStream((int)(l/1000));
            }

            @Override
            public void onFinish() {
                updatePeriodTimeTextView(0);
                nextPeriod(true, true);
            }
        };
        periodTimer.start();
    }

    private void makePaAnnouncement(final int timeRemaining){
        for (Integer paAnnounceTime: paAnouncementTimes){
            if (paAnnounceTime == timeRemaining && timeRemaining != sbCountdown.getMax() && timeRemaining != 5){
                sounds.play(idBeepThree, 1.0f, 1.0f, 1, 0, 1.0f);
                CountDownTimer soundDelayTimer = new CountDownTimer(2500, 2500) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        sounds.play(idRemainingTimes[(timeRemaining/30) - 1], 1.0f, 1.0f, 1, 0, 1.0f);
                    }
                }.start();
            }
            else if (paAnnounceTime == timeRemaining && timeRemaining == 5) {
                sounds.play(idFiveCountdown, 1.0f, 1.0f, 1, 0,  1.0f);
            }
        }
    }

    private void announceStartofPeriod(){
        sounds.play(idBoxingBell, 1.0f, 1.0f, 1, 0,  1.0f);
        CountDownTimer soundDelayTimer = new CountDownTimer(3500, 3500) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                sounds.play(idPeriods[getModifiedCurrentPeriod()], 1.0f, 1.0f, 1, 0, 1.0f);
                CountDownTimer soundDelayTimer = new CountDownTimer(1200, 1200) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        sounds.play(idRemainingTimes[(sbCountdown.getMax()/30) - 1], 1.0f, 1.0f, 1, 0, 1.0f);
                    }
                }.start();
            }
        }.start();
    }

    private void fadeMusicStream(final int timeRemaining){
        final int currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        for (Integer paAnnounceTime: paAnouncementTimes){
            if (paAnnounceTime == timeRemaining - 2 && timeRemaining - 2 != 5){

                fadeMusicOut(currentMusicVolume);

                new CountDownTimer(6000, 6000){//bring the music back after a set interval
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        fadeMusicIn(currentMusicVolume);
                    }
                }.start();

            }
            else if (paAnnounceTime == timeRemaining - 2 && timeRemaining == 7) {
                fadeMusicOut(currentMusicVolume);

                new CountDownTimer(13000, 13000){//bring the music back after a set interval
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        fadeMusicIn(currentMusicVolume);
                    }
                }.start();
            }
        }

    }

    private void fadeMusicOut(final int currentMusicVolume){
        final Handler hFadeOut = new Handler();
        hFadeOut.postDelayed(new Runnable() {
            private float timeRemainingInFade = 1500;

            @Override
            public void run() {
                timeRemainingInFade -= 50;
                if (timeRemainingInFade > 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(timeRemainingInFade / 1500f * currentMusicVolume), 0);
                    hFadeOut.postDelayed(this, 50);
                }
                else{
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }

            }
        }, 50);
    }

    private void fadeMusicIn(final int currentMusicVolume){
        audioManager.abandonAudioFocus(null);
        final Handler hFadeIn = new Handler();
        hFadeIn.postDelayed(new Runnable() {
            private float timeRemainingInFade = 2000;

            @Override
            public void run() {
                timeRemainingInFade -= 50;
                if (timeRemainingInFade > 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)((2000 - timeRemainingInFade) / 2000f * currentMusicVolume), 0);
                    hFadeIn.postDelayed(this, 50);
                }
                else{
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentMusicVolume, 0);
                }

            }
        }, 50);
    }

    private int getModifiedCurrentPeriod(){ //helper used to select correct announcement for when user "includes period zero"
        if (includePeriodZero){
            return currentPeriod;
        }
        else{
            return currentPeriod + 1;
        }
    }

    private void setUpViews(){
        sbCountdown = findViewById(R.id.sb_countdown);
        btnPlayTimer = findViewById(R.id.btn_play_timer);
        btnNextPeriod = findViewById(R.id.btn_next_period);
        btnPreviousPeriod = findViewById(R.id.btn_previous_period);
        tvPeriod = findViewById(R.id.tv_period);
        tvTime = findViewById(R.id.tv_time);

        sbCountdown.setMax(schedule.getPeriods().get(currentPeriod).getPeriodLength());
        sbCountdown.setProgress(0);

        sbCountdown.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updatePeriodTimeTextView(seekBar.getMax() - seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnPlayTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playingTimer){
                    playingTimer = false;
                    btnPlayTimer.setText("Play");
                    sbCountdown.setEnabled(true);
                    btnNextPeriod.setEnabled(true);
                    btnPreviousPeriod.setEnabled(true);
                    if (periodTimer != null){
                        periodTimer.cancel();
                    }
                }
                else {//not currently playing. start
                    playingTimer = true;
                    btnPlayTimer.setText("Stop");
                    sbCountdown.setEnabled(false);
                    btnNextPeriod.setEnabled(false);
                    btnPreviousPeriod.setEnabled(false);
                    startNewCountdownTimer(sbCountdown.getMax() - sbCountdown.getProgress());
                }
            }
        });

        btnNextPeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPeriod(false, false);
            }
        });

        btnPreviousPeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previousPeriod(false);
            }
        });
    }

    private void loadSounds(){
        idPeriods = new int[41];
        idRemainingTimes = new int[60];
        sounds = new SoundPool(1, AudioManager.STREAM_ALARM, 0);
        for (int i = 0; i < idPeriods.length; i++){
            String periodString = "period" + (i < 10 ? "0" : "") + String.valueOf(i);
            idPeriods[i] = sounds.load(this, getResources().getIdentifier(periodString,"raw",getPackageName()) ,1);
        }

        for (int i = 0; i < idRemainingTimes.length; i++){
            int minutes = ((i + 1) * 30) / 60;
            int seconds = ((i + 1) * 30) % 60;
            String timeRemainingString = "remaining" + (minutes < 10 ? "0" : "") + String.valueOf(minutes) +
                    ((seconds == 30) ? "30" : "00");
            idRemainingTimes[i] = sounds.load(this, getResources().getIdentifier(timeRemainingString,"raw",getPackageName()) ,1);
        }

        idBeepThree = sounds.load(this, R.raw.beepthree, 1);
        idBoxingBell = sounds.load(this, R.raw.boxingbell, 1);
        idFiveCountdown = sounds.load(this, R.raw.fivecountdown, 1);
    }

}

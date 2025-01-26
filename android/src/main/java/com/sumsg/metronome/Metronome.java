package com.sumsg.metronome;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import io.flutter.plugin.common.EventChannel;
import android.app.Activity;

public class Metronome {
    private final Object mLock = new Object();
    private int mBpm = 120;
    private static final int SAMPLE_RATE = 44100;
    private boolean play = false;
    private final AudioGenerator audioGenerator = new AudioGenerator(SAMPLE_RATE);
    private short[] mTookSilenceSoundArray;
    private short[] mTookAccentSilenceSoundArray;
    private AudioGenerator.Sample mTook;
    private AudioGenerator.Sample mAccentedTook;
    private int mBeatDivisionSampleCount;
    private float mVolume = (float) 0.0;
    private final Context context;
    private int timeSignature = 4;
    private int currentBeat=1;
    private Activity activity;
    private EventChannel.EventSink eventTickSink;

    public Metronome(Context ctx, String mainFile, String accentedFile) {
        context = ctx;
        setAudioFile(mainFile);
        setAccentedAudioFile(accentedFile );
    }

    public void setAudioFile(String path) {
        Log.e("Start loadSampleFromWav for: ", path);
        mTook = AudioGenerator.loadSampleFromWav(path);
        if (play) {
            stop();
            play(mBpm);
        }
    }

    public void setAccentedAudioFile(String path) {
        mAccentedTook = AudioGenerator.loadSampleFromWav(path);
        if (play) {
            stop();
            play(mBpm);
        }
    }

    public void setTimeSignature(int _timeSignature) {
        timeSignature= _timeSignature;
    }

    public void enableTickCallback(EventChannel.EventSink _eventTickSink,  Activity _activity) {
        eventTickSink = _eventTickSink;
        activity = _activity;
    }

    public void calcSilence() {
        // (beats per second * SAMPLE_RATE) - NumberOfSamples
        mBeatDivisionSampleCount = (int) (((60 / (float) mBpm) * SAMPLE_RATE));
        int silence = Math.max(mBeatDivisionSampleCount - mTook.getSampleCount(), 0);
        int silenceAccent = Math.max(mBeatDivisionSampleCount - mAccentedTook.getSampleCount(), 0);
        mTookSilenceSoundArray = new short[silence];
        mTookAccentSilenceSoundArray = new short[silenceAccent];

        for (int i = 0; i < silence; i++)
            mTookSilenceSoundArray[i] = 0;
        for (int i = 0; i < silenceAccent; i++)
            mTookAccentSilenceSoundArray[i] = 0;
    }

    private void isInitialized() {
        if (mTook == null) {
            throw new IllegalStateException("Not initialized correctly");
        }
    }

    public void play(int bpm) {
        isInitialized();
        setBPM(bpm);
        play = true;
        audioGenerator.createPlayer(context, mVolume);
        calcSilence();
        short[] accentSample = (short[]) mAccentedTook.getSample();
        short[] sample = (short[]) mTook.getSample();

        int accentLength = Math.min(accentSample.length, mBeatDivisionSampleCount);
        int sampleLength = Math.min(sample.length, mBeatDivisionSampleCount);

        new Thread(() -> {
            do {
                pulseEvent(currentBeat);          
                if(currentBeat==1 ){
                    audioGenerator.writeSound(accentSample, accentLength);
                    audioGenerator.writeSound(mTookAccentSilenceSoundArray);
 
                }else{
                    
                    audioGenerator.writeSound(sample, sampleLength);
                    audioGenerator.writeSound(mTookSilenceSoundArray);
                }

                if(currentBeat==timeSignature){
                    currentBeat = 1;//Reset current beat
                }else{
                    currentBeat++;
                }

                synchronized (mLock) {
                    if (!play)
                        return;
                }
            } while (true);
        }).start();     

    }

    public void pulseEvent(int currentBeat) {
        //long startTime = System.nanoTime();
        activity.runOnUiThread(() -> {
            if (eventTickSink != null) {
                eventTickSink.success(currentBeat);
            }
        });
        //long endTime = System.nanoTime();
        //long diff = (endTime - startTime)/1000;
        //Log.d("EventTick", "Czas wysyłania eventu: " + diff + " mikrosekund");
    }

    public void pause() {
        if (audioGenerator.getAudioTrack() != null) {
            play = false;
            audioGenerator.getAudioTrack().pause();
        }
    }

    public void stop() {
        if (audioGenerator.getAudioTrack() != null) {
            play = false;
            audioGenerator.destroyAudioTrack();
        }
    }

    public int getVolume() {
        return (int) (mVolume * 100);
    }

    public void setVolume(float val) {
        mVolume = val;
        if (audioGenerator.getAudioTrack() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    audioGenerator.getAudioTrack().setVolume(mVolume);
                } catch (Exception e) {
                    Log.e("setVolume", String.valueOf(e));
                }
            }
        }
    }

    public void setBPM(int bpm) {
        mBpm = bpm;
        calcSilence();
    }

    public double getBPM() {
        return mBpm;
    }

    public boolean isPlaying() {
        return play;
    }

    public void destroy() {

        if (!play)
            return;
        stop();
        synchronized (mLock) {
            mBeatDivisionSampleCount = 0;
        }
    }
}

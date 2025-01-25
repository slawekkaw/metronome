package com.sumsg.metronome;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import io.flutter.plugin.common.EventChannel;

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
    private BeatTimer beatTimer;
    private int timeSignature = 4;
    private int currentBeat=1;

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

    public void setTimeSignature(int timeSignatureLoc) {
        timeSignature= timeSignatureLoc;
        //TODO: restart after change time signature?
        // if (play) {
        //     if (beatTimer != null) {
        //         beatTimer.startBeatTimer(bpm);
        //     }
        // }
    }

    public void enableTickCallback(EventChannel.EventSink _eventTickSink) {
        beatTimer = new BeatTimer(_eventTickSink, timeSignature);
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
        new Thread(() -> {
            do {
               
                if(currentBeat==1 ){
                    short[] sample = (short[]) mAccentedTook.getSample();
                    audioGenerator.writeSound(sample, Math.min(sample.length, mBeatDivisionSampleCount));
                    audioGenerator.writeSound(mTookAccentSilenceSoundArray);
 
                }else{
                    short[] sample = (short[]) mTook.getSample();
                    audioGenerator.writeSound(sample, Math.min(sample.length, mBeatDivisionSampleCount));
                    audioGenerator.writeSound(mTookSilenceSoundArray);
                }

                if(currentBeat==timeSignature){
                    currentBeat = 1;//Reset current beat
                }else{
                    currentBeat++;
                }

                if (beatTimer != null) {
                    beatTimer.synchronizeTicks(currentBeat );
                }

                //if (beatTimer != null) {
                //    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> beatTimer.sendEvent(currentBeat));
                //}

                synchronized (mLock) {
                    if (!play)
                        return;
                }
            } while (true);
        }).start();
        
        if (beatTimer != null) {
            beatTimer.startBeatTimer(bpm);
        }
    }

    public void pause() {
        if (audioGenerator.getAudioTrack() != null) {
            play = false;
            audioGenerator.getAudioTrack().pause();
        }
        if (beatTimer != null) {
            beatTimer.stopBeatTimer();
        }
    }

    public void stop() {
        if (audioGenerator.getAudioTrack() != null) {
            play = false;
            audioGenerator.destroyAudioTrack();
        }
        if (beatTimer != null) {
            beatTimer.stopBeatTimer();
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
        if (play) {
            if (beatTimer != null) {
                beatTimer.startBeatTimer(bpm);
            }
        }
    }

    public double getBPM() {
        return mBpm;
    }

    public boolean isPlaying() {
        return play;
    }

    public void destroy() {
        if (beatTimer != null) {
            beatTimer.stopBeatTimer();
        }
        if (!play)
            return;
        stop();
        synchronized (mLock) {
            mBeatDivisionSampleCount = 0;
        }
    }
}

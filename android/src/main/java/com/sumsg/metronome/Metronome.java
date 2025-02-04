package com.sumsg.metronome;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.Arrays;

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
            play(mBpm, timeSignature);
        }
    }

    public void setAccentedAudioFile(String path) {
        mAccentedTook = AudioGenerator.loadSampleFromWav(path);
        if (play) {
            stop();
            play(mBpm, timeSignature);
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
        beatTimer = new BeatTimer(_eventTickSink);
    }

    public void calcSilence() {
        // (beats per second * SAMPLE_RATE) - NumberOfSamples
        mBeatDivisionSampleCount = (int) (((60 / (float) mBpm) * SAMPLE_RATE));
        int silence = Math.max(mBeatDivisionSampleCount - mTook.getSampleCount(), 0);
        int silenceAccent = Math.max(mBeatDivisionSampleCount - mAccentedTook.getSampleCount(), 0);

        System.out.println("Metronome calculated mBeatDivisionSampleCount:" + mBeatDivisionSampleCount + " silence: " + silence + " silenceAccent: " + 
            silenceAccent+" mTook.getSampleCount():"+mTook.getSampleCount()+" mAccentedTook.getSampleCount():"+mAccentedTook.getSampleCount());    

        mTookSilenceSoundArray = new short[silence];
        mTookAccentSilenceSoundArray = new short[silenceAccent];

        Arrays.fill(mTookSilenceSoundArray, (short) 0);
        Arrays.fill(mTookAccentSilenceSoundArray, (short) 0);
    
    }

    private void isInitialized() {
        if (mTook == null) {
            throw new IllegalStateException("Not initialized correctly");
        }
    }

    public void play(int bpm, int timeSignature) {
        isInitialized();
        setBPM(bpm);
        setTimeSignature(timeSignature);
        play = true;
        audioGenerator.createPlayer(context, mVolume);
        calcSilence();
        currentBeat = 1;
        short[] sampleAccented = (short[]) mAccentedTook.getSample();
        short[] sampleStandard = (short[]) mTook.getSample();

        new Thread(() -> {
            do {
                long currentTimeMillis = System.currentTimeMillis();
                System.out.println(">>>>>>Metronome Thread Loop start: send timestamp: " + currentTimeMillis);      
                if(currentBeat==1 ){
                    long startTimeTickNano = System.nanoTime();
                    if (beatTimer != null) {
                        beatTimer.startBeatTimer(bpm, timeSignature);// Start first tick
                        System.out.println("Metronome after startbeatTimer: " + System.currentTimeMillis());      
                    }           
                    synchronized (mLock) {
                        if(!play)
                            return;
                        
                        long startTimeNano = System.nanoTime();
                        audioGenerator.writeSound(sampleAccented, Math.min(sampleAccented.length, mBeatDivisionSampleCount));
                        long endTimeNano = System.nanoTime();
                        System.out.println("Metronome writeSound First Tick time: " + (endTimeNano - startTimeNano) / 1_000_000.0 + " ms"); 
                    }
                    synchronized (mLock) {
                        if(!play)
                            return;
                        long startTimeNano = System.nanoTime();
                        audioGenerator.writeSound(mTookAccentSilenceSoundArray);
                        long endTimeNano = System.nanoTime();
                        System.out.println("Metronome writeSound First SilentTick time: " + (endTimeNano - startTimeNano) / 1_000_000.0 + " ms"); 
                    }
                    long endTimeTickNano = System.nanoTime();
                    System.out.println("******** Metronome End First Tick time: " + (endTimeTickNano - startTimeTickNano) / 1_000_000.0 + " ms"); 
                    // if (beatTimer != null) {
                    //     beatTimer.startBeatTimer(bpm, timeSignature);// Start first tick
                    // }   

                    //System.out.println(">>>>>>>>>Metronome after first Ticker: " + System.currentTimeMillis());        
 
                }else{ 
                    synchronized (mLock) {
                        if(!play)
                            return;   
                        System.out.println("Metronome Before Beat n: " + System.currentTimeMillis());      
                        audioGenerator.writeSound(sampleStandard, Math.min(sampleStandard.length, mBeatDivisionSampleCount));
                        System.out.println("Metronome After Beat n: " + System.currentTimeMillis());      
                    }
                    synchronized (mLock) {
                        if(!play)
                            return;
                        System.out.println("Metronome Before Beat n Silnce: " + System.currentTimeMillis());      
                        audioGenerator.writeSound(mTookSilenceSoundArray);
                        System.out.println("Metronome After Beat n: Silence" + System.currentTimeMillis());      
                    }
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
            synchronized (mLock) {   
              audioGenerator.destroyAudioTrack();
            }
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
        // if (play) {
        //     if (beatTimer != null) {
        //         beatTimer.startBeatTimer(bpm,);
        //     }
        // }
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

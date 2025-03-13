package com.sumsg.metronome;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.Arrays;

import io.flutter.plugin.common.EventChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.media.AudioTrack;


public class Metronome {
    private final Object mLock = new Object();
    private int mBpm = 120;
    private static final int SAMPLE_RATE = 44100;
    private volatile boolean play = false;
    private final AudioGenerator audioGeneratorStandard = new AudioGenerator(SAMPLE_RATE);
    private final AudioGenerator audioGeneratorAccented = new AudioGenerator(SAMPLE_RATE);
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
    private long prevTickTimeNano = 0;
    private Thread metronomeThread = null;

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

    private void isInitialized() {
        if (mTook == null) {
            throw new IllegalStateException("Not initialized correctly");
        }
    }

    private void preloadSamples() {
        audioGeneratorAccented.createPlayerStatic(context, mVolume);
        audioGeneratorStandard.createPlayerStatic(context, mVolume);

        short[] sampleAccented = (short[]) mAccentedTook.getSample();
        short[] sampleStandard = (short[]) mTook.getSample();

        audioGeneratorAccented.writeSound(sampleAccented, sampleAccented.length );
        audioGeneratorStandard.writeSound(sampleStandard, sampleStandard.length);

    }

    public void play(int bpm, int timeSignature) {
        int tickTimeInmilis = (int) (60 / (float) bpm * 1000);
        mBeatDivisionSampleCount = (int) (((60 / (float) mBpm) * SAMPLE_RATE));
        isInitialized();
        setBPM(bpm);
        setTimeSignature(timeSignature);
        play = true;
                
        currentBeat = 1;
        //System.out.println(">>>>>>Metronome Start playing");      

        preloadSamples();
         ///
        prevTickTimeNano = System.nanoTime();
        new Thread(() -> {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {   

                synchronized (mLock) {
                    if (!play){
                        scheduler.shutdown();
                        return;
                    }
                }   
        
                if (beatTimer != null) {
                        beatTimer.sendEvent(currentBeat);
                }       

                 if((currentBeat>1)||(timeSignature==0) ){
                    audioGeneratorStandard.stop();
                    audioGeneratorStandard.play();
                 }else{
                    audioGeneratorAccented.stop();
                    audioGeneratorAccented.play();
                 }


                if(currentBeat>=timeSignature){
                    currentBeat = 1;//Reset current beat
                }else{
                    currentBeat++;
                }

                long endTickTimeNano = System.nanoTime();
                //System.out.println("******** Metronome End Tick time: " + (endTickTimeNano - prevTickTimeNano) / 1_000_000.0 + " ms"); 
                prevTickTimeNano = endTickTimeNano;

                synchronized (mLock) {
                    if (!play)
                        scheduler.shutdown();
                }
            }, 0, tickTimeInmilis, TimeUnit.MILLISECONDS);
            synchronized (mLock) {
                if (!play)
                    return;
            }
         }).start();   

           

    }

    public void pause() {
       stop();
    }

    public void stop() {
        play = false;
        //wait
    }

    public int getVolume() {
        return (int) (mVolume * 100);
    }

    public void setVolume(float val) {
        mVolume = val;
        if (audioGeneratorAccented.getAudioTrack() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    audioGeneratorAccented.getAudioTrack().setVolume(mVolume);
                } catch (Exception e) {
                    Log.e("setVolume", String.valueOf(e));
                }
            }
        }
        if (audioGeneratorStandard.getAudioTrack() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    audioGeneratorStandard.getAudioTrack().setVolume(mVolume);
                } catch (Exception e) {
                    Log.e("setVolume", String.valueOf(e));
                }
            }
        }
    }

    public void setBPM(int bpm) {
        mBpm = bpm;
       
    }

    public double getBPM() {
        return mBpm;
    }

    public boolean isPlaying() {
        return play;
    }

    public void destroy() {
        // if (beatTimer != null) {
        //     beatTimer.stopBeatTimer();
        // }
        if (!play)
            return;
        stop();
        synchronized (mLock) {
            mBeatDivisionSampleCount = 0;
        }
    }
}

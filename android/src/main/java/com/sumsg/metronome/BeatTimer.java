package com.sumsg.metronome;

import android.os.Handler;
import android.os.Looper;
import io.flutter.plugin.common.EventChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;


public class BeatTimer {
    // private final String TAG = "BeatTimer";
    private final EventChannel.EventSink eventTickSink;
    private Handler handler;
    private Runnable beatRunnable;
    private int timeSignature;
    private AtomicInteger currentTickAtomic = new AtomicInteger(1);
    private AtomicBoolean tickActive = new AtomicBoolean(false);
    private final Lock lock;
    private Condition tickEvent;

    BeatTimer(EventChannel.EventSink _eventTickSink, int _timeSignature, Lock _lock, Condition _tickEvent) {
        eventTickSink = _eventTickSink;
        timeSignature = _timeSignature;
        lock = _lock;
        tickEvent = _tickEvent;
    }

    public void startBeatTimer(int bpm) {
        
        stopBeatTimer();
        handler = new Handler(Looper.getMainLooper());
        
        tickActive.set(true);
        if ((handler!=null)&&(eventTickSink!=null)){

            beatRunnable = new Runnable() {
                @Override
                public void run() {
                    
                    try {
                        waitForTick();
                        eventTickSink.success(currentTickAtomic.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }                                 
                                                         
                    handler.post(this);
                }
            };

            handler.post(beatRunnable);  
        
        }
    }

    public void waitForTick() throws InterruptedException {
        lock.lock();
        try {
           
            if(!tickEvent.await(1000, TimeUnit.MILLISECONDS)){
                //Log.e(TAG, "Timeout");
                System.out.println("Timeout reached without signal.");
            }  // Waiting for the signal
            
            
        } finally {
            lock.unlock();
        }
    }

    public void stopBeatTimer() {
        if (handler != null && beatRunnable != null) {
            handler.removeCallbacks(beatRunnable);
            handler = null;
            beatRunnable = null;
        }
        tickActive.set(false);
    }

    public void synchronizeTicks(int currentTick){
        currentTickAtomic.set(currentTick);
    }
}

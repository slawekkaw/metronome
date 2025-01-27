package com.sumsg.metronome;

import android.os.Handler;
import android.os.Looper;
import io.flutter.plugin.common.EventChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class BeatTimer {
    
    // private final String TAG = "BeatTimer";
    private final EventChannel.EventSink eventTickSink;
    private Handler handler;
    private Runnable beatRunnable;
    private AtomicInteger currentTickAtomic = new AtomicInteger(1);

    BeatTimer(EventChannel.EventSink _eventTickSink) {
        eventTickSink = _eventTickSink;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            stopBeatTimer();
            handler = null;
        } finally {
            super.finalize();
        }
    }

    public void startBeatTimer(int bpm, int runForTicks) {
        
        stopBeatTimer();
        currentTickAtomic.set(1);
        //handler = new Handler(Looper.getMainLooper());
        double timerIntervalInSamples = 60 / (double) bpm * 1000;
        if (eventTickSink!=null){
         
            beatRunnable = new Runnable() {
                @Override
                public void run() {
                    eventTickSink.success(currentTickAtomic.get());
                 
                    if( currentTickAtomic.get() < runForTicks)
                    {
                       currentTickAtomic.getAndIncrement();
                       handler.postDelayed(this, (long) (timerIntervalInSamples));
                    }else{
                        handler.removeCallbacks(beatRunnable);// stops the timer
                        beatRunnable = null;
                    }
                }
            };

            handler.post(beatRunnable);// start immadietely
        }
    }

   public void sendEvent(int signatureNumber) {
        if (eventTickSink != null) {
            eventTickSink.success(signatureNumber);
        }
    }

    public void stopBeatTimer() {
        if (handler != null && beatRunnable != null) {
            handler.removeCallbacks(beatRunnable);
            beatRunnable = null;
        }
    }

}

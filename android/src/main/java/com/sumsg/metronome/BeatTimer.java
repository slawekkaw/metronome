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
    private int timeSignature;
    private AtomicInteger currentTickAtomic = new AtomicInteger(1);

    BeatTimer(EventChannel.EventSink _eventTickSink, int _timeSignature) {
        eventTickSink = _eventTickSink;
        timeSignature = _timeSignature;
    }

    public void startBeatTimer(int bpm) {
        
        stopBeatTimer();
        handler = new Handler(Looper.getMainLooper());
        double timerIntervalInSamples = 60 / (double) bpm;
        if (eventTickSink!=null){
         
            beatRunnable = new Runnable() {
                @Override
                public void run() {
                    int signatureNumber = currentTickAtomic.get();
                    eventTickSink.success(signatureNumber);
                    handler.postDelayed(this, (long) (timerIntervalInSamples * 1000));
                }
            };

            handler.post(beatRunnable);
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
            handler = null;
            beatRunnable = null;
        }
    }

    public void synchronizeTicks(int currentTick){
        currentTickAtomic.set(currentTick);
    }
}

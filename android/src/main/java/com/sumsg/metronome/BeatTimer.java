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
    //private AtomicInteger currentTickAtomic = new AtomicInteger(1);
    private int currentTick = 1;

    BeatTimer(EventChannel.EventSink _eventTickSink) {
        eventTickSink = _eventTickSink;
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            //stopBeatTimer();
            handler = null;
        } finally {
            super.finalize();
        }
    }

    // public void startBeatTimer(int bpm, int runForTicks) {
       
    //     long currentTimeMillis = System.currentTimeMillis();
    //     System.out.println("Start Beat Timer Event timestamp: " + currentTimeMillis);
        
    //     stopBeatTimer();
    //     currentTick = 1;
        
    //     double timerIntervalInSamples = 60 / (double) bpm * 1000;
    //     if (eventTickSink!=null){
         
    //         beatRunnable = new Runnable() {
    //             @Override
    //             public void run() {        
    //                 long currentTimeMillis = System.currentTimeMillis();
    //                 System.out.println("eventTick send timestamp: " + currentTimeMillis);
    //                 eventTickSink.success(currentTick);      
    //                 if( currentTick < runForTicks)
    //                 {
    //                    currentTick++;
    //                    handler.postDelayed(this, (long) (timerIntervalInSamples));
    //                 }else{
    //                     handler.removeCallbacks(beatRunnable);// stops the timer
    //                     beatRunnable = null;
    //                 }
    //             }
    //         };

    //         handler.post(beatRunnable);// start immadietely
    //     }
    // }

   public void sendEvent(int signatureNumber) {

        handler.post(new Runnable() {
        @Override
        public void run() {
            if (eventTickSink != null) {
                eventTickSink.success(signatureNumber);
            }
        }
        });   
    //System.out.println("eventTick send timestamp: ");
    }

    // public void stopBeatTimer() {
    //     if (handler != null && beatRunnable != null) {
    //         handler.removeCallbacks(beatRunnable);
    //         beatRunnable = null;
    //     }
    // }

}

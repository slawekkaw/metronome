package com.sumsg.metronome;

import android.content.Context;

import androidx.annotation.NonNull;
import java.util.Objects;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import android.app.Activity;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;


/** MetronomePlugin */
public class MetronomePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native
  /// Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine
  /// and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  //
  private EventChannel eventTick;
  private EventChannel.EventSink eventTickSink;
  // private final String TAG = "metronome";
  /// Metronome
  private Metronome metronome = null;
  private Context context;
  private Activity activity;

  @Override 
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
      activity = binding.getActivity(); // ✅ Prawidłowy sposób pobrania Activity
  }

  @Override 
  public void onDetachedFromActivity() {
      activity = null;
  }

  @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        // Implement logic if needed, otherwise leave it empty
        activity = binding.getActivity();
  }

  @Override
    public void onDetachedFromActivityForConfigChanges() {
        // Możesz dodać tutaj kod, jeśli trzeba coś zrobić przy odłączeniu aktywności w wyniku zmiany konfiguracji.
    }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "metronome");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
    
    eventTick = new EventChannel(flutterPluginBinding.getBinaryMessenger(),
        "metronome_tick");
    eventTick.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object args, EventChannel.EventSink events) {
        eventTickSink = events;
      }

      @Override
      public void onCancel(Object args) {
        eventTickSink = null;
      }
    });
  }



  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "init":
        metronomeInit(call);
        break;
      case "play":
        Integer _bpm = call.argument("bpm");
        if (_bpm == null)
          _bpm = 120;
        metronome.play(_bpm);
        break;
      case "pause":
        metronome.pause();
        break;
      case "stop":
        metronome.stop();
        break;
      case "getVolume":
        result.success(metronome.getVolume());
        break;
      case "setVolume":
        setVolume(call);
        break;
      case "isPlaying":
        result.success(metronome.isPlaying());
        break;
      case "setBPM":
        setBPM(call);
        break;
      case "getBPM":
        result.success(metronome.getBPM());
        break;
      case "setAudioFile":
        setAudioFile(call);
        break;
      case "destroy":
        metronome.destroy();
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    context = null;
    channel.setMethodCallHandler(null);
    eventTick.setStreamHandler(null);
  }

  private void metronomeInit(@NonNull MethodCall call) {
    if (!Objects.equals(call.argument("path"), "")) {
      String _mainFilePath = call.argument("path");
      String _accentedFilePath = call.argument("accentedPath");
      boolean enableTickCallback = call.argument("enableTickCallback");
      metronome = new Metronome(context, _mainFilePath, _accentedFilePath);
      if (enableTickCallback && eventTickSink!=null){
        metronome.enableTickCallback(eventTickSink, activity);
      }
      setVolume(call);
      setBPM(call);
      setTimeSignature(call);
    }
  }

  private void setVolume(@NonNull MethodCall call) {
    if (metronome != null) {
      Double _volume = call.argument("volume");
      if (_volume != null) {
        float _volume1 = _volume.floatValue();
        metronome.setVolume(_volume1);
      }
    }
  }

  private void setBPM(@NonNull MethodCall call) {
    if (metronome != null) {
      Integer _bpm = call.argument("bpm");
      if (_bpm != null) {
        metronome.setBPM(_bpm);
      }
    }
  }

  private void setAudioFile(@NonNull MethodCall call) {
    if (metronome != null) {
      metronome.setAudioFile(call.argument("path"));
    }
  }

  private void setTimeSignature(@NonNull MethodCall call) {
    if (metronome != null) {
      Integer _timeSignature = call.argument("timeSignature");
      if (_timeSignature != null) {
        metronome.setTimeSignature(_timeSignature);
      }
    }
  }
}

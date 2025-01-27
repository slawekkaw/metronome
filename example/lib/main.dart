//import 'package:flutter/foundation.dart';
import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:metronome/metronome.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final iconMetronomeKey = GlobalKey();
  final _metronomePlugin = Metronome();
  bool isplaying = false;
  int bpm = 120;
  int vol = 50;
  ValueNotifier<String> metronomeIcon = ValueNotifier<String>('assets/metronome-left.png');
  String metronomeIconRight = 'assets/metronome-right.png';
  String metronomeIconLeft = 'assets/metronome-left.png';
  ValueNotifier<int> currentTick = ValueNotifier<int>(1);
  final List wavs = [
    'base',
    'claves',
    'hihat',
    'snare',
    'sticks',
    'woodblock_high'
  ];
  int prevTickTime = 0;

  int getCurrentTime() {
    return DateTime.now().millisecondsSinceEpoch;
  }

  @override
  void initState() {
    super.initState();
    _metronomePlugin.init(
      'assets/audio/snare44_wav.wav',
      accentedPath: 'assets/audio/claves44_wav.wav',
      bpm: bpm,
      volume: vol,
      enableSession: true,
      enableTickCallback: true,
      timeSignature: 4,

    );
    _metronomePlugin.onListenTick((currentTickIntern) {
      //if (kDebugMode) {
        //print(onEvent.toString());
        //print('tick');

      //int currentTime = getCurrentTime();
      //int diff = (currentTime - prevTickTime);
      //prevTickTime = currentTime;
      log('Current tick app: $currentTickIntern ');
      currentTick.value = currentTickIntern;
      // //}
      
      if (metronomeIcon.value == metronomeIconRight) {
        //log("left");
        metronomeIcon.value = metronomeIconLeft;
      } else {
        metronomeIcon.value = metronomeIconRight;
        //log("right");
      }
      
      // setState(() {
      //   if (metronomeIcon == metronomeIconRight) {
      //     metronomeIcon = metronomeIconLeft;
      //   } else {
      //     metronomeIcon = metronomeIconRight;
      //   }
      // });
    });
  }

  @override
  void dispose() {
    _metronomePlugin.destroy();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Metronome example'),
        ),
        body: Container(
          padding: const EdgeInsets.all(10),
          child: ListView(
            children: [
              //MetronomeImage(key:iconMetronomeKey,   metronomeIcon: metronomeIcon),
              ValueListenableBuilder<String>(
                valueListenable: metronomeIcon,
                builder: (BuildContext context, value, Widget? child) {
                  return Image.asset(
                      metronomeIcon.value,
                      height: 100,
                      gaplessPlayback: true,
                    );
                },
              ),
              Text(
                'BPM:$bpm',
                style: const TextStyle(fontSize: 20),
              ),
              Slider(
                value: bpm.toDouble(),
                min: 30,
                max: 300,
                divisions: 270,
                onChangeEnd: (val) {
                  _metronomePlugin.setBPM(bpm);
                },
                onChanged: (val) {
                  bpm = val.toInt();
                  setState(() {});
                },
              ),
              Text(
                'Volume:$vol%',
                style: const TextStyle(fontSize: 20),
              ),
              Slider(
                value: vol.toDouble(),
                min: 0,
                max: 100,
                divisions: 100,
                onChangeEnd: (val) {
                  _metronomePlugin.setVolume(vol);
                },
                onChanged: (val) {
                  vol = val.toInt();
                  setState(() {});
                },
              ),
              SizedBox(
                width: 200,
                height: 350,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: wavs
                      .map(
                        (wav) => ElevatedButton(
                          child: Text(wav),
                          onPressed: () {
                            _metronomePlugin.setAudioAssets(
                                'assets/audio/${wav}44_wav.wav');
                          },
                        ),
                      )
                      .toList(),
                ),
              ),
            ],
          ),
        ),
        bottomNavigationBar: ValueListenableBuilder<int>(
                valueListenable: currentTick,
                builder: (BuildContext context, value, Widget? child) {
          return Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: List.generate(4, (index) {
              return Row(
                children: [
            Radio<int>(
              value: index + 1,
              groupValue: currentTick.value,
              onChanged: (int? value) {
                //setState(() {
                //  currentTick = value!;
                //});
              },
            ),
            Text('${index + 1}'),
                ],
              );
            }),
          );}
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () async {
            if (isplaying) {
              _metronomePlugin.pause();
              isplaying = false;
            } else {
              _metronomePlugin.setVolume(vol);
              _metronomePlugin.play(bpm);
              isplaying = true;
            }
            // int? bpm2 = await _metronomePlugin.getBPM();
            // print(bpm2);
            // int? vol2 = await _metronomePlugin.getVolume();
            // print(vol2);
            setState(() {});
          },
          child: Icon(isplaying ? Icons.pause : Icons.play_arrow),
        ),
      ),
    );
  }
}


//  class MetronomeImage extends StatefulWidget {
//      const MetronomeImage({Key? key, required this.metronomeIcon}) : super(key: key);

//      final String metronomeIcon;

//       @override
//                 MetronomeImageState createState() => MetronomeImageState();
//   }

// class MetronomeImageState extends State<MetronomeImage> {
//   @override
//   Widget build(BuildContext context) {
//     return Image.asset(
//       widget.metronomeIcon,
//       height: 100,
//       gaplessPlayback: true,
//     );
//   }
// }
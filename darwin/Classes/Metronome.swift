
import AVFoundation

class Metronome {
    private var beatTimer:BeatTimer?
    private var mixerNodeMain: AVAudioMixerNode?
    private var mixerNodeAccented: AVAudioMixerNode?
    //var
    public var audioBpm: Int = 120
    public var audioVolume: Float = 0
    public var timeSignature: Int = 4
    private var currentBeat: Int = 1
    private var audioActive: Bool = false;
    //
    private var timer: DispatchSourceTimer?

    private var playerMain:  AudioPlayer?
    private var playerAccented:  AudioPlayer?

    var lastTickTime: TimeInterval?
    
    init(mainFile: URL,accentedFile: URL,enableSession: Bool) {

        playerMain = AudioPlayer(fileUrl: mainFile) 
        playerAccented = AudioPlayer(fileUrl: accentedFile)  

        // mixerNodeMain = playerMain?.audioEngine.mainMixerNode
        // mixerNodeMain?.outputVolume = audioVolume    

        // mixerNodeAccented = playerAccented?.audioEngine.mainMixerNode
        // mixerNodeAccented?.outputVolume = audioVolume

    }

    public func enableTickCallback(_eventTickSink: EventTickHandler) {
       beatTimer = BeatTimer(eventTick: _eventTickSink)
    }

    func play(bpm: Int, timeSignature: Int) {
        //NSLog("Starting beat timer with bpm: \(bpm) and timeSignature: \(timeSignature)")
        self.audioBpm = bpm
        self.timeSignature = timeSignature
        self.currentBeat = 1
        audioActive = true;

       
        let interval = 60.0 / Double(bpm)
        let queue = DispatchQueue(label: "gignotesmetronome.queue", qos: .userInteractive)

        timer = DispatchSource.makeTimerSource(queue: queue)
        timer?.schedule(deadline: .now(), repeating: interval)
        timer?.setEventHandler { [weak self] in
            guard let strongSelf = self else { return }  // Unwrapping self safely
            DispatchQueue.main.async {
                //strongSelf.setVolume(vol: strongSelf.audioVolume)
                strongSelf.tick()
                strongSelf.currentBeat += 1
                if( strongSelf.currentBeat > timeSignature){
                    strongSelf.currentBeat = 1
                }
                
            }
        }
        timer?.resume()
      
    }

    private func tick() {
        let currentTime = Date().timeIntervalSince1970*1000
        if let lastTime = self.lastTickTime {
            let delta = currentTime - lastTime
                print("🔍 TickTime: \(delta) mili sekundy")
            }

        self.lastTickTime = currentTime

        if( currentBeat==1){
            playerAccented?.play(volume: audioVolume)
        }else{
            playerMain?.play(volume: audioVolume)
        }
        if(beatTimer != nil){
           beatTimer?.sendEvent(currentTickToSend: currentBeat)
           
        }
    }

    func pause() {
        self.stop()
    }
    func stop() {
        NSLog("Stop")
        audioActive = false;
        timer?.cancel()
        timer = nil
        
        self.currentBeat = 1
        
    }
    func setBPM(bpm: Int) {
        NSLog("Set BPM")
        audioBpm = bpm
        // if audioPlayerNode.isPlaying {
        //     play(bpm: self.audioBpm, timeSignature: self.timeSignature)
        //     if(beatTimer != nil){
        //         beatTimer?.startBeatTimer(bpm: bpm, runForTicks:timeSignature)
        //     }
        // }
    }
    func setTimeSignature(timeSignature: Int) {
        self.timeSignature = timeSignature

    }
    var getBPM: Int {
        return audioBpm;
    }
    var getVolume: Int {
        return Int(audioVolume * 100);
    }
    func setVolume(vol: Float) {
        //NSLog("setVolume"+String(vol))
        audioVolume = vol
        playerMain?.setVolume(vol)
        playerAccented?.setVolume(vol)
        //mixerNodeMain?.outputVolume = vol
        //mixerNodeAccented?.outputVolume = vol
    }
    var isPlaying: Bool {
        //return audioPlayerNode.isPlaying
        return false;
    }
    func destroy() {

        timer?.cancel()
        timer = nil
    }
    func setAudioFile(mainFile: URL) {
        NSLog("setAudioFile")
        // audioFileMain = try! AVAudioFile(forReading: mainFile)
        // if isPlaying {
        //     stop()
        //     play(bpm: audioBpm, timeSignature: timeSignature)
        // }
    }
}



// class AudioPlayer {
//     var audioEngine = AVAudioEngine()
//     var audioPlayerNode = AVAudioPlayerNode()
//     var audioFile: AVAudioFile?

//     init(fileUrl: URL) {
//         do {
      
//             audioFile = try AVAudioFile(forReading: fileUrl)   
//             audioEngine.attach(audioPlayerNode)
//             audioEngine.connect(audioPlayerNode, to: audioEngine.mainMixerNode, format: audioFile?.processingFormat)
//             try audioEngine.start()

//         } catch {
//             print("❌ Błąd inicjalizacji: \(error.localizedDescription)")
//         }
//     }

//     func play() {
//         guard let audioFile = audioFile else { return }

//         audioPlayerNode.stop()  // Zatrzymaj poprzednie odtwarzanie
//         audioEngine.mainMixerNode.outputVolume = 0
//         audioPlayerNode.scheduleFile(audioFile, at: nil, completionHandler: nil)    
        
//         audioPlayerNode.play()  // Odtwarzaj dźwięk
//     }
// }


class AudioPlayer {
    private var player: AVAudioPlayer?
    private var audioFileURL: URL?
    
    init(fileUrl: URL) {
        self.audioFileURL = fileUrl
        prepareAudio()
    }
    
    /// Przygotowanie audio (ładowanie pliku)
    private func prepareAudio() {
        guard let url = audioFileURL else { return }
        do {
            player = try AVAudioPlayer(contentsOf: url)
            player?.prepareToPlay()  // Buforowanie dźwięku, aby uniknąć opóźnień
        } catch {
            print("Błąd podczas ładowania pliku audio: \(error.localizedDescription)")
        }
    }

    /// Odtwarzanie dźwięku z określoną głośnością
    func play(volume: Float) {
        guard let player = player else { return }
        
        player.volume = volume  // Ustawienie głośności przed odtwarzaniem
        player.play()
    }
    
    /// Zatrzymanie odtwarzania
    func stop() {
        player?.stop()
        player?.currentTime = 0  // Reset czasu odtwarzania
    }
    
    /// Zmiana głośności w trakcie odtwarzania
    func setVolume(_ volume: Float) {
        player?.volume = volume
    }
}

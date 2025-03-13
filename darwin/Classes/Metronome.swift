
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

    }

    public func enableTickCallback(_eventTickSink: EventTickHandler) {
       beatTimer = BeatTimer(eventTick: _eventTickSink)
    }

    func play(bpm: Int, timeSignature: Int) {
        NSLog("Starting beat timer with bpm: \(bpm) and timeSignature: \(timeSignature)")
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
        // let currentTime = Date().timeIntervalSince1970*1000
        // if let lastTime = self.lastTickTime {
        //     let delta = currentTime - lastTime
        //         print("🔍 TickTime: \(delta) mili sekundy")
        //     }

        // self.lastTickTime = currentTime

        if( currentBeat>1 || self.timeSignature == 0){
            playerMain?.play(volume: audioVolume)
        }else{
            playerAccented?.play(volume: audioVolume)
            
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

    }
    func setTimeSignature(timeSignature: Int) {
        self.timeSignature = timeSignature

    }
    var getBPM: Int {
        return audioBpm;
    }

    var isPlaying: Bool {
        return audioActive
    }

    var getVolume: Int {
        return Int(audioVolume * 100);
    }
    func setVolume(vol: Float) {
        //NSLog("setVolume"+String(vol))
        audioVolume = vol
        playerMain?.setVolume(vol)
        playerAccented?.setVolume(vol)

    }

    func destroy() {

        timer?.cancel()
        timer = nil
    }
    func setAudioFile(mainFile: URL) {
        playerMain = AudioPlayer(fileUrl: mainFile) 
        NSLog("setAudioFile")
        // audioFileMain = try! AVAudioFile(forReading: mainFile)
        // if isPlaying {
        //     stop()
        //     play(bpm: audioBpm, timeSignature: timeSignature)
        // }
    }
    func setAccentedAudioFile(accentedFile: URL) {
        NSLog("setAudioFile")
        playerAccented = AudioPlayer(fileUrl: accentedFile)
        // audioFileMain = try! AVAudioFile(forReading: mainFile)
        // if isPlaying {
        //     stop()
        //     play(bpm: audioBpm, timeSignature: timeSignature)
        // }
    }
}

class AudioPlayer {
    private var player: AVAudioPlayer?
    private var audioFileURL: URL?
    
    init(fileUrl: URL) {
        self.audioFileURL = fileUrl
        prepareAudio()
    }
        
    private func prepareAudio() {
        guard let url = audioFileURL else { return }
        do {
            player = try AVAudioPlayer(contentsOf: url)
            player?.prepareToPlay()  
        } catch {
            print("Error loading file audio: \(error.localizedDescription)")
        }
    }

    func play(volume: Float) {
        guard let player = player else { return }
        
        player.volume = volume  
        player.play()
    }

    func stop() {
        player?.stop()
        player?.currentTime = 0  
    }  

    func setVolume(_ volume: Float) {
        player?.volume = volume
    }
}

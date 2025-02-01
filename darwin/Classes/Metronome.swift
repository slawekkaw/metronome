
import AVFoundation

class Metronome {
    private var beatTimer:BeatTimer?
    //
    private var audioPlayerNode: AVAudioPlayerNode
    private var audioFileMain: AVAudioFile
    private var audioFileAccented: AVAudioFile
    private var audioEngine: AVAudioEngine

    private var mixerNode: AVAudioMixerNode

    private var bufferAccented: AVAudioPCMBuffer
    private var buffer: AVAudioPCMBuffer


    //var
    public var audioBpm: Int = 120
    public var audioVolume: Float = 0
    public var timeSignature: Int = 4
    private var currentBeat: Int = 1
    private var audioActive: Bool = false;
    //
    
    init(mainFile: URL,accentedFile: URL,enableSession: Bool) {
        audioFileMain = try! AVAudioFile(forReading: mainFile)
        audioFileAccented = try! AVAudioFile(forReading: accentedFile)

        audioPlayerNode = AVAudioPlayerNode()      
        audioEngine = AVAudioEngine()
        audioEngine.attach(self.audioPlayerNode)
        mixerNode = audioEngine.mainMixerNode
        mixerNode.outputVolume = audioVolume             
        audioEngine.connect(audioPlayerNode, to: mixerNode, format: audioFileMain.processingFormat)
        audioEngine.prepare()
      
        if !self.audioEngine.isRunning {
            do {
                try self.audioEngine.start()
            } catch {
                print(error)
            }
        }
#if os(iOS)
        if enableSession {
            let session = AVAudioSession.sharedInstance()
            do {
                try session.setActive(true)
                //try session.overrideOutputAudioPort(AVAudioSession.PortOverride.none)
                try session.setCategory(AVAudioSession.Category.playback, options: [.defaultToSpeaker, .allowAirPlay, .allowBluetoothA2DP])
            } catch {
                print(error)
            }
        }
        UIApplication.shared.beginReceivingRemoteControlEvents()
#endif    
    
        buffer = AVAudioPCMBuffer()
        bufferAccented = AVAudioPCMBuffer()
    }

    public func enableTickCallback(_eventTickSink: EventTickHandler) {
       beatTimer = BeatTimer(eventTick: _eventTickSink)
    }
    private func generateBuffer(bpm: Int, audioFile: AVAudioFile ) -> AVAudioPCMBuffer {

        audioFile.framePosition = 0
        let beatLength = AVAudioFrameCount(audioFile.processingFormat.sampleRate * 60 / Double(bpm) )
        audioFile.framePosition = 0
        let bufferMainClick = AVAudioPCMBuffer(pcmFormat: audioFile.processingFormat,
                                               frameCapacity: beatLength)!
        try! audioFile.read(into: bufferMainClick)
        bufferMainClick.frameLength = beatLength
        let bufferBar = AVAudioPCMBuffer(pcmFormat: audioFile.processingFormat,
                                         frameCapacity:  beatLength)!
//        bufferBar.frameLength = 4 * beatLength
        bufferBar.frameLength = beatLength

        // don't forget if we have two or more channels then we have to multiply memory pointee at channels count
        let channelCount = Int(audioFile.processingFormat.channelCount)
        let mainClickArray = Array(
            UnsafeBufferPointer(start: bufferMainClick.floatChannelData![0],
                                count: channelCount * Int(beatLength))
        )

        var barArray = [Float]()
            barArray.append(contentsOf: mainClickArray)
        bufferBar.floatChannelData!.pointee.update(from: barArray,
                                                   count: channelCount * Int(bufferBar.frameLength))
        return bufferBar
    }

    private func playOneBar() {

        let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
                let timestamp = dateFormatter.string(from: Date())

        NSLog("\(timestamp) Start playOneBar, current beat: \(currentBeat)")
        if(beatTimer != nil){
           beatTimer?.startBeatTimer(bpm: audioBpm, runForTicks:timeSignature)
        }
        self.audioPlayerNode.scheduleBuffer(self.bufferAccented, at: nil, options: .interrupts,  completionHandler: self.handleBeatCompletion)  
    }


    private func handleBeatCompletion() {
        //NSLog("Start handleBeatCompletion, , current beat: \(currentBeat)")
        if self.currentBeat >= self.timeSignature {
            self.currentBeat = 1
            if( audioActive){
                // let dateFormatter = DateFormatter()
                // dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
                // let timestamp = dateFormatter.string(from: Date())
                // NSLog("\(timestamp) restart BAR, current beat: \(currentBeat)")
                playOneBar()
            }
        }else{
            // let dateFormatter = DateFormatter()
            //     dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
            //     let timestamp = dateFormatter.string(from: Date())
            
            // NSLog("\(timestamp) start next BEAT , current beat: \(currentBeat)")
            self.currentBeat += 1
            if( audioActive){
                self.audioPlayerNode.scheduleBuffer(self.buffer, at: nil,  completionHandler: self.handleBeatCompletion)  
            }
        }
    }


    func play(bpm: Int) {
        NSLog("Starting beat timer with bpm: \(bpm) and timeSignature: \(timeSignature)")
        audioBpm = bpm
        self.buffer = generateBuffer(bpm: bpm, audioFile: audioFileMain)
        self.bufferAccented = generateBuffer(bpm: bpm, audioFile: audioFileAccented)
        self.currentBeat = 1
        audioActive = true;
        // if(beatTimer != nil){
        //     beatTimer?.startBeatTimer(bpm: audioBpm, runForTicks:timeSignature)
        // }
        //NSLog("Before playOneBar")
        playOneBar()   

        if !audioPlayerNode.isPlaying {
            //NSLog("Start: self.audioPlayerNode.play()")
            self.audioPlayerNode.play()
        }

       
    }
    func pause() {
        self.stop()
        // audioPlayerNode.pause()
        // self.currentBeat = 1
        // if(beatTimer != nil){
        //     beatTimer?.stopBeatTimer()
        // }
    }
    func stop() {
        NSLog("Stop")
        audioActive = false;
        if audioPlayerNode.isPlaying {
            self.audioPlayerNode.stop()
        }
        self.currentBeat = 1
        if(beatTimer != nil){
            beatTimer?.stopBeatTimer()
        }
    }
    func setBPM(bpm: Int) {
        NSLog("Set BPM")
        audioBpm = bpm
        if audioPlayerNode.isPlaying {
            play(bpm: self.audioBpm)
            if(beatTimer != nil){
                beatTimer?.startBeatTimer(bpm: bpm, runForTicks:timeSignature)
            }
        }
    }
    func setTimeSignature(timeSignature: Int) {
        NSLog("setTimeSignature")
        self.timeSignature = timeSignature
        if audioPlayerNode.isPlaying {
            play(bpm: self.audioBpm)
            if(beatTimer != nil){
                beatTimer?.startBeatTimer(bpm: audioBpm, runForTicks:timeSignature)
            }
        }
    }
    var getBPM: Int {
        return audioBpm;
    }
    var getVolume: Int {
        return Int(audioVolume * 100);
    }
    func setVolume(vol: Float) {
        audioVolume = vol
        mixerNode.outputVolume = vol
    }
    var isPlaying: Bool {
        return audioPlayerNode.isPlaying
    }
    func destroy() {
        audioPlayerNode.reset()
        audioPlayerNode.stop()
        audioEngine.reset()
        audioEngine.stop()
        audioEngine.detach(audioPlayerNode)
        if(beatTimer != nil){
            beatTimer?.stopBeatTimer()
        }
    }
    func setAudioFile(mainFile: URL) {
        NSLog("setAudioFile")
        audioFileMain = try! AVAudioFile(forReading: mainFile)
        if isPlaying {
            stop()
            play(bpm: audioBpm)
        }
    }
}

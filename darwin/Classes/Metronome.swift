
import AVFoundation

class Metronome {
    private var beatTimer:BeatTimer?
    //
    private var audioPlayerNode: AVAudioPlayerNode
    private var audioFileMain: AVAudioFile
    private var audioFileAccented: AVAudioFile
    private var audioEngine: AVAudioEngine

    private var mixerNode: AVAudioMixerNode
    //var
    public var audioBpm: Int = 120
    public var audioVolume: Float = 0
    public var timeSignature: Int = 4
    private var currentBeat: Int = 1
    //
    
    init(mainFile: URL,accentedFile: URL,enableSession: Bool) {
        //
        audioFileMain = try! AVAudioFile(forReading: mainFile)
        audioFileAccented = try! AVAudioFile(forReading: accentedFile)

        audioPlayerNode = AVAudioPlayerNode()
        //audioPlayerAccentedNode = AVAudioPlayerNode()
        
        audioEngine = AVAudioEngine()
        //audioEngineAccented = AVAudioEngine()

        audioEngine.attach(self.audioPlayerNode)
        //audioEngineAccented.attach(self.audioPlayerNode)
        
        mixerNode = audioEngine.mainMixerNode
        //mixerNodeAccented = audioEngineAccented.mainMixerNode

        mixerNode.outputVolume = audioVolume
        //mixerNodeAccented.outputVolume = audioVolume
        
        audioEngine.connect(audioPlayerNode, to: mixerNode, format: audioFileMain.processingFormat)
        //audioEngineAccented.connect(audioPlayerAccentedNode, to: mixerNodeAccented, format: audioFileAccented.processingFormat)

        audioEngine.prepare()
        //audioEngineAccented.prepare()

        if !self.audioEngine.isRunning {
            do {
                try self.audioEngine.start()
            } catch {
                print(error)
            }
        }
        // if !self.audioEngineAccented.isRunning {
        //     do {
        //         try self.audioEngineAccented.start()
        //     } catch {
        //         print(error)
        //     }
        // }
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
    }
    public func enableTickCallback(_eventTickSink: EventTickHandler) {
       beatTimer = BeatTimer(eventTick: _eventTickSink)
    }
    private func generateBuffer(bpm: Int) -> AVAudioPCMBuffer {

        audioFileMain.framePosition = 0
        let beatLength = AVAudioFrameCount(audioFileMain.processingFormat.sampleRate * 60 / Double(bpm) )
        let bufferMainClick = AVAudioPCMBuffer(pcmFormat: audioFileMain.processingFormat,
                                               frameCapacity: beatLength)!
        try! audioFileMain.read(into: bufferMainClick)
        bufferMainClick.frameLength = beatLength
        let bufferBar = AVAudioPCMBuffer(pcmFormat: audioFileMain.processingFormat,
                                         frameCapacity:  beatLength)!
//        bufferBar.frameLength = 4 * beatLength
        bufferBar.frameLength = beatLength

        // don't forget if we have two or more channels then we have to multiply memory pointee at channels count
        let channelCount = Int(audioFileMain.processingFormat.channelCount)
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
    func play(bpm: Int) {
        audioBpm = bpm
        let buffer = generateBuffer(bpm: bpm)

        if audioPlayerNode.isPlaying {
            audioPlayerNode.scheduleBuffer(buffer, at: nil, options: .interruptsAtLoop, completionHandler: nil)
        } else {
            self.audioPlayerNode.play()
        }

        self.audioPlayerNode.scheduleBuffer(buffer, at: nil, options: .loops, completionHandler: nil)
        //
        if(beatTimer != nil){
            beatTimer?.startBeatTimer(bpm: bpm, runForTicks:timeSignature)
        }
    }
    func pause() {
        audioPlayerNode.pause()
        if(beatTimer != nil){
            beatTimer?.stopBeatTimer()
        }
    }
    func stop() {
        audioPlayerNode.stop()
        if(beatTimer != nil){
            beatTimer?.stopBeatTimer()
        }
    }
    func setBPM(bpm: Int) {
        audioBpm = bpm
        if audioPlayerNode.isPlaying {
            play(bpm: self.audioBpm)
            if(beatTimer != nil){
                beatTimer?.startBeatTimer(bpm: bpm, runForTicks:timeSignature)
            }
        }
    }
    func setTimeSignature(timeSignature: Int) {
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
        audioFileMain = try! AVAudioFile(forReading: mainFile)
        if isPlaying {
            stop()
            play(bpm: audioBpm)
        }
    }
}

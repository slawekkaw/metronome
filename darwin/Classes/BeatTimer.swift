class BeatTimer {
    private var eventTick: EventTickHandler
    private var currentTick = 1
    
    init(eventTick: EventTickHandler) {
        self.eventTick = eventTick
    }
    private var beatTimer : Timer? = nil {
       willSet {
           beatTimer?.invalidate()
       }
    }
    func startBeatTimer(bpm: Int, runForTicks: Int) {
        stopBeatTimer()
        let timerIntervallInSamples = 60 / Double(bpm)
        beatTimer = Timer.scheduledTimer(withTimeInterval: timerIntervallInSamples, repeats: true) { timer in
            self.eventTick.send(res: self.currentTick)
            if( self.currentTick < runForTicks){
                self.currentTick += 1
            }else{
                self.stopBeatTimer()
            }
        }
    }
    func stopBeatTimer() {
        guard beatTimer != nil else { return }
        beatTimer?.invalidate()
        beatTimer = nil
    }
}

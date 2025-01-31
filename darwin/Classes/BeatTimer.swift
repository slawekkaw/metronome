
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
         DispatchQueue.main.async {
            NSLog("Starting beat timer with bpm: \(bpm) and runForTicks: \(runForTicks)")
            self.currentTick = 1
            self.eventTick.send(res: self.currentTick) // First tick sent immediately
            self.currentTick += 1
            self.stopBeatTimer()
            let timerIntervallInSamples = 60 / Double(bpm)
            self.beatTimer = Timer.scheduledTimer(withTimeInterval: timerIntervallInSamples, repeats: true) { timer in
                self.eventTick.send(res: self.currentTick)
                if( self.currentTick < runForTicks){
                    self.currentTick += 1
                }else{
                    self.stopBeatTimer()
                }
            }
            if( self.beatTimer != nil){
                //NSLog("Beat timer created"); 
                RunLoop.main.add(self.beatTimer!, forMode: .common)
            }
         }
    }
    func stopBeatTimer() {
        if beatTimer == nil {
            //NSLog("stopBeatTimer() called, but timer was already nil")
            return
        }
        //NSLog("Stopping beat timer")
        beatTimer?.invalidate()
        beatTimer = nil
    }
}

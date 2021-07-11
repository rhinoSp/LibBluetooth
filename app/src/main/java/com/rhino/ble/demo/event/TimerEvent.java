package com.rhino.ble.demo.event;

/**
 * @author rhino
 * @since Create on 2019/12/6.
 **/
public class TimerEvent {

    public long timestamp;

    @Override
    public String toString() {
        return "TimerEvent{" +
                "timestamp=" + timestamp +
                '}';
    }

    public static TimerEvent currentTimerEvent() {
        TimerEvent timerEvent = new TimerEvent();
        timerEvent.timestamp = System.currentTimeMillis();
        return timerEvent;
    }

}

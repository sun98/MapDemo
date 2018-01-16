package cn.nibius.mapv2.util;

/**
 * Created by Nibius at 2018/1/16 16:22.
 */

public class Constant {
    public enum LightEvent {
        NOLIGHT, LONGLIGHT, TIMEDGREENLIGHT, TIMEDREDLIGHT, UNKNOWNLIGHT;
    }
    public enum RoadStateEvent{
        NOROADSTATE, ICE, OBSTACLE, UNKNOWNSTATE;
    }
}

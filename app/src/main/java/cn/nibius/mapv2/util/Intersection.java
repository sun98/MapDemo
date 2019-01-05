package cn.nibius.mapv2.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tinsmore on 2019/1/5.
 */

public class Intersection {

    public String ID;
    public double centerLat, centerLng;

    public Map currentState =  new HashMap();
    public Map timeToChange =  new HashMap();
    public Map approaches =  new HashMap();

}

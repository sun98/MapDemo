package cn.nibius.mapv2.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Modified by Tinsmore at 2019/1/5 14:05.
 * All messages from service to MainActivity
 */

public class MessagePackage {

    private Map intersections;
    private Vehicle myCar;

    public MessagePackage() {
        intersections =  new HashMap();
        myCar = new Vehicle();
    }

    public void setIntersections(Map intersections) {
        this.intersections = intersections;
    }

    public void setMyCar(Vehicle myCar) {
        this.myCar = myCar;
    }

    public Map getIntersections() { return intersections; }

    public Vehicle getMyCar() { return myCar; }
}

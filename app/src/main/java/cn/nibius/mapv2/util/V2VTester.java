package cn.nibius.mapv2.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nibius at 2018/1/19 00:42.
 */

public class V2VTester {
    private Map<String, Double> speed = new HashMap<>();
    private double selfLat, selfLng, selfSpeed, selfAngle, selfAcce;
    private double oldSelfSpeed;

    // These constant parameters should be modified
    private double ACCE_INTERVAL = 0.1;         // delta_speed divided by this value is acceleration (should be time)
    private double ACCE_LIMIT = -0.5;           // Lower bound of negative acceleration
//    这里应该是角度，不是弧度
    private double FRONT_ANGLE = Math.PI / 6;   // Cars in front of current car with +-30 degree
    private double REACT_TIME_THRES = 5;        // Threshold to judge whether the reaction time is enough

    // Initializer
    public V2VTester() {
        selfLat = selfLng = selfSpeed = selfAngle = selfAcce = oldSelfSpeed = -1;
    }

    public void update(double lat, double lng, double ang, double speed) {

        // Save old speed to calculate acceleration
        oldSelfSpeed = selfSpeed;

        selfLat = lat;
        selfLng = lng;
        selfAngle = ang;
        selfSpeed = speed;
        selfAcce = calcAcce(oldSelfSpeed, speed);
    }

    public Boolean test(double otherLat, double otherLng, double otherAngle,
                        double otherSpeed, String ID) {
        double tmpSpeed, tmpAcce;
        double car2carAngle, VehicleDist, reactTime;
        boolean State = false;  // true - need warning, false otherwise

        if (speed.containsKey(ID)) {
            // Has tracked this car
            tmpSpeed = speed.get(ID);
            tmpAcce = calcAcce(tmpSpeed, otherSpeed);

            // Test whether other car is in front of current car
            car2carAngle = AngleUtil.getAngle(selfLng, selfLat, otherLng, otherLat);
            if (Math.abs(car2carAngle - selfAngle) < FRONT_ANGLE) {
                // Test the distance (in time) and acceleration
                VehicleDist = AngleUtil.getDistance(selfLng, selfLat, otherLng, otherLat);
                reactTime = VehicleDist / selfSpeed;     // Assume the worst, when the front car has stopped
                if (reactTime < REACT_TIME_THRES && tmpAcce < ACCE_LIMIT) {
                    State = true;
                }
            }
        } else {
            // Save other car's speed to calculate acceleration
            speed.put(ID, otherSpeed);
        }

        // Clean up saved acceleration
        if (speed.size() > 50) {
            speed.clear();
        }

        return State;
    }

    private double calcAcce(double oldSpeed, double speed) {
        return (speed - oldSpeed) / ACCE_INTERVAL;
    }
}

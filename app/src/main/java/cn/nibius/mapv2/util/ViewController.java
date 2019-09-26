package cn.nibius.mapv2.util;

import android.location.Location;
import java.util.Map;
import android.util.Log;


public class ViewController {
    private Vehicle myCar;
    private Map inters;

    private double xCenterLat = 31.027853, xCenterLng = 121.421893;
    private double xRefeLat = 31.02754, xRefeLng = 121.4221;
    private double xCenterLeft = 0.28125, xCenterTop = 0.5011;
    private double xRefeLeft = 0.32958, xRefeTop = 0.9809;

    private double tCenterLat = 31.0290921, tCenterLng = 121.4257313;
    private double tRefeLat = 31.0277, tRefeLng = 121.426278;
    private double tCenterLeft = 0.62136, tCenterTop = 0.0582298;
    private double tRefeLeft = 0.5962, tRefeTop = 0.9982;

    private double xk1 = 0, xk2 = 0, tk1 = 0, tk2 = 0;
    private int cross = 0;
    // 0 - not in,  1 - xcross, 2 - tcross, 3 - other cross

    public ViewController(Vehicle car, Map intersections){
        myCar = car;
        inters = intersections;

        double Ax = xRefeLat-xCenterLat, Bx = xRefeLng-xCenterLng, Xx = xRefeLeft-xCenterLeft, Yx = xRefeTop-xCenterTop;
        xk1 = (Ax*Xx + Bx*Yx)/(Ax*Ax + Bx*Bx);
        xk2 = (Bx*Xx - Ax*Yx)/(Ax*Ax + Bx*Bx);

        double At = tRefeLat-tCenterLat, Bt = tRefeLng-tCenterLng, Xt = tRefeLeft-tCenterLeft, Yt = tRefeTop-tCenterTop;
        tk1 = (At*Xt + Bt*Yt)/(At*At + Bt*Bt);
        tk2 = (Bt*Xt - At*Yt)/(At*At + Bt*Bt);
    }

    public double getViewLeft(){
        if (cross == 1){
            double result = xCenterLeft + xk1*(myCar.currentLat - xCenterLat) + xk2*(myCar.currentLng - xCenterLng);
            if (result > 1)
                result = 1;
            if (result < 0)
                result = 0;
            return result;
        }
        else if (cross == 2){
            double result = tCenterLeft + tk1*(myCar.currentLat - tCenterLat) + tk2*(myCar.currentLng - tCenterLng);
            if (result > 1)
                result = 1;
            if (result < 0)
                result = 0;
            return result;
        }
        else {
            return 0;
        }
    }

    public double getViewTop(){
        if (cross == 1){
            double result = xCenterTop + xk1*(myCar.currentLng - xCenterLng) - xk2*(myCar.currentLat - xCenterLat);
            if (result > 1)
                result = 1;
            if (result < 0)
                result = 0;
            return result;
        }
        else if (cross == 2){
            double result = tCenterTop + tk1*(myCar.currentLng - tCenterLng) - tk2*(myCar.currentLat - tCenterLat);
            if (result > 1)
                result = 1;
            if (result < 0)
                result = 0;
            return result;
        }
        else {
            return 0;
        }
    }

    private double getDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results=new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }

    public int viewPos(){
        double distance_xcross, distance_tcross;
        distance_xcross = getDistance(myCar.currentLat, myCar.currentLng, xCenterLat, xCenterLng);
        distance_tcross = getDistance(myCar.currentLat, myCar.currentLng, tCenterLat, tCenterLng);

        if(distance_xcross <= 100 && distance_xcross < distance_tcross){
            cross = 1;
        }
        else if(distance_tcross <= 100){
            cross = 2;
        }
        else if(closeFactor() < 500){
            cross = 3;
        }
        else {
            cross = 0;
        }

        return cross;
    }

    private double closeFactor(){
        double minAngleDist = 9999;

        for(Object i : inters.values()) {
            Intersection inter = (Intersection) i;
            double angle = myCar.getAngle(myCar.currentLng, myCar.currentLat, inter.centerLng, inter.centerLat);
            double angle_diff = Math.abs(angle - myCar.heading);
            double distance = getDistance(myCar.currentLat, myCar.currentLng, inter.centerLat, inter.centerLng);
            if (angle_diff*distance < minAngleDist){
                minAngleDist = angle_diff*distance;
            }
        }

        return minAngleDist;
    }

    public String nextIntersection(){
        String targetID = null;
        double minAngleDist = 9999;

        for(Object i : inters.values()) {
            Intersection inter = (Intersection) i;
            double angle = myCar.getAngle(myCar.currentLng, myCar.currentLat, inter.centerLng, inter.centerLat);
            double angle_diff = Math.abs(angle - myCar.heading);
            double distance = getDistance(myCar.currentLat, myCar.currentLng, inter.centerLat, inter.centerLng);
            if (angle_diff*distance < minAngleDist){
                targetID = inter.ID;
                minAngleDist = angle_diff*distance;
            }
        }

        return targetID;
    }

    private int getDirection(double angle){
        if (angle < 45 || angle >= 315)
            return 0;
        else if (angle >= 45 && angle < 135)
            return 1;
        else if (angle >= 135 && angle < 225)
            return 2;
        else if (angle >= 225 && angle < 315)
            return 3;
        else
            return 0;
    }

    private boolean matchDirection(int dir, String name){
        if (name.equals("Northbound") && dir == 0)
            return true;
        else if (name.equals("Eastbound") && dir == 1)
            return true;
        else if (name.equals("Southbound") && dir == 2)
            return true;
        else if (name.equals("Westbound") && dir == 3)
            return true;
        else
            return false;
    }

    public String currentLane(String interID){
        String currentLane = "";
        Intersection intersection = (Intersection) inters.get(interID);
        double angle = myCar.getAngle(intersection.centerLng, intersection.centerLat, myCar.currentLng, myCar.currentLat);
        int direction = getDirection(angle);
        for (Object obj : intersection.approaches.values()) {
            Approach appr = (Approach) obj;
            for (Object lane : appr.lanesNodesList.keySet()) {
                currentLane = (String) lane;
                if (matchDirection(direction, appr.selfName)) {
                    return currentLane;
                }
                else{
                    break;
                }
            }
        }

        return currentLane;
    }

    public boolean needRemind(){
        double tunnelLng = 121.431, tunnelLat = 31.032;
        double dist = getDistance(myCar.currentLat, myCar.currentLng, tunnelLat, tunnelLng);
        if (dist < 100 && dist > 90)
            return true;
        else
            return false;
    }
}

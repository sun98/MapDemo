package cn.nibius.mapv2.util;


import android.location.Location;
import android.util.Log;

public class ViewController {
    private Viechle myCar;

    private double xCenterLat = 31.027853, xCenterLng = 121.421893;
    private double xRefeLat = 31.027845, xRefeLng = 121.421729;
    private double xCenterLeft = 0.4444, xCenterTop = 0.5;
    private double xRefeLeft = 0.3148, xRefeTop = 0.4375;

    private double tCenterLat = 31.029086, tCenterLng = 121.425732;
    private double tRefeLat = 0, tRefeLng = 0;
    private double tCenterLeft = 0.4444, tCenterTop = 0.5;
    private double tRefeLeft = 0.3148, tRefeTop = 0.4375;

    private double xk1 = 0, xk2 = 0, tk1 = 0, tk2 = 0;
    private int cross = 0;
    // 0 - not in  1 - xcross, 2 - tcross


    public ViewController(Viechle car){
        myCar = car;
        double A = xRefeLat-xCenterLat, B = xRefeLng-xCenterLng, X = xRefeLeft-xCenterLeft, Y = xRefeTop-xCenterTop;
        xk1 = (A*X + B*Y)/(A*A + B*B);
        xk2 = (B*X - A*Y)/(A*A + B*B);
    }


    public int toChangeView(){
        //Log.d("view","Distance: "+String.valueOf(getDistance(myCar.currentLat, myCar.currentLng, xCenterLat, xCenterLng)));
        if(getDistance(myCar.currentLat, myCar.currentLng, xCenterLat, xCenterLng) <= 200){
            cross = 1;
        }
        else if(getDistance(myCar.currentLat, myCar.currentLng, tCenterLat, tCenterLng) <= 200){
            cross = 2;
        }
        else {
            cross = 0;
        }

        return cross;
    }


    public double getViewLeft(){
        if (cross == 1){
            return xCenterLeft + xk1*(myCar.currentLat - xCenterLat) + xk2*(myCar.currentLng - xCenterLng);
        }
        else if (cross == 2){
            return 0;
        }
        else {
            return 0;
        }
    }


    public double getViewTop(){
        if (cross == 1){
            return xCenterTop + xk1*(myCar.currentLng - xCenterLng) - xk2*(myCar.currentLat - xCenterLat);
        }
        else if (cross == 2){
            return 0;
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
}

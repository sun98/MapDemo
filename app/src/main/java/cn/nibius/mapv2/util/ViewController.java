package cn.nibius.mapv2.util;


import android.location.Location;
import android.util.Log;

public class ViewController {
    private Viechle myCar;

    private double xCenterLat = 31.027853, xCenterLng = 121.421893;
    private double xRefeLat = 31.027845, xRefeLng = 121.421729;
    private double xCenterLeft = 0.4444, xCenterTop = 0.5;
    private double xRefeLeft = 0.3148, xRefeTop = 0.4375;

    private double tCenterLat = 31.0290921, tCenterLng = 121.4257313;
    private double tRefeLat = 31.02901, tRefeLng = 121.425636;
    private double tCenterLeft = 0.608235, tCenterTop = 0.121376;
    private double tRefeLeft = 0.51154, tRefeTop = 0.19757;

    private double xk1 = 0, xk2 = 0, tk1 = 0, tk2 = 0;
    private int cross = 0;
    // 0 - not in  1 - xcross, 2 - tcross


    public ViewController(Viechle car){
        myCar = car;

        double Ax = xRefeLat-xCenterLat, Bx = xRefeLng-xCenterLng, Xx = xRefeLeft-xCenterLeft, Yx = xRefeTop-xCenterTop;
        xk1 = (Ax*Xx + Bx*Yx)/(Ax*Ax + Bx*Bx);
        xk2 = (Bx*Xx - Ax*Yx)/(Ax*Ax + Bx*Bx);

        double At = tRefeLat-tCenterLat, Bt = tRefeLng-tCenterLng, Xt = tRefeLeft-tCenterLeft, Yt = tRefeTop-tCenterTop;
        tk1 = (At*Xt + Bt*Yt)/(At*At + Bt*Bt);
        tk2 = (Bt*Xt - At*Yt)/(At*At + Bt*Bt);
    }


    public int toChangeView(){
        //Log.d("view","Distance x: "+String.valueOf(getDistance(myCar.currentLat, myCar.currentLng, xCenterLat, xCenterLng)));
        //Log.d("view","Distance t: "+String.valueOf(getDistance(myCar.currentLat, myCar.currentLng, tCenterLat, tCenterLng)));
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
            return tCenterLeft + tk1*(myCar.currentLat - tCenterLat) + tk2*(myCar.currentLng - tCenterLng);
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
            return tCenterTop + tk1*(myCar.currentLng - tCenterLng) - tk2*(myCar.currentLat - tCenterLat);
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

package cn.nibius.mapv2.util;

/**
 * Created by Tinsm on 2019/3/10.
 */

public class ViewController {

    private double k1 = 960371, k2 = 1356698;
    private double centerLat = 31.027853, centerLng = 121.421893;

    public int toChangeView(Viechle myCar){
        return 1;
    }

    public double getViewLeft(Viechle myCar){
        return 940 + k1*(myCar.currentLat - centerLat) + k2*(myCar.currentLng - centerLng);
    }

    public double getViewTop(Viechle myCar){
        return 540 + k1*(myCar.currentLng - centerLng) - k2*(myCar.currentLat - centerLat);
    }

    public double tgetViewLeft(double lat, double lng){
        return 940 + k1*(lat - centerLat) + k2*(lng - centerLng);
    }

    public double tgetViewTop(double lat, double lng){
        return 540 + k1*(lng - centerLng) - k2*(lat - centerLat);
    }
}

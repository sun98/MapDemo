package cn.nibius.mapv2.util;

import android.location.Location;

/**
 * Created by Tinsmore on 2019/1/5.
 */

public class Vehicle {

    public double currentLat = 31.0278622712, currentLng = 121.4218843711, speed = 0, heading = 0;
    public int safetyMessage = 0;
    private double lastLat = -1, lastLng = -1;
    private final double timeGap = 0.1;
    private int consecutiveCount = 0;
    private static int maxCount = 5;
    private boolean updated = false;


    public void updatePosition(double lat, double lng, int newData){
        if (newData == 0) {
            consecutiveCount++;
            if (consecutiveCount > maxCount)
                consecutiveCount = maxCount;
        }
        else if (newData == 1)
            consecutiveCount = 0;
        if (consecutiveCount < maxCount && newData == 0)
            return;
        double travelDist = getDistance(lastLat, lastLng, lat, lng);
        /*
        if (travelDist > 10)
            return;
            */
        lastLat = currentLat;
        lastLng = currentLng;
        currentLat = lat;
        currentLng = lng;
        if (!updated){
            updated = true;
            return;
        }
        speed = travelDist/timeGap;
        if (travelDist >= 0.3)
            heading = getAngle(lastLng, lastLat, currentLng, currentLat);
    }

    public void updateSafety(int message){
        safetyMessage = message;
    }


    public double getDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results=new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }


    public double getAngle(double long_A, double lat_A, double long_B, double lat_B) {
        MyLatLng A = new MyLatLng(long_A, lat_A);
        MyLatLng B = new MyLatLng(long_B, lat_B);
        double dx = (B.m_RadLo - A.m_RadLo) * A.Ed;
        double dy = (B.m_RadLa - A.m_RadLa) * A.Ec;
        double angle = 0.0;
        angle = Math.atan(Math.abs(dx / dy)) * 180. / Math.PI;
        double dLo = B.m_Longitude - A.m_Longitude;
        double dLa = B.m_Latitude - A.m_Latitude;
        if (dLo > 0 && dLa <= 0) {
            angle = (90. - angle) + 90;
        } else if (dLo <= 0 && dLa < 0) {
            angle = angle + 180.;
        } else if (dLo < 0 && dLa >= 0) {
            angle = (90. - angle) + 270;
        }
        if (Double.isNaN(angle))
            angle = 0.0;
        return angle;
    }


    class MyLatLng {
        final static double Rc = 6378137;
        final static double Rj = 6356725;
        double m_LoDeg, m_LoMin, m_LoSec;
        double m_LaDeg, m_LaMin, m_LaSec;
        double m_Longitude, m_Latitude;
        double m_RadLo, m_RadLa;
        double Ec;
        double Ed;

        public MyLatLng(double longitude, double latitude) {
            m_LoDeg = (int) longitude;
            m_LoMin = (int) ((longitude - m_LoDeg) * 60);
            m_LoSec = (longitude - m_LoDeg - m_LoMin / 60.) * 3600;

            m_LaDeg = (int) latitude;
            m_LaMin = (int) ((latitude - m_LaDeg) * 60);
            m_LaSec = (latitude - m_LaDeg - m_LaMin / 60.) * 3600;

            m_Longitude = longitude;
            m_Latitude = latitude;
            m_RadLo = longitude * Math.PI / 180.;
            m_RadLa = latitude * Math.PI / 180.;
            Ec = Rj + (Rc - Rj) * (90. - m_Latitude) / 90.;
            Ed = Ec * Math.cos(m_RadLa);
        }
    }
}

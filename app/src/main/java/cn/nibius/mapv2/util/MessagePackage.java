package cn.nibius.mapv2.util;

/**
 * Created by Nibius at 2017/11/25 14:05.
 * All messages from service to MainActivity
 */

public class MessagePackage {
    private double currentLat, currentLng;   // 43, 44
    private int currentEvent;   // 47
    private boolean currentMapFlag; // 48
    private double effectiveLatS, effectiveLngS;    // 49, 50; light position
    private boolean currentTimFlag; // 56
    private double effectiveLatR, effectiveLngR;    // 57, 58; obstacle position
    private String message; // message string

    public MessagePackage() {
        this.currentLat = 0;
        this.currentLng = 0;
        this.currentEvent = -1;
        this.currentMapFlag = false;
        this.effectiveLatS = 0;
        this.effectiveLngS = 0;
        this.currentTimFlag = false;
        this.effectiveLatR = 0;
        this.effectiveLngR = 0;
        this.message = "NULL";
    }

    public MessagePackage(
            double currentLat,
            double currentLng,
            int currentEvent,
            boolean currentMapFlag,
            double effectiveLatS,
            double effectiveLngS,
            boolean currentTimFlag,
            double effectiveLatR,
            double effectiveLngR,
            String message) {
        this.currentLat = currentLat;
        this.currentLng = currentLng;
        this.currentEvent = currentEvent;
        this.currentMapFlag = currentMapFlag;
        this.effectiveLatS = effectiveLatS;
        this.effectiveLngS = effectiveLngS;
        this.currentTimFlag = currentTimFlag;
        this.effectiveLatR = effectiveLatR;
        this.effectiveLngR = effectiveLngR;
        this.message = message;
    }

    public double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public double getCurrentLng() {
        return currentLng;
    }

    public void setCurrentLng(double currentLng) {
        this.currentLng = currentLng;
    }

    public int getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(int currentEvent) {
        this.currentEvent = currentEvent;
    }

    public boolean isCurrentMapFlag() {
        return currentMapFlag;
    }

    public void setCurrentMapFlag(boolean currentMapFlag) {
        this.currentMapFlag = currentMapFlag;
    }

    public double getEffectiveLatS() {
        return effectiveLatS;
    }

    public void setEffectiveLatS(double effectiveLatS) {
        this.effectiveLatS = effectiveLatS;
    }

    public double getEffectiveLngS() {
        return effectiveLngS;
    }

    public void setEffectiveLngS(double effectiveLngS) {
        this.effectiveLngS = effectiveLngS;
    }

    public boolean isCurrentTimFlag() {
        return currentTimFlag;
    }

    public void setCurrentTimFlag(boolean currentTimFlag) {
        this.currentTimFlag = currentTimFlag;
    }

    public double getEffectiveLatR() {
        return effectiveLatR;
    }

    public void setEffectiveLatR(double effectiveLatR) {
        this.effectiveLatR = effectiveLatR;
    }

    public double getEffectiveLngR() {
        return effectiveLngR;
    }

    public void setEffectiveLngR(double effectiveLngR) {
        this.effectiveLngR = effectiveLngR;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

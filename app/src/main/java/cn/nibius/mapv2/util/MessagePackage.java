package cn.nibius.mapv2.util;

/**
 * Created by Nibius at 2017/11/25 14:05.
 * All messages from service to MainActivity
 */

public class MessagePackage {
    private double currentLat, currentLng;   // 43, 44
    private double currentSpeed;
    private double currentAngle;    // 46
    private Constant.LightEvent currentLightEvent;   // 47
    private Constant.RoadStateEvent currentRoadStateEvent;
    private Constant.V2VEvent currentV2VEvent;
    private boolean currentV2VCancel;
    private boolean currentMapFlag; // 48
    private double effectiveLatS, effectiveLngS;    // 49, 50; light position
    private boolean currentTimFlag; // 56
    private double effectiveLatR, effectiveLngR;    // 57, 58; obstacle position
    private double otherLat, otherLng;
    private String message; // message string

    private String text7100;

    public MessagePackage() {
        this.currentLat = 0;
        this.currentLng = 0;
        this.currentSpeed = 0;
        this.currentAngle = 0;
        this.currentLightEvent = Constant.LightEvent.NOLIGHT;
        this.currentRoadStateEvent = Constant.RoadStateEvent.NOROADSTATE;
        this.currentV2VEvent = Constant.V2VEvent.NOV2V;
        this.currentV2VCancel = true;
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
            double currentSpeed,
            double currentAngle,
            Constant.LightEvent currentLightEvent,
            Constant.RoadStateEvent currentRoadStateEvent,
            Constant.V2VEvent currentV2VEvent,
            boolean currentV2VCancel,
            boolean currentMapFlag,
            double effectiveLatS,
            double effectiveLngS,
            boolean currentTimFlag,
            double effectiveLatR,
            double effectiveLngR,
            String message) {
        this.currentLat = currentLat;
        this.currentLng = currentLng;
        this.currentSpeed = currentSpeed;
        this.currentAngle = currentAngle;
        this.currentLightEvent = currentLightEvent;
        this.currentRoadStateEvent = currentRoadStateEvent;
        this.currentV2VEvent = currentV2VEvent;
        this.currentV2VCancel = currentV2VCancel;
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

    public Constant.LightEvent getCurrentLightEvent() {
        return currentLightEvent;
    }

    public void setCurrentLightEvent(Constant.LightEvent currentLightEvent) {
        this.currentLightEvent = currentLightEvent;
    }

    public Constant.RoadStateEvent getCurrentRoadStateEvent() {
        return currentRoadStateEvent;
    }

    public void setCurrentRoadStateEvent(Constant.RoadStateEvent currentRoadStateEvent) {
        this.currentRoadStateEvent = currentRoadStateEvent;
    }

    public Constant.V2VEvent getCurrentV2VEvent() {
        return currentV2VEvent;
    }

    public void setCurrentV2VEvent(Constant.V2VEvent currentV2VEvent) {
        this.currentV2VEvent = currentV2VEvent;
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

    public double getCurrentAngle() {
        return currentAngle;
    }

    public void setCurrentAngle(double currentAngle) {
        this.currentAngle = currentAngle;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public boolean isCurrentV2VCancel() {
        return currentV2VCancel;
    }

    public void setCurrentV2VCancel(boolean currentV2VCancel) {
        this.currentV2VCancel = currentV2VCancel;
    }

}

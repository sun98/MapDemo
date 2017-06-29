package com.example.mapdemo;

import java.io.Serializable;

class MapData implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public int[] car1; //本车位置，三个数分别代表经度(-180,180)、纬度(-90,90)、方向(0,360)(以正北方为基准方向)
    public int[] car2; //危险车辆位置
    public int condition;   //建议车速(0,)限速(-1,)施工(-1,)急刹车(-1,)
    public int COD; //车辆距离(可通过经纬度解算)
    public int BTD; //红绿灯距离()
    public int BTS; //建议车速()
    public int SLD; //限速提示()
    public int RWD; //道路施工()

    MapData() {
        car1 = new int[3];
        car2 = new int[3];
        for (int i = 0; i < 3; ++i) {
            car1[i] = car2[i] = 0;
        }
        condition = 0;
    }

    public int getCondition() {
        return condition;

    }

    public int getCOD() {
        return COD;
    }

    public int getBTD() {
        return BTD;
    }

    public int getBTS() {
        return BTS;
    }

    public int getSLD() {
        return SLD;
    }

    public int getRWD() {
        return RWD;
    }

    public int[] getCar1() {
        return car1;

    }
}

class UIMessage implements Serializable {
    /**
     * 1.	前方路口左侧/右侧有车辆即将到达，存在碰撞危险，请小心驾驶
     * 2.	道路前方XX米处有车辆静止，请改道
     * 3.	道路前方有车辆慢行，请减速或改道
     * 4.	当前道路限速XX，请勿超速
     * 5.	前方XX米处有交通事故，请小心驾驶
     * 6.	前方XX米处有道路施工，请小心驾驶
     * 7.	前方XX米处有红绿灯路口，建议车速XXkm/h
     */
    private static final long serialVersionUID = 1L;
    private Boolean m1, m2, m3, m4, m5, m6, m7;
    private int leftOrRight, steadyCar, limitedSpeed, accidentDistance,
            constructionDistance, intersectionDistance, optimizedSpeed;

    UIMessage() {

    }
}


class VoiceData implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    VoiceData() {

    }
}
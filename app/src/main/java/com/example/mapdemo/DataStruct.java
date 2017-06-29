package com.example.mapdemo;

import java.io.Serializable;

class MapData implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public int[] car1; //����λ�ã��������ֱ������(-180,180)��γ��(-90,90)������(0,360)(��������Ϊ��׼����)
    public int[] car2; //Σ�ճ���λ��
    public int condition;   //���鳵��(0,)����(-1,)ʩ��(-1,)��ɲ��(-1,)
    public int COD; //��������(��ͨ����γ�Ƚ���)
    public int BTD; //���̵ƾ���()
    public int BTS; //���鳵��()
    public int SLD; //������ʾ()
    public int RWD; //��·ʩ��()

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
     * 1.	ǰ��·�����/�Ҳ��г����������������ײΣ�գ���С�ļ�ʻ
     * 2.	��·ǰ��XX�״��г�����ֹ����ĵ�
     * 3.	��·ǰ���г������У�����ٻ�ĵ�
     * 4.	��ǰ��·����XX��������
     * 5.	ǰ��XX�״��н�ͨ�¹ʣ���С�ļ�ʻ
     * 6.	ǰ��XX�״��е�·ʩ������С�ļ�ʻ
     * 7.	ǰ��XX�״��к��̵�·�ڣ����鳵��XXkm/h
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
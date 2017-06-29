package com.example.mapdemo;

public class AngleUtil {  
    /** 
     * ??B??��?? 
     * @param A ???????��??? 
     * @param distance   AB????????  ??��km 
     * @param angle  AB???????????????��??0~360?? 
     * @return  B????��?? 
     */  
    public static MyLatLng getMyLatLng(double long_A, double lat_A,double distance,double angle){  
    	MyLatLng A=new MyLatLng(long_A,lat_A);
        double dx = distance*1000*Math.sin(Math.toRadians(angle));  
        double dy= distance*1000*Math.cos(Math.toRadians(angle));  
        
        double bjd=(dx/A.Ed+A.m_RadLo)*180./Math.PI;  
        double bwd=(dy/A.Ec+A.m_RadLa)*180./Math.PI;  
        return new MyLatLng(bjd, bwd);  
    }  
    
    public static double getDistance(double long_A,double lat_A,double long_B,double lat_B){  
    	MyLatLng A=new MyLatLng(long_A,lat_A);  
        MyLatLng B=new MyLatLng(long_B,lat_B); 
        double dx=(B.m_RadLo-A.m_RadLo)*A.Ed;  
        double dy=(B.m_RadLa-A.m_RadLa)*A.Ec;
        double dis;
        dis = Math.sqrt(dx*dx+dy*dy);
        return dis;
    }
      
    /** 
     * ???AB????????????????? 
     * @param A  A????��?? 
     * @param B  B????��?? 
     * @return  AB??????????????????0~360?? 
     */  
    public static double getAngle(double long_A,double lat_A,double long_B,double lat_B){  
    	MyLatLng A=new MyLatLng(long_A,lat_A);  
        MyLatLng B=new MyLatLng(long_B,lat_B); 
        double dx=(B.m_RadLo-A.m_RadLo)*A.Ed;  
        double dy=(B.m_RadLa-A.m_RadLa)*A.Ec;  
        double angle=0.0;  
        angle=Math.atan(Math.abs(dx/dy))*180./Math.PI;    
        double dLo=B.m_Longitude-A.m_Longitude;  
        double dLa=B.m_Latitude-A.m_Latitude;  
        if(dLo>0&&dLa<=0){  
            angle=(90.-angle)+90;  
        }  
        else if(dLo<=0&&dLa<0){  
            angle=angle+180.;  
        }else if(dLo<0&&dLa>=0){  
            angle= (90.-angle)+270;  
        }  
        return angle;  
    }  
    static class MyLatLng {  
        final static double Rc=6378137;  
        final static double Rj=6356725;  
        double m_LoDeg,m_LoMin,m_LoSec;  
        double m_LaDeg,m_LaMin,m_LaSec;  
        double m_Longitude,m_Latitude;  
        double m_RadLo,m_RadLa;  
        double Ec;  
        double Ed;  
        public MyLatLng(double longitude,double latitude){  
            m_LoDeg=(int)longitude;  
            m_LoMin=(int)((longitude-m_LoDeg)*60);  
            m_LoSec=(longitude-m_LoDeg-m_LoMin/60.)*3600;  
              
            m_LaDeg=(int)latitude;  
            m_LaMin=(int)((latitude-m_LaDeg)*60);  
            m_LaSec=(latitude-m_LaDeg-m_LaMin/60.)*3600;  
              
            m_Longitude=longitude;  
            m_Latitude=latitude;  
            m_RadLo=longitude*Math.PI/180.;  
            m_RadLa=latitude*Math.PI/180.;  
            Ec=Rj+(Rc-Rj)*(90.-m_Latitude)/90.;  
            Ed=Ec*Math.cos(m_RadLa);  
        }  
    }  
}  
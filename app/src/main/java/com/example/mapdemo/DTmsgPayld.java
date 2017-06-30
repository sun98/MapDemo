package com.example.mapdemo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.lang.System;




public class DTmsgPayld {

	int Lat_r;	//	Remote Vehicle Latitude. -9e9 ~ 9e9
	int Long_r;	//	Remote Vehicle Longitude. -18e9 ~18e9
	int Elev_r;	//	Remote Vehicle Elevation. Encodes the Original Value.	(1dm)
	int Heading_r;	// 0..28800, step:0.0125 deg
	int Lat_l;	//	Local Vehicle Latitude. -9e9 ~ 9e9
	int Long_l;	//	Local Vehicle Longitude. -18e9 ~18e9
	int Elev_l;	//	Local Vehicle Elevation. Encodes the Original Value.	(1dm)
	int Heading_l;	// 0..28800, step:0.0125 deg
	
	int Timestamp;	// 1s, 0=1970/1/1 00:00:00
	short Event;	
					//为了不使某些数据越界引发不必要的困扰，这个类当中的值全部为INT32（也是最终客户端接收到的值）
					//经纬度采用WGS84坐标系。在地图上需要进一步的转化。
	/*
	 * switch(Event)
	 * 0 no events
	 * 1 car   
	 * 2 light
	 * 3 speed limit
	 * 4 accident 
	 */


	void from_file(String path){
		System.out.println("Building From File..");
		try {
            BufferedReader reader=null;
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String str=null;
            String[] para = {"Lag1","Lag1","Lag1"};
            while ((str=reader.readLine())!=null) {
                String strs[]=str.split("[\t]+");
                System.out.println(strs[0]);
                /*
                switch (strs[0]){
                case "Lag1": Lat_r=Integer.parseInt(strs[1]);
                			 System.out.println("Remote Latitude:" + String.valueOf(Lat_r));
                			 break;
                case "Lng1": Long_r=Integer.parseInt(strs[1]);
   			 				System.out.println("Remote Longitude:" + String.valueOf(Long_r));
   			 				break;                
                case "Elev1": Elev_r=Integer.parseInt(strs[1]);
   			 				System.out.println("Remote Altitude:" + String.valueOf(Elev_r));
   			 				break;                
            	case "Dir1": Heading_r=Integer.parseInt(strs[1]);
			 				System.out.println("Remote Heading:" + String.valueOf(Heading_r));
			 				break;            
  
                case "Lag0": Lat_l=Integer.parseInt(strs[1]);
   			 				System.out.println("Local Latitude:" + String.valueOf(Lat_l));
   			 				break;
                case "Lng0": Long_l=Integer.parseInt(strs[1]);
				 			System.out.println("Local Longitude:" + String.valueOf(Long_l));
				 			break;                
                case "Elev0": Elev_l=Integer.parseInt(strs[1]);
				 			System.out.println("Local Altitude:" + String.valueOf(Elev_l));
				 			break;                
				case "Dir0": Heading_l=Integer.parseInt(strs[1]);
							System.out.println("Local Heading:" + String.valueOf(Heading_l));
							break;                   
				case "Stamp": Timestamp=Integer.parseInt(strs[1]);			
							System.out.println("TimeStamp:" + String.valueOf(Timestamp));
							break;
				case "Msg": Event=Short.parseShort(strs[1]);
							System.out.println("Event Code:" + String.valueOf(Event));
							break;
				default: 	System.out.println("Match not Found.");
							break;
                }
                
                */
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	/*
	public static void main(String[] args) {
		DTmsgPayld message = new DTmsgPayld();
		System.out.println("Running.");
		String path="D:/My Documents/Documents/Book1.txt";
		message.from_file(path);
		System.out.println(message.Lat_r);
	}
	*/
}




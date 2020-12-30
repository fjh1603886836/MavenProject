package com.enjoytechsz.mavenproject.util;

/**
 * ============================
 * 作    者：Fjh
 * 版    本：1.0
 * 创建日期：2020/12/24 0024
 * 描   述：
 * 修订历史：
 * ============================
 */
public class GeographicUtil {


    public static String DDtoDMS(Double dd) {
        // Decimal Degrees to Degrees,Minutes,Seconds conversion
        //example 123.456
        //d = int(123.456°) = 123°
        //m = int((123.456° - 123°) × 60) = 27'
        //s = (123.456° - 123° - 27'/60) × 3600 = 21.6"
        //123.456°
        //= 123° 27' 21.6"

        String[] array = dd.toString().split("[.]");
        String degrees=array[0];//得到度

        Double m=Double.parseDouble("0."+array[1])*60;
        String[] array1=m.toString().split("[.]");
        String minutes=array1[0];//得到分

        Double s=Double.parseDouble("0."+array1[1])*60;
        String[] array2=s.toString().split("[.]");
        String seconds=array2[0];//得到秒

        return degrees+"°"+minutes+'"'+seconds+"'";
    }

}

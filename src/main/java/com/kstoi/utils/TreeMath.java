package com.kstoi.utils;

public class TreeMath {
    private TreeMath(){}
    public static Double entropy(double p){
        if (p <= 0d) return 0d;
        return -p * Math.log(p) / Math.log(2d);
    }
    public static boolean isNumeric(String number){
        try {
            Double.parseDouble(number);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}

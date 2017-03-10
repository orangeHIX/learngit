package com.company;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static com.company.ROC.getROCCurve;

/**
 * Created by hzhuangyixuan on 2017/2/27.
 */
public class AUC {

    static class Point{
        double x;
        double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /**Compute Area Under the Curve (AUC) using the trapezoidal rule
     * @param x x coordinates
     * @param y y coordinates
     */
    public static double getAUC(List<Double> x, List<Double> y){
        if(x.size() != y.size())
            throw new IllegalArgumentException("x's length is not consistent of y's");
        List<Point> list = new ArrayList<>();
        for( int i = 0; i < x.size(); i++){
            list.add(new Point(x.get(i),y.get(i)));
        }
        list.sort((p1,p2)->{
            double diff = p1.x-p2.x;
            if(diff >0) return 1;
            else if (diff < 0) return -1;
            else return 0;
        });

        double area = 0;
        for(int i = 1; i < list.size(); i++){
            double width = list.get(i).x - list.get(i-1).x;
            if(width!= 0){
                area += width*list.get(i).y;
            }
        }
        return area;
    }

    public static void main(String[] args){
        List<Integer> y = Arrays.asList(1,1,2,2);
        List<Double> pred = Arrays.asList(0.1d, 0.4d, 0.35d, 0.8d);
        List<ROC.ROCCurveRecord> list =  ROC.getROCCurve(y,pred,2,1,false);
        List<Double> fpr = list.stream().map(o->o.falsePositiveRate).collect(Collectors.toList());
        List<Double> tpr = list.stream().map(o->o.truePositiveRate).collect(Collectors.toList());
        System.out.println(getAUC(fpr, tpr));

    }
}

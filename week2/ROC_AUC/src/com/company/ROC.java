package com.company;

import java.awt.geom.Arc2D;
import java.util.*;

/**
 * Created by hzhuangyixuan on 2017/2/27.
 */
public class ROC {

    static class ROCCurveRecord{
        double falsePositiveRate;
        double truePositiveRate;
        double threshold;

        public ROCCurveRecord(double falsePositiveRate, double truePositiveRate, double threshold) {
            this.falsePositiveRate = falsePositiveRate;
            this.truePositiveRate = truePositiveRate;
            this.threshold = threshold;
        }
    }

    /**
     * Compute precision-recall pairs for different probability thresholds

     <p>Note: this implementation is restricted to the binary classification task.</p>

     <p>The precision is the ratio ``tp / (tp + fp)`` where ``tp`` is the number of
     true positives and ``fp`` the number of false positives. The precision is
     intuitively the ability of the classifier not to label as positive a sample
     that is negative.</p>

     <p>The recall is the ratio ``tp / (tp + fn)`` where ``tp`` is the number of
     true positives and ``fn`` the number of false negatives. The recall is
     intuitively the ability of the classifier to find all the positive samples.</p>

     <p>The last precision and recall values are 1. and 0. respectively and do not
     have a corresponding threshold.  This ensures that the graph starts on the
     x axis.</p>
     * @param yTrue True binary labels in range {0, 1} or {-1, 1}
     * @param yScore Target scores, can either be probability estimates of the positive
     *               class, confidence values, or non-thresholded measure of decisions
     * @param dropIntermediate Whether to drop some suboptimal thresholds which would not appear
     *                         on a plotted ROC curve. This is useful in order to create lighter
     *                         ROC curves.
     * @return ROCCurve consists of falsePositiveRate, truePositiveRate, and threshold*/
    public static List<ROCCurveRecord> getROCCurve(List<Integer> yTrue, List<Double> yScore,
                                       int posLabel, int negLabel, boolean dropIntermediate){

        List<PositiveCountRecord> records = binaryClfCurve(
                yTrue, yScore, posLabel);

        // TODO Attempt to drop thresholds corresponding to points in between and collinear with other points.
        // These are always suboptimal and do not appear on a plotted ROC curve (and thus do not affect the AUC).

        //Add an extra threshold position if necessary
        if( records.size() == 0 || records.get(0).fps != 0){
            records.add(0, new PositiveCountRecord(0,0,records.get(0).threshold+1));
        }

        List<ROCCurveRecord> curveRecords = new ArrayList<>();
        int ps = records.get(records.size()-1).tps; //The total number of positive samples
        int ns = records.get(records.size()-1).fps; //The total number of negative samples

        if( ns <= 0) {
            System.out.println("No negative samples in y_true, " +
                    "false positive value should be meaningless");
            return null;
        }else if(ps <= 0){
            System.out.println("No positive samples in y_true, "+
                    "true positive value should be meaningless");
            return null;
        }else {
            for(PositiveCountRecord rec : records){
                curveRecords.add(new ROCCurveRecord(rec.fps/(double)ps,
                        rec.tps/(double)ns, rec.threshold));
            }
        }
        return curveRecords;
    }



    static class Sample{
        Integer yTrue;
        Double yScore;

        public Sample(Integer yTrue, Double yScore) {
            this.yTrue = yTrue;
            this.yScore = yScore;
        }
    }

    static class PositiveCountRecord{
        /** An increasing count of true positives, at index i being the number
         of positive samples assigned a score >= threshold*/
        int tps;
        /**A count of false positives, at index i being the number of negative
         samples assigned a score >= threshold.*/
        int fps;
        /**decreasing score value*/
        double threshold;

        public PositiveCountRecord(int tps, int fps, double threshold) {
            this.tps = tps;
            this.fps = fps;
            this.threshold = threshold;
        }
    }

    /**Calculate true and false positives per binary classification threshold*/
    private static List<PositiveCountRecord> binaryClfCurve(List<Integer> yTrue, List<Double> yScore,
                                                            int posLabel) {
        if(yTrue.size() != yScore.size())
            throw new IllegalArgumentException("yTrue's length is not consistent of yScore's");

        ArrayList<Sample> list = new ArrayList<>();
        for(int i = 0; i<yTrue.size(); i++){
            list.add(new Sample(yTrue.get(i), yScore.get(i)));
        }

        // sort scores and corresponding truth values(desc)
        list.sort((o1, o2) -> {
            double diff = o1.yScore-o2.yScore;
            if( diff < 0) return 1;
            else if( diff > 0 ) return -1;
            else return 0;
        });

        /* y_score typically has many tied values. Here we extract
        * the indices associated with the distinct values. We also
        * concatenate a value for the end of the curve.
        * */
        List<Integer> distinctValueIndices = new ArrayList<>();
        for(int i = 1; i < list.size(); i++){
            if(list.get(i).yScore != list.get(i-1).yScore){
                distinctValueIndices.add(i-1);
            }
        }
        distinctValueIndices.add(list.size()-1);

        // accumulate the true positives with decreasing threshold
        List<PositiveCountRecord> records = new ArrayList<>();
        int cumsum = 0;
        int j = 0;
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).yTrue == posLabel){
                cumsum++;
            }
            if(i == distinctValueIndices.get(j)){
                records.add(new PositiveCountRecord(cumsum,1+i-cumsum, list.get(i).yScore));
                j++;
            }
        }
        return records;
    }


    public static void main(String[] args){
        List<Integer> y = new ArrayList<>();
        y.addAll(Arrays.asList(1,1,2,2));
        List<Double> scores = new ArrayList<>();
        scores.addAll(Arrays.asList(0.1d, 0.4d, 0.35d, 0.8d));
        List<ROCCurveRecord> curve = getROCCurve(y, scores,2,1,false);

        for(ROCCurveRecord rec : curve){
            System.out.println("fpr:"+rec.falsePositiveRate
                    +" tpr:"+rec.truePositiveRate
                    +" threshold:"+rec.threshold);
        }
    }
}

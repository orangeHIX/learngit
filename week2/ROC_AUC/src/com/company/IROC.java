package com.company;

import java.util.List;

/**
 * Created by hzhuangyixuan on 2017/2/28.
 */
public interface IROC {
    public static List<ROC.ROCCurveRecord> getROCCurve(List<Integer> yTrue, List<Double> yScore,
                                                       int posLabel, int negLabel, boolean dropIntermediate)
}

package org.lenskit.mooc.nonpers;

import org.lenskit.api.Result;
import org.lenskit.results.Results;

import java.util.*;

/**
 * Created by hzhuangyixuan on 2017/2/26.
 */
public class test {
    public static void main(String[] args){
        List<Result> results = new ArrayList<>();
        results.add(Results.create(1,4));
        results.add(Results.create(2,-1));
        results.add(Results.create(3,7));
        Collections.sort(results,new Comparator<Result>() {
            @Override
            public int compare(Result o1, Result o2) {
                double diff = o1.getScore()-o2.getScore();
                if(diff > 0) return -1;
                else if (diff < 0) return 1;
                else return 0;
            }
        });
        System.out.print(results);
    }
}

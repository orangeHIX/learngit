package org.lenskit.mooc.hybrid;

import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.inject.Transient;
import org.lenskit.util.ProgressLogger;
import org.lenskit.util.math.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Trainer that builds logistic models.
 */
public class LogisticModelProvider implements Provider<LogisticModel> {
    private static final Logger logger = LoggerFactory.getLogger(LogisticModelProvider.class);
    private static final double LEARNING_RATE = 0.00005;
    private static final int ITERATION_COUNT = 100;

    private final LogisticTrainingSplit dataSplit;
    private final BiasModel baseline;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;
    private final int parameterCount;
    private final Random random;

    @Inject
    public LogisticModelProvider(@Transient LogisticTrainingSplit split,
                                 @Transient UserBiasModel bias,
                                 @Transient RecommenderList recs,
                                 @Transient RatingSummary rs,
                                 @Transient Random rng) {
        dataSplit = split;
        baseline = bias;
        recommenders = recs;
        ratingSummary = rs;
        parameterCount = 1 + recommenders.getRecommenderCount() + 1;
        random = rng;
    }

    @Override
    public LogisticModel get() {
        List<ItemScorer> scorers = recommenders.getItemScorers();
        double intercept = 0;
        double[] params = new double[parameterCount];

        LogisticModel current = LogisticModel.create(intercept, params);
        List<Rating> ratingList = dataSplit.getTuneRatings();
        // Implement model training


        // cache the scores from each component recommender instead of recomputing them for each update
        Map<String, double[]> map = new HashMap<>();
        for(Rating r : ratingList) {
            double[] x = new double[parameterCount];
            double bias = baseline.getIntercept()
                    + baseline.getUserBias(r.getUserId())
                    + baseline.getItemBias(r.getItemId());
            double populairty = Math.log10(ratingSummary.getItemRatingCount(r.getItemId()));
            double[] x3_n = recommenders.getItemScorers().stream()
                    .mapToDouble(scorer-> {
                double score = 0;
                Result reuslt = scorer.score(r.getUserId(), r.getItemId());
                if (reuslt != null && reuslt.hasScore()) {
                    score = reuslt.getScore() - bias;
                }
                return score;
            }).toArray();
            x[0] = bias;
            x[1] =populairty;
            System.arraycopy(x3_n,0, x, 2, x3_n.length);
            map.put(getRatingKey(r), x);
        }

//        int count = 0;
//        double[] L = new double[ITERATION_COUNT];
        for(int i = 0; i < ITERATION_COUNT; i++){
            Collections.shuffle(ratingList);
            for(Rating r : ratingList){
                double[] x = map.get(getRatingKey(r));
                double ratingValue = (r.getValue() > 0 ? 1 : 0);

                double eva = current.evaluate(1, x);
                double error = ratingValue - eva ;
                intercept += LEARNING_RATE * error;
                for(int j = 0; j < parameterCount; j ++){
                    params[j] += LEARNING_RATE * error * x[j];
                }

//                L[i] += ratingValue * Math.log(eva) + (1-ratingValue)*Math.log(1-eva);
//                if(count == 0 && Double.isNaN(L[i])){
//                    System.out.println("debug r:"+r+" eva:" +eva +" r_value: " + r.getValue() );
//                    System.out.println("debug L["+i+"]:" + L[i]);
//                    count++;
//                }
                current = LogisticModel.create(intercept, params);
            }
//            if(Double.isNaN(L[i])){
//                break;
//            }
        }
//        for(int i = 0; i < ITERATION_COUNT; i++) {
//            System.out.println("iter " + i + " L:" +L[i]);
//        }
        return current;
    }

    private static String getRatingKey(Rating r){
        return ""+ r.getUserId() + "_" + r.getItemId();
    }

}

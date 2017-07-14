package org.lenskit.mooc.hybrid;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.bias.BiasModel;
import org.lenskit.bias.UserBiasModel;
import org.lenskit.data.ratings.RatingSummary;
import org.lenskit.results.Results;
import org.lenskit.util.collections.LongUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Item scorer that does a logistic blend of a subsidiary item scorer and popularity.  It tries to predict
 * whether a user has rated a particular item.
 */
public class LogisticItemScorer extends AbstractItemScorer {
    private final LogisticModel logisticModel;
    private final BiasModel biasModel;
    private final RecommenderList recommenders;
    private final RatingSummary ratingSummary;

    @Inject
    public LogisticItemScorer(LogisticModel model, UserBiasModel bias, RecommenderList recs, RatingSummary rs) {
        logisticModel = model;
        biasModel = bias;
        recommenders = recs;
        ratingSummary = rs;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        List<Result> results = new ArrayList<>();

        // Implement item scorer
        for(long item : items){
            double[] x = new double[2+recommenders.getRecommenderCount()];
            double bias = biasModel.getIntercept() + biasModel.getItemBias(item) + biasModel.getUserBias(user);
            double populairty = Math.log10(ratingSummary.getItemRatingCount(item));
            double[] x3_n = recommenders.getItemScorers().stream()
                    .mapToDouble(scorer-> {
                        double score = 0;
                        Result reuslt = scorer.score(user, item);
                        if (reuslt != null && reuslt.hasScore()) {
                            score = reuslt.getScore() - bias;
                        }
                        return score;
                    }).toArray();
            x[0] = bias;
            x[1] =populairty;
            System.arraycopy(x3_n,0, x, 2, x3_n.length);
            results.add(Results.create(item, logisticModel.evaluate(1, x)));
        }
        return Results.newResultMap(results);
    }
}

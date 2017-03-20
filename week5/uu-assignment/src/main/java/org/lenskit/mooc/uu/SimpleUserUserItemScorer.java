package org.lenskit.mooc.uu;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.IdBox;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.math.Scalars;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;
import java.util.regex.Matcher;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    /**
     * Instantiate a new user-user item scorer.
     * @param dao The data access object.
     */
    @Inject
    public SimpleUserUserItemScorer(DataAccessObject dao) {
        this.dao = dao;
        neighborhoodSize = 30;
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        // TODO Score the items for the user with user-user CF

        Long2DoubleOpenHashMap u = getUserRatingVector(user);
        double uMean = Vectors.mean(u);
        for(Long i : u.keySet()){
            u.addTo(i, -uMean);
        }
        double uEuclideanNorm = Vectors.euclideanNorm(u);

        //Long2DoubleOpenHashMap uItemPrediction = new Long2DoubleOpenHashMap(items.size());
        List<Result> results = new ArrayList<>();
        for(long item : items) {
            List<Rating> history = dao.query(Rating.class)
                    .withAttribute(CommonAttributes.ITEM_ID, item).get();

            if(history.size()<2) continue;  //Refuse to score items if there are not at least 2 neighbors to contribute to the item’s score

            List<UserCos> list = new ArrayList<>();
            for(Rating r : history) {
                Long2DoubleOpenHashMap v = getUserRatingVector(r.getUserId());
                // Use mean-centering to normalize ratings for scoring
                double userMean = Vectors.mean(v);
                for(Long i : v.keySet()){
                    v.addTo(i, -userMean);
                }
                double cos = Vectors.dotProduct(u,v)/(uEuclideanNorm * Vectors.euclideanNorm(v));
                list.add(new UserCos(r.getUserId(), r.getValue(), userMean, cos));
            }

            /*For each item’s score, use the 30 most similar users who have rated the item and
              whose similarity to the target user is positive*/
            Collections.sort(list, new Comparator<UserCos>() {
                @Override
                public int compare(UserCos o1, UserCos o2) {
                    if(o1.cos-o2.cos > 0) return -1;
                    else if(o1.cos-o2.cos<0) return 1;
                    else return 0;
                }
            });
            List<UserCos> top30 = list.subList(0, Math.min(30, list.size()));

            //cosine similarity
            double numerator = 0;
            double denominator = 0;
            for(UserCos v : top30){
                numerator += v.cos*(v.rating-v.mean);
                denominator += Math.abs(v.cos);
            }
            //uItemPrediction.put(item, uMean + numerator/denominator);
            results.add(Results.create(item, uMean + numerator/denominator));
        }

        return Results.newResultMap(results);
    }

    static class UserCos{
        long user;
        double rating;
        double cos;
        double mean;

        public UserCos(long user, double rating, double mean, double cos) {
            this.user = user;
            this.rating = rating;
            this.mean = mean;
            this.cos = cos;
        }
    }


    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector, mapping item IDs to the user's rating
     *         for that item.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;
    }

}

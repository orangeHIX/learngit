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

        Long2DoubleOpenHashMap ratings = getUserRatingVector(user);
        double sum = 0;
        for(Map.Entry<Long, Double> entry : ratings.entrySet()){
            sum += entry.getValue();
        }
        double mean = sum / ratings.size();
    }

    static class UserCos{
        long user;
        double cos;
        double mean;

        public UserCos(long user, double mean, double cos) {
            this.user = user;
            this.mean = mean;
            this.cos = cos;
        }
    }

    private void getSimilarUser(Long2DoubleOpenHashMap u, Collection<Long> items){
        double uEuclideanNorm = Vectors.euclideanNorm(u);

        for(Long item : items) {
            List<IdBox<List<Rating>>> history = dao.query(Rating.class)
                    .withAttribute(CommonAttributes.ITEM_ID, item).groupBy(CommonAttributes.USER_ID).get();

            List<UserCos> list = new ArrayList<>();
            for(IdBox<List<Rating>> idBox : history) {
                Long2DoubleOpenHashMap v = getUserRatingVector(idBox.getId());
                double userMean = Vectors.mean(v);
                double cos = Vectors.dotProduct(u,v)/(uEuclideanNorm * Vectors.euclideanNorm(v));
                list.add(new UserCos(idBox.getId(), userMean, cos));
            }
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

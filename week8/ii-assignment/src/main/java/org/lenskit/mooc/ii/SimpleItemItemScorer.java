package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, DataAccessObject dao) {
        model = m;
        this.dao = dao;
        neighborhoodSize = 20;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);

        // Normalize the user's ratings by subtracting the item mean from each one.
        // Mean center the ratings.
        for (Map.Entry<Long, Double> entry : ratings.entrySet()) {
            entry.setValue(entry.getValue() - itemMeans.get(entry.getKey()));
        }

        List<Result> results = new ArrayList<>();

        for (long item: items ) {
            // Compute the user's score for each item, add it to results
            Long2DoubleMap nei = model.getNeighbors(item);
            double numerator = 0;
            double denominator = 0;
            List<Map.Entry<Long,Double>> candidate = new ArrayList<>();
            for(Map.Entry<Long,Double> entry : nei.entrySet()){
                if(ratings.containsKey(entry.getKey()))
                    candidate.add(entry);
            }
            if(candidate.size() > 20) {
                Collections.sort(candidate, new Comparator<Map.Entry<Long, Double>>() {
                    @Override
                    public int compare(Map.Entry<Long, Double> o1, Map.Entry<Long, Double> o2) {
                        if (o1.getValue() - o2.getValue() < 0) return 1;
                        else if (o1.getValue() - o2.getValue() > 0) return -1;
                        else return 0;
                    }
                });
                candidate = candidate.subList(0, 20);
            }
            for(Map.Entry<Long,Double> entry : candidate) {
                numerator += ratings.get(entry.getKey()) * entry.getValue();
                denominator += Math.abs(entry.getValue());
            }
            double score = itemMeans.get(item) + numerator/denominator;
            results.add(Results.create(item, score));
        }
        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
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

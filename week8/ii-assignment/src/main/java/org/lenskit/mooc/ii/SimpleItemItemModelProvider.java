package org.lenskit.mooc.ii;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.inject.Transient;
import org.lenskit.util.IdBox;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.io.ObjectStream;
import org.lenskit.util.math.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemModelProvider implements Provider<SimpleItemItemModel> {
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelProvider.class);

    private final DataAccessObject dao;

    /**
     * Construct the model provider.
     *
     * @param dao The data access object.
     */
    @Inject
    public SimpleItemItemModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
    }

    /**
     * Construct the item-item model.
     *
     * @return The item-item model.
     */
    @Override
    public SimpleItemItemModel get() {
        Map<Long, Long2DoubleMap> itemVectors = Maps.newHashMap();
        Long2DoubleMap itemMeans = new Long2DoubleOpenHashMap();

        try (ObjectStream<IdBox<List<Rating>>> stream = dao.query(Rating.class)
                .groupBy(CommonAttributes.ITEM_ID)
                .stream()) {
            for (IdBox<List<Rating>> item : stream) {
                long itemId = item.getId();
                List<Rating> itemRatings = item.getValue();
                Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap(Ratings.itemRatingVector(itemRatings));

                // Compute and store the item's mean.
                double mean = Vectors.mean(ratings);
                itemMeans.put(itemId, mean);

                // Mean center the ratings.
                for (Map.Entry<Long, Double> entry : ratings.entrySet()) {
                    entry.setValue(entry.getValue() - mean);
                }

                itemVectors.put(itemId, LongUtils.frozenMap(ratings));
            }
        }

        // Map items to vectors (maps) of item similarities.
        Map<Long, Long2DoubleMap> itemSimilarities = Maps.newHashMap();

        // Compute the similarities between each pair of items
        for (Map.Entry<Long, Long2DoubleMap> item1 : itemVectors.entrySet()) {
            for (Map.Entry<Long, Long2DoubleMap> item2 : itemVectors.entrySet()) {
                long i1 = item1.getKey();
                long i2 = item2.getKey();
                double similarity = 0;
                if (Long.compare(i1, i2) == 0) continue;
                double numerator = Vectors.dotProduct(item1.getValue(), item2.getValue());
                if (numerator <= 0) continue;   // Ignore nonpositive similarities
                double d1 = Vectors.euclideanNorm(item1.getValue());
                double d2 = Vectors.euclideanNorm(item2.getValue());
                similarity = numerator / (d1 * d2);

                if (!itemSimilarities.containsKey(i1)) {
                    itemSimilarities.put(i1, new Long2DoubleOpenHashMap());
                }
                itemSimilarities.get(i1).put(i2, similarity);
            }
        }

        return new SimpleItemItemModel(LongUtils.frozenMap(itemMeans), itemSimilarities);
    }
}

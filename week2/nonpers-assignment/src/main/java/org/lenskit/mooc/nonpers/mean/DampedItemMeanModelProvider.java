package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lenskit.baseline.MeanDamping;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * Provider class that builds the mean rating item scorer, computing damped item means from the
 * ratings in the DAO.
 */
public class DampedItemMeanModelProvider implements Provider<ItemMeanModel> {

    public static double ALPHA = 5d;
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(DampedItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;
    /**
     * The damping factor.
     */
    private final double damping;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     *            annotation on this parameter means that the DAO will be used to build the model, but the
     *            model will <strong>not</strong> retain a reference to the DAO.  This is standard procedure
     *            for LensKit models.
     * @param damping The damping factor for Bayesian damping.  This is number of fake global-mean ratings to
     *                assume.  It is provided as a parameter so that it can be reconfigured.  See the file
     *                {@code damped-mean.groovy} for how it is used.
     */
    @Inject
    public DampedItemMeanModelProvider(@Transient DataAccessObject dao,
                                       @MeanDamping double damping) {
        this.dao = dao;
        this.damping = damping;
    }



    /**
     * Construct an item mean model.
     *
     * <p>The {@link Provider#get()} method constructs whatever object the provider class is intended to build.</p>
     *
     * @return The item mean model with mean ratings for all items.
     */
    @Override
    public ItemMeanModel get() {
        Long2DoubleOpenHashMap sumMap = new Long2DoubleOpenHashMap();
        Long2IntOpenHashMap countMap = new Long2IntOpenHashMap();
        double globalValue = 0d;
        int globalCount = 0;
        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            for (Rating r: ratings) {
                // this loop will run once for each rating in the data set
                long itemId = r.getItemId();
                if( !countMap.containsKey(itemId) ){
                    countMap.put(itemId,1);
                    sumMap.put(itemId,r.getValue());
                }else{
                    double origSum = sumMap.get(itemId);
                    int origCount = countMap.get(itemId);
                    sumMap.put(itemId, origSum+r.getValue());
                    countMap.put(itemId, origCount + 1);
                }
                globalValue += r.getValue();
                globalCount++;
            }
        }

        Long2DoubleOpenHashMap dampedMeans = new Long2DoubleOpenHashMap();

        double globalMean = globalValue/globalCount;
        for(Map.Entry<Long,Integer> entry : countMap.entrySet()){
            long itemId = entry.getKey();
            dampedMeans.put(itemId, (sumMap.get(itemId)+ALPHA*globalMean)/(entry.getValue()+ALPHA));
        }
        logger.debug("2959 damped mean:" + dampedMeans.get(2959)+
                " value:"+sumMap.get(2959)+" count:"+countMap.get(2959));
        logger.debug("1203 damped mean:" + dampedMeans.get(1203)+
                " value:"+sumMap.get(1203)+" count:"+countMap.get(1203));

        logger.info("computed mean ratings for {} items", dampedMeans.size());
        return new ItemMeanModel(dampedMeans);
    }
}

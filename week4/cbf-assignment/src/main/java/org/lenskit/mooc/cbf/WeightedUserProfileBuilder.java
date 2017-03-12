package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String, Double> profile = new HashMap<>();

        // Normalize the user's ratings
        // Build the user's weighted profile

        double avgRating = 0d;
        for(Rating r : ratings){
            avgRating += r.getValue();
        }

        avgRating = avgRating/ratings.size();
        for (Rating r : ratings) {
            // Get this item's vector and add it to the user's profile
            Map<String, Double> itemVector = model.getItemVector(r.getItemId());
            for (Map.Entry<String, Double> entry : itemVector.entrySet()) {
                String tag = entry.getKey();
                if (!profile.containsKey(tag)) {
                    profile.put(tag, entry.getValue()*(r.getValue()-avgRating));
                } else {
                    profile.put(tag, entry.getValue()*(r.getValue()-avgRating) + profile.get(tag));
                }
            }
        }

        // The profile is accumulated, return it.
        return profile;
    }
}

package com.bookverse.reviewservice.service.strategy;

import com.bookverse.reviewservice.model.Review;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("averageRatingStrategy")
public class AverageRatingStrategy implements RatingStrategy {

    @Override
    public double calculateRating(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }
}

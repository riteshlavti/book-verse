package com.bookverse.reviewservice.service.strategy;

import com.bookverse.reviewservice.model.Review;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component("weightedRatingStrategy")
public class WeightedRatingStrategy implements RatingStrategy{

    @Override
    public double calculateRating(List<Review> reviews){
        return reviews.stream()
                .mapToDouble(r -> r.getRating() * getWeight(r))
                .sum() / reviews.stream().mapToDouble(this::getWeight).sum();
    }

    public double getWeight(Review review){
        long daysOld = ChronoUnit.DAYS.between(review.getCreatedAt(), LocalDateTime.now());
        log.info("Review ID: {}, Days Old: {}, Weight: {}", review.getReviewId(), daysOld, 1.0/(1+daysOld * 0.01));
        return 1.0/(1+daysOld * 0.01); // Decrease weight by 1% for each day old
    }
}

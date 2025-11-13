package com.bookverse.reviewservice.service.strategy;

import com.bookverse.reviewservice.model.Review;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface RatingStrategy {
    double calculateRating(List<Review> reviews);
}

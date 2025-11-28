package com.bookverse.reviewservice.service;

import com.bookverse.reviewservice.exception.ExternalServiceException;
import com.bookverse.reviewservice.feign.BookServiceClient;
import com.bookverse.reviewservice.repository.ReviewRepository;
import com.bookverse.reviewservice.service.strategy.RatingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AnalyticsService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookServiceClient bookServiceClient;
    private final Map<String, RatingStrategy> ratingStrategies;

    @Autowired
    public AnalyticsService(Map<String, RatingStrategy> ratingStrategies) {
        this.ratingStrategies = ratingStrategies;
    }

    public double getBookRating(int bookId, String strategyType){
        try {
            bookServiceClient.getBookById(bookId);
        }catch (Exception e){
            throw new ExternalServiceException("Book not found with id: " + bookId +". "+ e.getMessage(), e);
        }
        log.info("Strategy selected: {}", strategyType);
        RatingStrategy strategy = ratingStrategies.get(strategyType);
        return strategy.calculateRating(reviewRepository.findByBookId(bookId));
    }
}

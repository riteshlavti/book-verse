package com.bookverse.reviewservice.service;

import com.bookverse.reviewservice.dto.BookResponseDto;
import com.bookverse.reviewservice.exception.ExternalServiceException;
import com.bookverse.reviewservice.feign.BookServiceClient;
import com.bookverse.reviewservice.model.Review;
import com.bookverse.reviewservice.repository.ReviewRepository;
import com.bookverse.reviewservice.service.strategy.RatingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookServiceClient bookServiceClient;

    @Mock
    private RatingStrategy averageRatingStrategy;

    @Mock
    private RatingStrategy weightedRatingStrategy;

    private AnalyticsService analyticsService;

    private List<Review> reviews;
    private BookResponseDto bookResponseDto;

    @BeforeEach
    void setUp() {
        // Setup strategies map
        Map<String, RatingStrategy> ratingStrategies = new HashMap<>();
        ratingStrategies.put("averageRatingStrategy", averageRatingStrategy);
        ratingStrategies.put("weightedRatingStrategy", weightedRatingStrategy);

        // Re-initialize the service with the strategy map
        analyticsService = new AnalyticsService(ratingStrategies);
        ReflectionTestUtils.setField(analyticsService, "reviewRepository", reviewRepository);
        ReflectionTestUtils.setField(analyticsService, "bookServiceClient", bookServiceClient);

        // Setup test data
        reviews = new ArrayList<>();

        Review review1 = new Review();
        review1.setReviewId(1);
        review1.setBookId(1);
        review1.setReviewer("user1");
        review1.setRating(4.5);
        review1.setReviewText("Great book!");
        review1.setCreatedAt(LocalDateTime.now());

        Review review2 = new Review();
        review2.setReviewId(2);
        review2.setBookId(1);
        review2.setReviewer("user2");
        review2.setRating(5.0);
        review2.setReviewText("Excellent!");
        review2.setCreatedAt(LocalDateTime.now());

        Review review3 = new Review();
        review3.setReviewId(3);
        review3.setBookId(1);
        review3.setReviewer("user3");
        review3.setRating(3.5);
        review3.setReviewText("Good");
        review3.setCreatedAt(LocalDateTime.now());

        reviews.add(review1);
        reviews.add(review2);
        reviews.add(review3);

        bookResponseDto = BookResponseDto.builder()
                .bookId(1)
                .title("Test Book")
                .build();
    }

    // ========== Positive Test Cases ==========

    @Test
    void getBookRating_ShouldReturnAverageRating_WhenUsingAverageStrategy() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);
        when(averageRatingStrategy.calculateRating(reviews)).thenReturn(4.33);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(4.33);

        verify(bookServiceClient, times(1)).getBookById(1);
        verify(reviewRepository, times(1)).findByBookId(1);
        verify(averageRatingStrategy, times(1)).calculateRating(reviews);
    }

    @Test
    void getBookRating_ShouldReturnWeightedRating_WhenUsingWeightedStrategy() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);
        when(weightedRatingStrategy.calculateRating(reviews)).thenReturn(4.5);

        double result = analyticsService.getBookRating(1, "weightedRatingStrategy");

        assertThat(result).isEqualTo(4.5);

        verify(bookServiceClient, times(1)).getBookById(1);
        verify(reviewRepository, times(1)).findByBookId(1);
        verify(weightedRatingStrategy, times(1)).calculateRating(reviews);
    }

    @Test
    void getBookRating_ShouldReturnZero_WhenNoReviews() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(Collections.emptyList());
        when(averageRatingStrategy.calculateRating(Collections.emptyList())).thenReturn(0.0);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(0.0);

        verify(reviewRepository, times(1)).findByBookId(1);
        verify(averageRatingStrategy, times(1)).calculateRating(Collections.emptyList());
    }

    @Test
    void getBookRating_ShouldReturnMaxRating_WhenAllPerfectScores() {
        List<Review> perfectReviews = new ArrayList<>();
        Review perfect1 = new Review();
        perfect1.setRating(5.0);
        Review perfect2 = new Review();
        perfect2.setRating(5.0);
        perfectReviews.add(perfect1);
        perfectReviews.add(perfect2);

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(perfectReviews);
        when(averageRatingStrategy.calculateRating(perfectReviews)).thenReturn(5.0);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(5.0);
    }

    @Test
    void getBookRating_ShouldReturnMinRating_WhenAllLowestScores() {
        List<Review> lowReviews = new ArrayList<>();
        Review low1 = new Review();
        low1.setRating(1.0);
        Review low2 = new Review();
        low2.setRating(1.0);
        lowReviews.add(low1);
        lowReviews.add(low2);

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(lowReviews);
        when(averageRatingStrategy.calculateRating(lowReviews)).thenReturn(1.0);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(1.0);
    }

    @Test
    void getBookRating_ShouldHandleSingleReview() {
        List<Review> singleReview = Collections.singletonList(reviews.get(0));

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(singleReview);
        when(averageRatingStrategy.calculateRating(singleReview)).thenReturn(4.5);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(4.5);

        verify(averageRatingStrategy, times(1)).calculateRating(singleReview);
    }

    // ========== Negative Test Cases ==========

    @Test
    void getBookRating_ShouldThrowException_WhenBookNotFound() {
        when(bookServiceClient.getBookById(999)).thenThrow(new RuntimeException("Book not found"));

        assertThatThrownBy(() -> analyticsService.getBookRating(999, "averageRatingStrategy"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");

        verify(bookServiceClient, times(1)).getBookById(999);
        verify(reviewRepository, never()).findByBookId(anyInt());
    }

    @Test
    void getBookRating_ShouldThrowException_WhenInvalidStrategy() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);

        assertThatThrownBy(() -> analyticsService.getBookRating(1, "invalidStrategy"))
                .isInstanceOf(NullPointerException.class);

        verify(bookServiceClient, times(1)).getBookById(1);
        verify(reviewRepository, times(1)).findByBookId(1);
    }

    @Test
    void getBookRating_ShouldThrowException_WhenStrategyNotInMap() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);

        assertThatThrownBy(() -> analyticsService.getBookRating(1, "nonExistentStrategy"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getBookRating_ShouldThrowException_WhenBookServiceFails() {
        when(bookServiceClient.getBookById(1))
                .thenThrow(new ExternalServiceException("External service error"));

        assertThatThrownBy(() -> analyticsService.getBookRating(1, "averageRatingStrategy"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("External service error");

        verify(reviewRepository, never()).findByBookId(anyInt());
    }

    @Test
    void getBookRating_ShouldThrowException_WhenRepositoryFails() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> analyticsService.getBookRating(1, "averageRatingStrategy"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(bookServiceClient, times(1)).getBookById(1);
    }

    @Test
    void getBookRating_ShouldThrowException_WhenStrategyCalculationFails() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);
        when(averageRatingStrategy.calculateRating(reviews))
                .thenThrow(new ArithmeticException("Calculation error"));

        assertThatThrownBy(() -> analyticsService.getBookRating(1, "averageRatingStrategy"))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("Calculation error");
    }

    // ========== Edge Cases ==========

    @Test
    void getBookRating_ShouldHandleDecimalRatings() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);
        when(averageRatingStrategy.calculateRating(reviews)).thenReturn(3.456789);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(3.456789);
    }

    @Test
    void getBookRating_ShouldHandleLargeBookId() {
        int largeBookId = Integer.MAX_VALUE;

        when(bookServiceClient.getBookById(largeBookId)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(largeBookId)).thenReturn(reviews);
        when(averageRatingStrategy.calculateRating(reviews)).thenReturn(4.0);

        double result = analyticsService.getBookRating(largeBookId, "averageRatingStrategy");

        assertThat(result).isEqualTo(4.0);

        verify(bookServiceClient, times(1)).getBookById(largeBookId);
    }

    @Test
    void getBookRating_ShouldHandleLargeNumberOfReviews() {
        List<Review> largeReviewList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Review review = new Review();
            review.setReviewId(i);
            review.setBookId(1);
            review.setRating(4.0);
            largeReviewList.add(review);
        }

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(largeReviewList);
        when(averageRatingStrategy.calculateRating(largeReviewList)).thenReturn(4.0);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(4.0);

        verify(averageRatingStrategy, times(1)).calculateRating(largeReviewList);
    }

    @Test
    void getBookRating_ShouldHandleNullReviewList() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(null);
        when(averageRatingStrategy.calculateRating(null)).thenReturn(0.0);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void getBookRating_ShouldSwitchBetweenStrategies() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);
        when(averageRatingStrategy.calculateRating(reviews)).thenReturn(4.0);
        when(weightedRatingStrategy.calculateRating(reviews)).thenReturn(4.5);

        double averageResult = analyticsService.getBookRating(1, "averageRatingStrategy");
        double weightedResult = analyticsService.getBookRating(1, "weightedRatingStrategy");

        assertThat(averageResult).isEqualTo(4.0);
        assertThat(weightedResult).isEqualTo(4.5);

        verify(averageRatingStrategy, times(1)).calculateRating(reviews);
        verify(weightedRatingStrategy, times(1)).calculateRating(reviews);
    }

    @Test
    void getBookRating_ShouldHandleMixedRatings() {
        List<Review> mixedReviews = new ArrayList<>();

        Review r1 = new Review();
        r1.setRating(1.0);
        Review r2 = new Review();
        r2.setRating(5.0);
        Review r3 = new Review();
        r3.setRating(3.0);

        mixedReviews.add(r1);
        mixedReviews.add(r2);
        mixedReviews.add(r3);

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(mixedReviews);
        when(averageRatingStrategy.calculateRating(mixedReviews)).thenReturn(3.0);

        double result = analyticsService.getBookRating(1, "averageRatingStrategy");

        assertThat(result).isEqualTo(3.0);
    }

    @Test
    void getBookRating_ShouldVerifyStrategyIsCalledOnce() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);
        when(averageRatingStrategy.calculateRating(reviews)).thenReturn(4.0);

        analyticsService.getBookRating(1, "averageRatingStrategy");

        verify(averageRatingStrategy, times(1)).calculateRating(reviews);
        verify(weightedRatingStrategy, never()).calculateRating(any());
    }

    @Test
    void getBookRating_ShouldHandleEmptyStrategyName() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);

        assertThatThrownBy(() -> analyticsService.getBookRating(1, ""))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getBookRating_ShouldHandleMultipleConsecutiveCalls() {
        when(bookServiceClient.getBookById(anyInt())).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(anyInt())).thenReturn(reviews);
        when(averageRatingStrategy.calculateRating(reviews)).thenReturn(4.0, 4.2, 4.5);

        double result1 = analyticsService.getBookRating(1, "averageRatingStrategy");
        double result2 = analyticsService.getBookRating(2, "averageRatingStrategy");
        double result3 = analyticsService.getBookRating(3, "averageRatingStrategy");

        assertThat(result1).isEqualTo(4.0);
        assertThat(result2).isEqualTo(4.2);
        assertThat(result3).isEqualTo(4.5);

        verify(averageRatingStrategy, times(3)).calculateRating(reviews);
    }
}

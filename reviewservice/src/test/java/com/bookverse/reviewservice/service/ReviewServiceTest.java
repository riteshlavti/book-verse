package com.bookverse.reviewservice.service;

import com.bookverse.reviewservice.dto.BookResponseDto;
import com.bookverse.reviewservice.dto.ReviewRequestDto;
import com.bookverse.reviewservice.dto.ReviewResponseDto;
import com.bookverse.reviewservice.exception.ReviewServiceException;
import com.bookverse.reviewservice.feign.BookServiceClient;
import com.bookverse.reviewservice.mapper.ReviewMapper;
import com.bookverse.reviewservice.model.Review;
import com.bookverse.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private BookServiceClient bookServiceClient;

    @InjectMocks
    private ReviewService reviewService;

    private Review review;
    private ReviewRequestDto reviewRequestDto;
    private ReviewResponseDto reviewResponseDto;
    private BookResponseDto bookResponseDto;

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setReviewId(1);
        review.setBookId(1);
        review.setReviewer("user1");
        review.setReviewText("Great book!");
        review.setRating(4.5);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        reviewRequestDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Great book!")
                .rating(4.5)
                .build();

        reviewResponseDto = ReviewResponseDto.builder()
                .reviewId(1)
                .bookId(1)
                .username("user1")
                .rating(4.5)
                .reviewText("Great book!")
                .createdAt(LocalDateTime.now())
                .build();

        bookResponseDto = BookResponseDto.builder()
                .bookId(1)
                .title("Test Book")
                .build();
    }

    @Test
    void getReviewById_ShouldReturnReview_WhenReviewExists() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewMapper.entityToResponseDto(review)).thenReturn(reviewResponseDto);

        ReviewResponseDto result = reviewService.getReviewById(1);

        assertThat(result).isNotNull();
        assertThat(result.getReviewId()).isEqualTo(1);
        assertThat(result.getRating()).isEqualTo(4.5);
        assertThat(result.getReviewText()).isEqualTo("Great book!");

        verify(reviewRepository, times(1)).findById(1);
        verify(reviewMapper, times(1)).entityToResponseDto(review);
    }

    @Test
    void createReview_ShouldCreateReview_WhenValidData() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewMapper.requestDtoToEntity(reviewRequestDto, "user1")).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.entityToResponseDto(review)).thenReturn(reviewResponseDto);

        ReviewResponseDto result = reviewService.createReview(reviewRequestDto, "user1");

        assertThat(result).isNotNull();
        assertThat(result.getReviewId()).isEqualTo(1);
        assertThat(result.getBookId()).isEqualTo(1);

        verify(bookServiceClient, times(1)).getBookById(1);
        verify(reviewRepository, times(1)).save(review);
        verify(reviewMapper, times(1)).entityToResponseDto(review);
    }

    @Test
    void getReviewsByBookId_ShouldReturnReviews_WhenReviewsExist() {
        Review review2 = new Review();
        review2.setReviewId(2);
        review2.setBookId(1);
        review2.setReviewer("user2");
        review2.setReviewText("Excellent!");
        review2.setRating(5.0);

        ReviewResponseDto response2 = ReviewResponseDto.builder()
                .reviewId(2)
                .bookId(1)
                .username("user2")
                .rating(5.0)
                .reviewText("Excellent!")
                .createdAt(LocalDateTime.now())
                .build();

        List<Review> reviews = Arrays.asList(review, review2);
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(reviews);
        when(reviewMapper.entityToResponseDto(review)).thenReturn(reviewResponseDto);
        when(reviewMapper.entityToResponseDto(review2)).thenReturn(response2);

        List<ReviewResponseDto> result = reviewService.getReviewsByBookId(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReviewId()).isEqualTo(1);
        assertThat(result.get(1).getReviewId()).isEqualTo(2);

        verify(bookServiceClient, times(1)).getBookById(1);
        verify(reviewRepository, times(1)).findByBookId(1);
        verify(reviewMapper, times(2)).entityToResponseDto(any(Review.class));
    }

    @Test
    void updateReview_ShouldUpdateReview_WhenAuthorized() {
        ReviewRequestDto updateDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Updated review!")
                .rating(5.0)
                .build();

        Review updatedReview = new Review();
        updatedReview.setReviewId(1);
        updatedReview.setBookId(1);
        updatedReview.setReviewer("user1");
        updatedReview.setReviewText("Updated review!");
        updatedReview.setRating(5.0);

        ReviewResponseDto updatedResponse = ReviewResponseDto.builder()
                .reviewId(1)
                .bookId(1)
                .username("user1")
                .rating(5.0)
                .reviewText("Updated review!")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(updatedReview);
        when(reviewMapper.entityToResponseDto(updatedReview)).thenReturn(updatedResponse);

        ReviewResponseDto result = reviewService.updateReview(1, updateDto, "user1");

        assertThat(result).isNotNull();
        assertThat(result.getReviewText()).isEqualTo("Updated review!");
        assertThat(result.getRating()).isEqualTo(5.0);

        verify(reviewRepository, times(1)).findById(1);
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(reviewMapper, times(1)).entityToResponseDto(updatedReview);
    }

    @Test
    void deleteReview_ShouldDeleteReview_WhenAuthorized() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        doNothing().when(reviewRepository).deleteById(1);

        String result = reviewService.deleteReview(1, "user1");

        assertThat(result).isEqualTo("Review deleted successfully");

        verify(reviewRepository, times(1)).findById(1);
        verify(reviewRepository, times(1)).deleteById(1);
    }

    @Test
    void getReviewById_ShouldThrowException_WhenReviewNotFound() {
        when(reviewRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById(999))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Review not found with id: 999");

        verify(reviewRepository, times(1)).findById(999);
        verify(reviewMapper, never()).entityToResponseDto(any());
    }

    @Test
    void createReview_ShouldThrowException_WhenBookNotFound() {
        when(bookServiceClient.getBookById(999)).thenThrow(new RuntimeException("Book not found"));

        assertThatThrownBy(() -> reviewService.createReview(reviewRequestDto, "user1"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Failed to create review");

        verify(bookServiceClient, times(1)).getBookById(anyInt());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_ShouldThrowException_WhenSaveFails() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewMapper.requestDtoToEntity(reviewRequestDto, "user1")).thenReturn(review);
        when(reviewRepository.save(review)).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> reviewService.createReview(reviewRequestDto, "user1"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Failed to create review");

        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void getReviewsByBookId_ShouldThrowException_WhenBookNotFound() {
        when(bookServiceClient.getBookById(999)).thenThrow(new RuntimeException("Book not found"));

        assertThatThrownBy(() -> reviewService.getReviewsByBookId(999))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Failed to fetch reviews for bookId: 999");

        verify(bookServiceClient, times(1)).getBookById(999);
        verify(reviewRepository, never()).findByBookId(anyInt());
    }

    @Test
    void updateReview_ShouldThrowException_WhenReviewNotFound() {
        when(reviewRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(999, reviewRequestDto, "user1"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Review not found with id: 999");

        verify(reviewRepository, times(1)).findById(999);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_ShouldThrowException_WhenUnauthorized() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(1, reviewRequestDto, "user2"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Unauthorized to update this review");

        verify(reviewRepository, times(1)).findById(1);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_ShouldThrowException_WhenSaveFails() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> reviewService.updateReview(1, reviewRequestDto, "user1"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Failed to update review");

        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void deleteReview_ShouldThrowException_WhenReviewNotFound() {
        when(reviewRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(999, "user1"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Review not found with id: 999");

        verify(reviewRepository, times(1)).findById(999);
        verify(reviewRepository, never()).deleteById(anyInt());
    }

    @Test
    void deleteReview_ShouldThrowException_WhenUnauthorized() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(1, "user2"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Unauthorized to delete this review");

        verify(reviewRepository, times(1)).findById(1);
        verify(reviewRepository, never()).deleteById(anyInt());
    }

    @Test
    void deleteReview_ShouldThrowException_WhenDeleteFails() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        doThrow(new RuntimeException("Database error")).when(reviewRepository).deleteById(1);

        assertThatThrownBy(() -> reviewService.deleteReview(1, "user1"))
                .isInstanceOf(ReviewServiceException.class)
                .hasMessageContaining("Failed to delete review");

        verify(reviewRepository, times(1)).deleteById(1);
    }

    @Test
    void getReviewsByBookId_ShouldReturnEmptyList_WhenNoReviewsExist() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(Collections.emptyList());

        List<ReviewResponseDto> result = reviewService.getReviewsByBookId(1);

        assertThat(result).isEmpty();

        verify(bookServiceClient, times(1)).getBookById(1);
        verify(reviewRepository, times(1)).findByBookId(1);
    }

    @Test
    void createReview_ShouldHandleMinimumRating() {
        ReviewRequestDto minRatingDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Not good")
                .rating(1.0)
                .build();

        Review minRatingReview = new Review();
        minRatingReview.setRating(1.0);
        minRatingReview.setBookId(1);
        minRatingReview.setReviewer("user1");
        minRatingReview.setReviewText("Not good");

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewMapper.requestDtoToEntity(minRatingDto, "user1")).thenReturn(minRatingReview);
        when(reviewRepository.save(minRatingReview)).thenReturn(minRatingReview);
        when(reviewMapper.entityToResponseDto(minRatingReview)).thenReturn(
                ReviewResponseDto.builder().rating(1.0).build()
        );

        ReviewResponseDto result = reviewService.createReview(minRatingDto, "user1");

        assertThat(result.getRating()).isEqualTo(1.0);
    }

    @Test
    void createReview_ShouldHandleMaximumRating() {
        ReviewRequestDto maxRatingDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Perfect!")
                .rating(5.0)
                .build();

        Review maxRatingReview = new Review();
        maxRatingReview.setRating(5.0);
        maxRatingReview.setBookId(1);
        maxRatingReview.setReviewer("user1");
        maxRatingReview.setReviewText("Perfect!");

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewMapper.requestDtoToEntity(maxRatingDto, "user1")).thenReturn(maxRatingReview);
        when(reviewRepository.save(maxRatingReview)).thenReturn(maxRatingReview);
        when(reviewMapper.entityToResponseDto(maxRatingReview)).thenReturn(
                ReviewResponseDto.builder().rating(5.0).build()
        );

        ReviewResponseDto result = reviewService.createReview(maxRatingDto, "user1");

        assertThat(result.getRating()).isEqualTo(5.0);
    }

    @Test
    void createReview_ShouldHandleMaximumLengthReviewText() {
        String maxLengthText = "a".repeat(500);
        ReviewRequestDto maxLengthDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText(maxLengthText)
                .rating(4.0)
                .build();

        Review maxLengthReview = new Review();
        maxLengthReview.setReviewText(maxLengthText);
        maxLengthReview.setBookId(1);
        maxLengthReview.setReviewer("user1");
        maxLengthReview.setRating(4.0);

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewMapper.requestDtoToEntity(maxLengthDto, "user1")).thenReturn(maxLengthReview);
        when(reviewRepository.save(maxLengthReview)).thenReturn(maxLengthReview);
        when(reviewMapper.entityToResponseDto(maxLengthReview)).thenReturn(
                ReviewResponseDto.builder().reviewText(maxLengthText).build()
        );

        ReviewResponseDto result = reviewService.createReview(maxLengthDto, "user1");

        assertThat(result.getReviewText()).hasSize(500);
    }

    @Test
    void updateReview_ShouldOnlyUpdateAllowedFields() {
        ReviewRequestDto updateDto = ReviewRequestDto.builder()
                .bookId(99) // Should not update bookId
                .reviewText("Updated text")
                .rating(4.8)
                .build();

        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewMapper.entityToResponseDto(any(Review.class))).thenReturn(
                ReviewResponseDto.builder()
                        .reviewId(1)
                        .bookId(1) // Original bookId
                        .reviewText("Updated text")
                        .rating(4.8)
                        .build()
        );

        ReviewResponseDto result = reviewService.updateReview(1, updateDto, "user1");

        assertThat(result.getBookId()).isEqualTo(1); // Should remain unchanged
        assertThat(result.getReviewText()).isEqualTo("Updated text");
        assertThat(result.getRating()).isEqualTo(4.8);
    }

    @Test
    void getReviewsByBookId_ShouldHandleLargeNumberOfReviews() {
        List<Review> largeReviewList = Arrays.asList(
                review,
                createReview(2, "user2", 4.0),
                createReview(3, "user3", 5.0),
                createReview(4, "user4", 3.5),
                createReview(5, "user5", 4.5)
        );

        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewRepository.findByBookId(1)).thenReturn(largeReviewList);
        when(reviewMapper.entityToResponseDto(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            return ReviewResponseDto.builder()
                    .reviewId(r.getReviewId())
                    .bookId(r.getBookId())
                    .rating(r.getRating())
                    .build();
        });

        List<ReviewResponseDto> result = reviewService.getReviewsByBookId(1);

        assertThat(result).hasSize(5);
    }

    @Test
    void createReview_ShouldHandleDifferentUserIds() {
        when(bookServiceClient.getBookById(1)).thenReturn(bookResponseDto);
        when(reviewMapper.requestDtoToEntity(any(), anyString())).thenReturn(review);
        when(reviewRepository.save(any())).thenReturn(review);
        when(reviewMapper.entityToResponseDto(any())).thenReturn(reviewResponseDto);

        reviewService.createReview(reviewRequestDto, "user123");
        reviewService.createReview(reviewRequestDto, "admin");
        reviewService.createReview(reviewRequestDto, "testUser");

        verify(reviewMapper, times(1)).requestDtoToEntity(any(), eq("user123"));
        verify(reviewMapper, times(1)).requestDtoToEntity(any(), eq("admin"));
        verify(reviewMapper, times(1)).requestDtoToEntity(any(), eq("testUser"));
    }

    // Helper method
    private Review createReview(int id, String reviewer, double rating) {
        Review r = new Review();
        r.setReviewId(id);
        r.setBookId(1);
        r.setReviewer(reviewer);
        r.setRating(rating);
        r.setReviewText("Test review");
        return r;
    }
}

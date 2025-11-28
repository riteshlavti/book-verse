package com.bookverse.reviewservice.controller;

import com.bookverse.reviewservice.config.SecurityConfig;
import com.bookverse.reviewservice.dto.ReviewRequestDto;
import com.bookverse.reviewservice.dto.ReviewResponseDto;
import com.bookverse.reviewservice.exception.ReviewServiceException;
import com.bookverse.reviewservice.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    private ReviewRequestDto reviewRequestDto;
    private ReviewResponseDto reviewResponseDto;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    @WithMockUser
    void getReviewById_ShouldReturnReview_WhenReviewExists() throws Exception {
        when(reviewService.getReviewById(1)).thenReturn(reviewResponseDto);

        mockMvc.perform(get("/api/review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.reviewText").value("Great book!"));

        verify(reviewService, times(1)).getReviewById(1);
    }

    @Test
    @WithMockUser
    void getReviewsByBookId_ShouldReturnListOfReviews_WhenReviewsExist() throws Exception {
        ReviewResponseDto review2 = ReviewResponseDto.builder()
                .reviewId(2)
                .bookId(1)
                .username("user2")
                .rating(5.0)
                .reviewText("Excellent!")
                .createdAt(LocalDateTime.now())
                .build();

        List<ReviewResponseDto> reviews = Arrays.asList(reviewResponseDto, review2);
        when(reviewService.getReviewsByBookId(1)).thenReturn(reviews);

        mockMvc.perform(get("/api/review/book/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reviewId").value(1))
                .andExpect(jsonPath("$[1].reviewId").value(2));

        verify(reviewService, times(1)).getReviewsByBookId(1);
    }

    @Test
    @WithMockUser
    void addReview_ShouldCreateReview_WhenValidRequest() throws Exception {
        when(reviewService.createReview(any(ReviewRequestDto.class), eq("user1")))
                .thenReturn(reviewResponseDto);

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.rating").value(4.5));

        verify(reviewService, times(1)).createReview(any(ReviewRequestDto.class), eq("user1"));
    }

    @Test
    @WithMockUser
    void updateReview_ShouldUpdateReview_WhenValidRequest() throws Exception {
        ReviewResponseDto updatedReview = ReviewResponseDto.builder()
                .reviewId(1)
                .bookId(1)
                .username("user1")
                .rating(5.0)
                .reviewText("Updated: Absolutely amazing!")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.updateReview(eq(1), any(ReviewRequestDto.class), eq("user1")))
                .thenReturn(updatedReview);

        ReviewRequestDto updateDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Updated: Absolutely amazing!")
                .rating(5.0)
                .build();

        mockMvc.perform(put("/api/review/1")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.reviewText").value("Updated: Absolutely amazing!"));

        verify(reviewService, times(1)).updateReview(eq(1), any(ReviewRequestDto.class), eq("user1"));
    }

    @Test
    @WithMockUser
    void deleteReview_ShouldDeleteReview_WhenValidRequest() throws Exception {
        when(reviewService.deleteReview(1, "user1")).thenReturn("Review deleted successfully");

        mockMvc.perform(delete("/api/review/1")
                        .with(csrf())
                        .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Review deleted successfully"));

        verify(reviewService, times(1)).deleteReview(1, "user1");
    }


    @Test
    @WithMockUser
    void getReviewById_ShouldReturnNotFound_WhenReviewDoesNotExist() throws Exception {
        when(reviewService.getReviewById(999)).thenThrow(new ReviewServiceException("Review not found with id: 999"));

        mockMvc.perform(get("/api/review/999"))
                .andExpect(status().isNotFound());

        verify(reviewService, times(1)).getReviewById(999);
    }

    @Test
    @WithMockUser
    void getReviewsByBookId_ShouldReturnEmptyList_WhenNoReviewsExist() throws Exception {
        when(reviewService.getReviewsByBookId(999)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/review/book/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(reviewService, times(1)).getReviewsByBookId(999);
    }

    @Test
    @WithMockUser
    void addReview_ShouldReturnBadRequest_WhenMissingRequiredFields() throws Exception {
        ReviewRequestDto invalidDto = ReviewRequestDto.builder()
                .bookId(1)
                // Missing reviewText and rating
                .build();

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser
    void addReview_ShouldReturnBadRequest_WhenRatingOutOfRange() throws Exception {
        ReviewRequestDto invalidDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Test review")
                .rating(6.0) // Invalid rating > 5
                .build();

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser
    void addReview_ShouldReturnBadRequest_WhenReviewTextTooLong() throws Exception {
        String longText = "a".repeat(501); // Exceeds 500 character limit
        ReviewRequestDto invalidDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText(longText)
                .rating(4.5)
                .build();

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    @WithMockUser
    void updateReview_ShouldReturnError_WhenUnauthorized() throws Exception {
        when(reviewService.updateReview(eq(1), any(ReviewRequestDto.class), eq("user2")))
                .thenThrow(new ReviewServiceException("Unauthorized to update this review"));

        mockMvc.perform(put("/api/review/1")
                        .with(csrf())
                        .header("X-User-Id", "user2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequestDto)))
                .andExpect(status().isInternalServerError());

        verify(reviewService, times(1)).updateReview(eq(1), any(ReviewRequestDto.class), eq("user2"));
    }

    @Test
    @WithMockUser
    void deleteReview_ShouldReturnError_WhenUnauthorized() throws Exception {
        when(reviewService.deleteReview(1, "user2"))
                .thenThrow(new ReviewServiceException("Unauthorized to delete this review"));

        mockMvc.perform(delete("/api/review/1")
                        .with(csrf())
                        .header("X-User-Id", "user2"))
                .andExpect(status().isInternalServerError());

        verify(reviewService, times(1)).deleteReview(1, "user2");
    }

    @Test
    @WithMockUser
    void addReview_ShouldHandleMinimumRating() throws Exception {
        ReviewRequestDto minRatingDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Not good")
                .rating(1.0)
                .build();

        ReviewResponseDto minRatingResponse = ReviewResponseDto.builder()
                .reviewId(1)
                .bookId(1)
                .username("user1")
                .rating(1.0)
                .reviewText("Not good")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(ReviewRequestDto.class), eq("user1")))
                .thenReturn(minRatingResponse);

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minRatingDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(1.0));

        verify(reviewService, times(1)).createReview(any(ReviewRequestDto.class), eq("user1"));
    }

    @Test
    @WithMockUser
    void addReview_ShouldHandleMaximumRating() throws Exception {
        ReviewRequestDto maxRatingDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText("Perfect!")
                .rating(5.0)
                .build();

        ReviewResponseDto maxRatingResponse = ReviewResponseDto.builder()
                .reviewId(1)
                .bookId(1)
                .username("user1")
                .rating(5.0)
                .reviewText("Perfect!")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(ReviewRequestDto.class), eq("user1")))
                .thenReturn(maxRatingResponse);

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxRatingDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0));

        verify(reviewService, times(1)).createReview(any(ReviewRequestDto.class), eq("user1"));
    }

    @Test
    @WithMockUser
    void addReview_ShouldHandleMaximumLengthReviewText() throws Exception {
        String maxLengthText = "a".repeat(500); // Exactly 500 characters
        ReviewRequestDto maxLengthDto = ReviewRequestDto.builder()
                .bookId(1)
                .reviewText(maxLengthText)
                .rating(4.0)
                .build();

        ReviewResponseDto maxLengthResponse = ReviewResponseDto.builder()
                .reviewId(1)
                .bookId(1)
                .username("user1")
                .rating(4.0)
                .reviewText(maxLengthText)
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(ReviewRequestDto.class), eq("user1")))
                .thenReturn(maxLengthResponse);

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxLengthDto)))
                .andExpect(status().isOk());

        verify(reviewService, times(1)).createReview(any(ReviewRequestDto.class), eq("user1"));
    }

    @Test
    @WithMockUser
    void getReviewsByBookId_ShouldHandleLargeNumberOfReviews() throws Exception {
        List<ReviewResponseDto> largeList = Arrays.asList(
                reviewResponseDto,
                ReviewResponseDto.builder().reviewId(2).bookId(1).rating(4.0).reviewText("Good").createdAt(LocalDateTime.now()).build(),
                ReviewResponseDto.builder().reviewId(3).bookId(1).rating(5.0).reviewText("Excellent").createdAt(LocalDateTime.now()).build()
        );

        when(reviewService.getReviewsByBookId(1)).thenReturn(largeList);

        mockMvc.perform(get("/api/review/book/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        verify(reviewService, times(1)).getReviewsByBookId(1);
    }

    @Test
    @WithMockUser
    void addReview_ShouldReturnError_WhenServiceThrowsException() throws Exception {
        when(reviewService.createReview(any(ReviewRequestDto.class), eq("user1")))
                .thenThrow(new ReviewServiceException("Failed to create review"));

        mockMvc.perform(post("/api/review/")
                        .with(csrf())
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequestDto)))
                .andExpect(status().isInternalServerError());

        verify(reviewService, times(1)).createReview(any(ReviewRequestDto.class), eq("user1"));
    }
}

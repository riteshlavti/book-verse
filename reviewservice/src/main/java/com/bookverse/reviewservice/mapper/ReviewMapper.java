package com.bookverse.reviewservice.mapper;

import com.bookverse.reviewservice.dto.ReviewRequestDto;
import com.bookverse.reviewservice.dto.ReviewResponseDto;
import com.bookverse.reviewservice.model.Review;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
@Builder
public class ReviewMapper {

    public ReviewResponseDto entityToResponseDto(Review review) {
        return ReviewResponseDto.builder()
                .reviewId(review.getReviewId())
                .bookId(review.getBookId())
                .reviewText(review.getReviewText())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .build();
    }
    public Review requestDtoToEntity(ReviewRequestDto dto, String reviewer) {
        Review review = new Review();
        review.setBookId(dto.getBookId());
        review.setReviewText(dto.getReviewText());
        review.setRating(dto.getRating());
        review.setReviewer(reviewer); // Default username, can be modified later
        return review;
    }
}

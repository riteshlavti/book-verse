package com.bookverse.reviewservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    private int reviewId;
    private int bookId;
    private String username;
    private double rating;
    private String reviewText;
    private LocalDateTime createdAt;
}

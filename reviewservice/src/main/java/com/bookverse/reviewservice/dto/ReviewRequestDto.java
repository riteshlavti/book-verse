package com.bookverse.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequestDto {
    @NotNull
    private int bookId;

    @NotNull
    @Size(max = 500, message = "Review text must be less than 500 characters")
    private String reviewText;

    @NotNull
    @Min(1)
    @Max(5)
    private double rating;
}

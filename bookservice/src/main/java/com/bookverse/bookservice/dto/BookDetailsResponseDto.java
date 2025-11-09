package com.bookverse.bookservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookDetailsResponseDto {
    private BookResponseDto book;
    private List<ReviewDto> reviews;
    private double averageRating;
}

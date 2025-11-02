package com.bookverse.bookservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ReviewDto {
    private String reviewerName;
    private int reviewRating;
    private String reviewComment;
    private Date reviewDate;
}

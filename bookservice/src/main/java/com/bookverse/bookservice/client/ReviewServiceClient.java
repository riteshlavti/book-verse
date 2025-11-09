package com.bookverse.bookservice.client;

import com.bookverse.bookservice.dto.ReviewDto;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("review-service")
public interface ReviewServiceClient {

    @GetMapping("/api/review/book/{bookId}/average-rating")
    public double getBookRating(@NotNull @PathVariable("bookId") int id,
                                @RequestParam(name = "strategy", defaultValue = "averageRatingStrategy") String strategy);

    @GetMapping("/api/review/book/{bookId}")
    public List<ReviewDto> getReviewsByBookId(@NotNull @PathVariable("bookId") int id);
}

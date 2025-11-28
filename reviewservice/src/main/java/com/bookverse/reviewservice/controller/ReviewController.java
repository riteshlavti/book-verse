package com.bookverse.reviewservice.controller;

import com.bookverse.reviewservice.dto.ReviewRequestDto;
import com.bookverse.reviewservice.dto.ReviewResponseDto;
import com.bookverse.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review/")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/{id}")
    public ReviewResponseDto getReviewById(@NotNull @PathVariable ("id") int id) {
        return reviewService.getReviewById(id);
    }

    @GetMapping("/book/{bookId}")
    public List<ReviewResponseDto> getReviewsByBookId(@NotNull @PathVariable ("bookId") int bookId) {
        return reviewService.getReviewsByBookId(bookId);
    }

    @PostMapping("")
    public ReviewResponseDto addReview(@Valid @RequestBody ReviewRequestDto reviewRequestDto,
                                       @RequestHeader ("X-User-Id") String reviewerId) {
        return reviewService.createReview(reviewRequestDto, reviewerId);
    }

    @PutMapping("/{id}")
    public ReviewResponseDto updateReview(@NotNull @PathVariable("id") int id,
                                          @Valid @RequestBody ReviewRequestDto reviewRequestDto,
                                          @RequestHeader("X-User-Id") String reviewerId) {
        return reviewService.updateReview(id, reviewRequestDto, reviewerId);
    }

    @DeleteMapping("/{id}")
    public String deleteReview(@NotNull @PathVariable("id") int id,
                               @RequestHeader("X-User-Id") String reviewerId) {
        return reviewService.deleteReview(id, reviewerId);
    }
}

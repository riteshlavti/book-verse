package com.bookverse.reviewservice.controller;

import com.bookverse.reviewservice.service.AnalyticsService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review/")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/book/{bookId}/average-rating")
    public double getBookRating(@NotNull @PathVariable int bookId,
                                @RequestParam(name = "strategy", defaultValue = "averageRatingStrategy") String strategy) {
        return analyticsService.getBookRating(bookId, strategy);
    }
}

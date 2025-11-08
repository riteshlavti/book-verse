package com.bookverse.bookservice.controller;

import com.bookverse.bookservice.dto.BookDetailsResponseDto;
import com.bookverse.bookservice.service.BookDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
public class BookDetailsController {

    @Autowired
    private BookDetailsService BookDetailsService;

    @GetMapping("/{bookId}/details")
    public BookDetailsResponseDto getBookDetails(
            @PathVariable("bookId") int bookId,
            @RequestParam(name ="reviewStrategy", defaultValue = "averageRatingStrategy") String reviewStrategy) {
        return BookDetailsService.getBookDetails(bookId, reviewStrategy);
    }
}

package com.bookverse.bookservice.controller;

import com.bookverse.bookservice.model.Book;
import com.bookverse.bookservice.service.BookSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookSearchController {

    @Autowired
    BookSearchService bookSearchService;

    @GetMapping("")
    public List<Book> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return bookSearchService.searchBooks(null, null, null, page, size);
    }

    @GetMapping("/search")
    public List<Book> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bookSearchService.searchBooks(query, author, genre, page, size);
    }
}

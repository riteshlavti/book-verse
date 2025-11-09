package com.bookverse.bookservice.controller;

import com.bookverse.bookservice.dto.BookRequestDto;
import com.bookverse.bookservice.dto.BookResponseDto;
import com.bookverse.bookservice.service.BookCrudService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
public class BookCrudController {

    @Autowired
    BookCrudService bookCrudService;

    @GetMapping("/{id}")
    public BookResponseDto getBookById(@PathVariable("id") int id){
        return bookCrudService.getBookById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    public String addBook(@Valid @RequestBody BookRequestDto book) {
        return  bookCrudService.addBook(book);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public String updateBook(@PathVariable("id") int id, @Valid @RequestBody BookRequestDto book) {
        return bookCrudService.updateBookDetails(id, book);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable("id") int id) {
        return bookCrudService.deleteBook(id);
    }
}

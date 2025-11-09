package com.bookverse.bookservice.service;

import com.bookverse.bookservice.repository.BookRepository;
import com.bookverse.bookservice.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookSearchService {

    @Autowired
    BookRepository bookRepository;

    public List<Book> searchBooks(String query, String author, String genre, int page, int size) {

        if (query != null && !query.isEmpty()) {
            return bookRepository
                    .fullTextSearch(query, PageRequest.of(page, size));
        } else if (author != null && !author.isEmpty() && genre != null && !genre.isEmpty()) {
            return bookRepository.findByAuthorContainingIgnoreCaseAndGenreContainingIgnoreCase(author, genre, PageRequest.of(page,size));
        } else if (author != null && !author.isEmpty()) {
            return bookRepository.findByAuthorContainingIgnoreCase(author, PageRequest.of(page,size));
        } else if (genre != null && !genre.isEmpty()) {
            return bookRepository.findByGenreContainingIgnoreCase(genre, PageRequest.of(page,size));
        } else {
            return bookRepository.findAll();
        }
    }
}

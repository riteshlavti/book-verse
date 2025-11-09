package com.bookverse.bookservice.service;

import com.bookverse.bookservice.model.Book;
import com.bookverse.bookservice.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookSearchServiceTest {
    @Mock
    BookRepository bookRepository;
    @InjectMocks
    BookSearchService bookSearchService;

    @Test
    void testSearchBooks_Query() {
        Book book = new Book();
        List<Book> books = List.of(book);
        when(bookRepository.fullTextSearch("query", PageRequest.of(0, 10))).thenReturn(books);
        List<Book> result = bookSearchService.searchBooks("query", null, null, 0, 10);
        assertEquals(books, result);
    }

    @Test
    void testSearchBooks_AuthorAndGenre() {
        Book book = new Book();
        List<Book> books = List.of(book);
        when(bookRepository.findByAuthorContainingIgnoreCaseAndGenreContainingIgnoreCase("author", "genre", PageRequest.of(0, 10))).thenReturn(books);
        List<Book> result = bookSearchService.searchBooks(null, "author", "genre", 0, 10);
        assertEquals(books, result);
    }

    @Test
    void testSearchBooks_AuthorOnly() {
        Book book = new Book();
        List<Book> books = List.of(book);
        when(bookRepository.findByAuthorContainingIgnoreCase("author", PageRequest.of(0, 10))).thenReturn(books);
        List<Book> result = bookSearchService.searchBooks(null, "author", null, 0, 10);
        assertEquals(books, result);
    }

    @Test
    void testSearchBooks_GenreOnly() {
        Book book = new Book();
        List<Book> books = List.of(book);
        when(bookRepository.findByGenreContainingIgnoreCase("genre", PageRequest.of(0, 10))).thenReturn(books);
        List<Book> result = bookSearchService.searchBooks(null, null, "genre", 0, 10);
        assertEquals(books, result);
    }

    @Test
    void testSearchBooks_All() {
        List<Book> books = Collections.emptyList();
        when(bookRepository.findAll()).thenReturn(books);
        List<Book> result = bookSearchService.searchBooks(null, null, null, 0, 10);
        assertEquals(books, result);
    }
}

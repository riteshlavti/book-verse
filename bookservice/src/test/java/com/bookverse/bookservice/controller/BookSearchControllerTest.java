package com.bookverse.bookservice.controller;

import com.bookverse.bookservice.model.Book;
import com.bookverse.bookservice.service.BookSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookSearchController.class)
public class BookSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookSearchService bookSearchService;

    @Test
    @WithMockUser
    void testGetAllBooks_Success() throws Exception {
        Book book = new Book();
        book.setBookId(1);
        book.setTitle("Book 1");
        List<Book> books = Arrays.asList(book);
        Mockito.when(bookSearchService.searchBooks(null, null, null, 0, 20)).thenReturn(books);

        mockMvc.perform(get("/api/books?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].title").value("Book 1"));
    }

    @Test
    @WithMockUser
    void testSearchBooks_Success() throws Exception {
        Book book = new Book();
        book.setBookId(2);
        book.setTitle("Search Book");
        List<Book> books = Arrays.asList(book);
        Mockito.when(bookSearchService.searchBooks("search", "author", "genre", 0, 10)).thenReturn(books);

        mockMvc.perform(get("/api/books/search?query=search&author=author&genre=genre&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(2))
                .andExpect(jsonPath("$[0].title").value("Search Book"));
    }

    @Test
    @WithMockUser
    void testGetAllBooks_EmptyResult() throws Exception {
        Mockito.when(bookSearchService.searchBooks(null, null, null, 0, 20)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/books?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}

package com.bookverse.bookservice.controller;

import com.bookverse.bookservice.config.SecurityConfig;
import com.bookverse.bookservice.dto.BookRequestDto;
import com.bookverse.bookservice.dto.BookResponseDto;
import com.bookverse.bookservice.mapper.BookMapper;
import com.bookverse.bookservice.service.BookCrudService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookCrudController.class)
@Import(SecurityConfig.class)
class BookCrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookCrudService bookCrudService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookMapper bookMapper;

    @Test
    @WithMockUser
    void shouldReturnBookById() throws Exception {
        BookResponseDto mockBook = new BookResponseDto(1,"Deep Work", "Cal Newport", "Tech", LocalDate.now());
        when(bookCrudService.getBookById(1)).thenReturn(mockBook);

        mockMvc.perform(get("/api/book/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Deep Work"))
                .andExpect(jsonPath("$.author").value("Cal Newport"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAddBookWhenAdmin() throws Exception {
        BookRequestDto request = new BookRequestDto("Clean Code", "Robert Martin", "Tech", LocalDate.now());
        when(bookCrudService.addBook(any(BookRequestDto.class))).thenReturn("Book added successfully");

        mockMvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Book added successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectAddBookWhenNotAdmin() throws Exception {
        BookRequestDto request = new BookRequestDto("Clean Code", "Robert Martin", "Tech", LocalDate.now());

        mockMvc.perform(post("/api/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateBookWhenAdmin() throws Exception {
        BookRequestDto updateRequest = new BookRequestDto("Deep Work (Updated)", "Cal Newport", "Tech", LocalDate.now());
        when(bookCrudService.updateBookDetails(eq(1), any(BookRequestDto.class)))
                .thenReturn("Book updated successfully");

        mockMvc.perform(put("/api/book/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Book updated successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteBookWhenAdmin() throws Exception {
        when(bookCrudService.deleteBook(1)).thenReturn("Book deleted successfully");

        mockMvc.perform(delete("/api/book/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Book deleted successfully"));
    }
}

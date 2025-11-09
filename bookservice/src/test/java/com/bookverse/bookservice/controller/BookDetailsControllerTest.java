package com.bookverse.bookservice.controller;

import com.bookverse.bookservice.dto.BookDetailsResponseDto;
import com.bookverse.bookservice.dto.BookResponseDto;
import com.bookverse.bookservice.dto.ReviewDto;
import com.bookverse.bookservice.service.BookCrudService;
import com.bookverse.bookservice.service.BookDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookDetailsController.class)
public class BookDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookDetailsService bookDetailsService;

    @Test
    @WithMockUser
    void testGetBookDetails_Success() throws Exception {
        BookDetailsResponseDto responseDto = new BookDetailsResponseDto();
        BookResponseDto bookDto = new BookResponseDto();
        bookDto.setBookId(1);
        bookDto.setTitle("Test Book");
        bookDto.setAuthor("Author Name");
        bookDto.setGenre("Fiction");
        bookDto.setPublished_date(LocalDate.now());

        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setCreatedAt(LocalDateTime.now());
        reviewDto.setReviewer("Reviewer");
        reviewDto.setRating(4.5);
        reviewDto.setReviewText("Great book!");

        responseDto.setBook(bookDto);
        responseDto.setReviews(java.util.List.of(reviewDto));
        responseDto.setAverageRating(4.5);


        Mockito.when(bookDetailsService.getBookDetails(anyInt(), anyString())).thenReturn(responseDto);

        mockMvc.perform(get("/api/book/1/details?reviewStrategy=averageRatingStrategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.book.bookId").value(1))
                .andExpect(jsonPath("$.book.title").value("Test Book"))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviews[0].reviewer").value("Reviewer"))
                .andExpect(jsonPath("$.reviews[0].rating").value(4.5))
                .andExpect(jsonPath("$.reviews[0].reviewText").value("Great book!"));
    }
}

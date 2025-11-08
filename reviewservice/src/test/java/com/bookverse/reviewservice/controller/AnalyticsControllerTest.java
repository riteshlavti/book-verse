package com.bookverse.reviewservice.controller;

import com.bookverse.reviewservice.config.SecurityConfig;
import com.bookverse.reviewservice.exception.ExternalServiceException;
import com.bookverse.reviewservice.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnAverageRating_WhenDefaultStrategy() throws Exception {
        when(analyticsService.getBookRating(1, "averageRatingStrategy")).thenReturn(4.5);

        mockMvc.perform(get("/api/review/book/1/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.5"));

        verify(analyticsService, times(1)).getBookRating(1, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnRating_WhenCustomStrategy() throws Exception {
        when(analyticsService.getBookRating(1, "weightedRatingStrategy")).thenReturn(4.8);

        mockMvc.perform(get("/api/review/book/1/average-rating")
                        .param("strategy", "weightedRatingStrategy"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.8"));

        verify(analyticsService, times(1)).getBookRating(1, "weightedRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnZero_WhenNoReviews() throws Exception {
        when(analyticsService.getBookRating(999, "averageRatingStrategy")).thenReturn(0.0);

        mockMvc.perform(get("/api/review/book/999/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));

        verify(analyticsService, times(1)).getBookRating(999, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnMaxRating_WhenPerfectScore() throws Exception {
        when(analyticsService.getBookRating(5, "averageRatingStrategy")).thenReturn(5.0);

        mockMvc.perform(get("/api/review/book/5/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("5.0"));

        verify(analyticsService, times(1)).getBookRating(5, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnMinRating_WhenLowestScore() throws Exception {
        when(analyticsService.getBookRating(10, "averageRatingStrategy")).thenReturn(1.0);

        mockMvc.perform(get("/api/review/book/10/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.0"));

        verify(analyticsService, times(1)).getBookRating(10, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnError_WhenBookDoesNotExist() throws Exception {
        when(analyticsService.getBookRating(999, "averageRatingStrategy"))
                .thenThrow(new ExternalServiceException("Book not found"));

        mockMvc.perform(get("/api/review/book/999/average-rating"))
                .andExpect(status().isNotFound())
                .andExpect((jsonPath("$.message").value("Book not found")));

        verify(analyticsService, times(1)).getBookRating(999, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnError_WhenInvalidStrategy() throws Exception {
        when(analyticsService.getBookRating(1, "invalidStrategy"))
                .thenThrow(new NullPointerException("Strategy not found"));

        mockMvc.perform(get("/api/review/book/1/average-rating")
                        .param("strategy", "invalidStrategy"))
                .andExpect(status().is5xxServerError());

        verify(analyticsService, times(1)).getBookRating(1, "invalidStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldReturnError_WhenServiceThrowsException() throws Exception {
        when(analyticsService.getBookRating(1, "averageRatingStrategy"))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/review/book/1/average-rating"))
                .andExpect(status().is5xxServerError());

        verify(analyticsService, times(1)).getBookRating(1, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldHandleDecimalRating() throws Exception {
        when(analyticsService.getBookRating(3, "averageRatingStrategy")).thenReturn(3.456789);

        mockMvc.perform(get("/api/review/book/3/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("3.456789"));

        verify(analyticsService, times(1)).getBookRating(3, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldHandleLargeBookId() throws Exception {
        when(analyticsService.getBookRating(Integer.MAX_VALUE, "averageRatingStrategy")).thenReturn(4.0);

        mockMvc.perform(get("/api/review/book/" + Integer.MAX_VALUE + "/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.0"));

        verify(analyticsService, times(1)).getBookRating(Integer.MAX_VALUE, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldUseDefaultStrategy_WhenStrategyParamNotProvided() throws Exception {
        when(analyticsService.getBookRating(1, "averageRatingStrategy")).thenReturn(4.2);

        mockMvc.perform(get("/api/review/book/1/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.2"));

        verify(analyticsService, times(1)).getBookRating(1, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldHandleEmptyStrategyParam() throws Exception {
        when(analyticsService.getBookRating(1, "averageRatingStrategy")).thenReturn(4.0);

        mockMvc.perform(get("/api/review/book/1/average-rating")
                        .param("strategy", ""))
                .andExpect(status().isOk());

        verify(analyticsService, times(1)).getBookRating(1, "averageRatingStrategy");
    }

    @Test
    @WithMockUser
    void getBookRating_ShouldHandleMultipleConsecutiveCalls() throws Exception {
        when(analyticsService.getBookRating(anyInt(), eq("averageRatingStrategy")))
                .thenReturn(4.0, 4.5, 3.8);

        mockMvc.perform(get("/api/review/book/1/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.0"));

        mockMvc.perform(get("/api/review/book/2/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.5"));

        mockMvc.perform(get("/api/review/book/3/average-rating"))
                .andExpect(status().isOk())
                .andExpect(content().string("3.8"));

        verify(analyticsService, times(3)).getBookRating(anyInt(), eq("averageRatingStrategy"));
    }
}

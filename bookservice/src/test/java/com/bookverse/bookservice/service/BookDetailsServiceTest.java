package com.bookverse.bookservice.service;

import com.bookverse.bookservice.client.ReviewServiceClient;
import com.bookverse.bookservice.dto.BookDetailsResponseDto;
import com.bookverse.bookservice.dto.BookResponseDto;
import com.bookverse.bookservice.dto.ReviewDto;
import com.bookverse.bookservice.exception.BookServiceException;
import com.bookverse.bookservice.mapper.BookMapper;
import com.bookverse.bookservice.model.Book;
import com.bookverse.bookservice.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookDetailsServiceTest {
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private ReviewServiceClient reviewServiceClient;
    @InjectMocks
    private BookDetailsService bookDetailsService;

    @Test
    void testGetBookDetails_Success() {
        Book book = new Book();
        book.setBookId(1);
        BookResponseDto bookDto = BookResponseDto.builder().bookId(1).title("Book").build();
        List<ReviewDto> reviews = List.of();
        double rating = 4.0;
        when(bookRepository.findById(1)).thenReturn(Optional.of(book));
        when(bookMapper.entityToDto(book)).thenReturn(bookDto);
        when(reviewServiceClient.getReviewsByBookId(1)).thenReturn(reviews);
        when(reviewServiceClient.getBookRating(1, "averageRatingStrategy")).thenReturn(rating);

        BookDetailsResponseDto result = bookDetailsService.getBookDetails(1, "averageRatingStrategy");
        assertEquals(bookDto, result.getBook());
        assertEquals(reviews, result.getReviews());
        assertEquals(rating, result.getAverageRating());
    }

    @Test
    void testGetBookDetails_BookNotFound() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(BookServiceException.class, () -> bookDetailsService.getBookDetails(99, "averageRatingStrategy"));
    }

    @Test
    void testGetBookDetails_ReviewServiceError() {
        Book book = new Book();
        book.setBookId(2);
        BookResponseDto bookDto = BookResponseDto.builder().bookId(2).title("Book").build();
        when(bookRepository.findById(2)).thenReturn(Optional.of(book));
        when(bookMapper.entityToDto(book)).thenReturn(bookDto);
        when(reviewServiceClient.getReviewsByBookId(2)).thenThrow(new RuntimeException());
        when(reviewServiceClient.getBookRating(2, "averageRatingStrategy")).thenThrow(new RuntimeException());

        BookDetailsResponseDto result = bookDetailsService.getBookDetails(2, "averageRatingStrategy");
        assertEquals(bookDto, result.getBook());
        assertEquals(List.of(), result.getReviews());
        assertEquals(0.0, result.getAverageRating());
    }
}

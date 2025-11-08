package com.bookverse.bookservice.service;

import com.bookverse.bookservice.client.ReviewServiceClient;
import com.bookverse.bookservice.dto.BookDetailsResponseDto;
import com.bookverse.bookservice.dto.BookResponseDto;
import com.bookverse.bookservice.dto.ReviewDto;
import com.bookverse.bookservice.exception.BookServiceException;
import com.bookverse.bookservice.mapper.BookMapper;
import com.bookverse.bookservice.model.Book;
import com.bookverse.bookservice.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BookDetailsService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ReviewServiceClient reviewServiceClient;

    @Async("asyncExecutor")
    public CompletableFuture<BookResponseDto> getBookById(int bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() ->
                new BookServiceException("Book not found with id: " + bookId));

        return CompletableFuture.completedFuture(bookMapper.entityToDto(book));
    }

    @Async("asyncExecutor")
    public CompletableFuture<Double> getBookAverageRating(int bookId, String strategy) {
        try {
            return CompletableFuture.completedFuture(reviewServiceClient.getBookRating(bookId, strategy));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(0.0);
        }
    }

    @Async("asyncExecutor")
    public CompletableFuture<List<ReviewDto>> getReviewsForBook(int bookId) {
        try {
            return CompletableFuture.completedFuture(reviewServiceClient.getReviewsByBookId(bookId));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    public BookDetailsResponseDto getBookDetails(int bookId, String reviewStrategy) {
        CompletableFuture<BookResponseDto> bookFuture = getBookById(bookId);
        CompletableFuture<List<ReviewDto>> reviewsFuture = getReviewsForBook(bookId);
        CompletableFuture<Double> ratingFuture = getBookAverageRating(bookId, reviewStrategy);

        CompletableFuture.allOf(bookFuture, reviewsFuture, ratingFuture).join();
        try {
            return BookDetailsResponseDto.builder()
                    .book(bookFuture.get())
                    .averageRating(ratingFuture.get())
                    .reviews(reviewsFuture.get())
                    .build();
        } catch (Exception e) {
            throw new BookServiceException("Failed to fetch book details for id: " + bookId, e);
        }
    }
}

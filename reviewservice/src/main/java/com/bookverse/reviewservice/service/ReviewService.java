package com.bookverse.reviewservice.service;

import com.bookverse.reviewservice.dto.ReviewRequestDto;
import com.bookverse.reviewservice.dto.ReviewResponseDto;
import com.bookverse.reviewservice.exception.ReviewServiceException;
import com.bookverse.reviewservice.feign.BookServiceClient;
import com.bookverse.reviewservice.mapper.ReviewMapper;
import com.bookverse.reviewservice.model.Review;
import com.bookverse.reviewservice.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private BookServiceClient bookServiceClient;

    public ReviewResponseDto getReviewById(int id){
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewServiceException("Review not found with id: " + id));
        return reviewMapper.entityToResponseDto(review);
    }

    public ReviewResponseDto createReview(ReviewRequestDto reviewRequestDto, String reviewerId){
        try{
            bookServiceClient.getBookById(reviewRequestDto.getBookId());
            return reviewMapper.entityToResponseDto(reviewRepository.
                    save(reviewMapper.requestDtoToEntity(reviewRequestDto,reviewerId)));
        }catch (Exception e){
            throw new ReviewServiceException("Failed to create review",e);
        }
    }

    public List<ReviewResponseDto> getReviewsByBookId(int bookId){
        try {
            bookServiceClient.getBookById(bookId);
            List<Review> reviews = reviewRepository.findByBookId(bookId);
            List<ReviewResponseDto> reviewResponseDtos = new ArrayList<>();
            for (Review review : reviews) {
                reviewResponseDtos.add(reviewMapper.entityToResponseDto(review));
            }
            return reviewResponseDtos;
        } catch (Exception e) {
            throw new ReviewServiceException("Failed to fetch reviews for bookId: " + bookId, e);
        }
    }

    public ReviewResponseDto updateReview(int id, ReviewRequestDto reviewRequestDto, String reviewerId){
        Review existingReview = reviewRepository.findById(id).orElseThrow(() ->
                new ReviewServiceException("Review not found with id: " + id));

        if (!existingReview.getReviewer().equals(reviewerId)) {
            throw new ReviewServiceException("Unauthorized to update this review");
        }

        existingReview.setReviewText(reviewRequestDto.getReviewText());
        existingReview.setRating(reviewRequestDto.getRating());
        Review updatedReview;
        try{
            updatedReview = reviewRepository.save(existingReview);
        }catch (Exception e){
            throw new ReviewServiceException("Failed to update review",e);
        }
        return reviewMapper.entityToResponseDto(updatedReview);
    }

    public String deleteReview(int id, String reviewerId){
        Review existingReview = reviewRepository.findById(id).orElseThrow(() ->
                new ReviewServiceException("Review not found with id: " + id));

        if (!existingReview.getReviewer().equals(reviewerId)) {
            throw new ReviewServiceException("Unauthorized to delete this review");
        }
        try{
            reviewRepository.deleteById(id);
            return "Review deleted successfully";
        }catch (Exception e){
            throw new ReviewServiceException("Failed to delete review",e);
        }
    }
}

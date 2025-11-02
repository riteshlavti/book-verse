//package com.bookverse.bookservice.client;
//
//import com.bookverse.bookservice.dto.ReviewDto;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.util.List;
//
//@FeignClient("review-service")
//public interface ReviewServiceClient {
//
//    @GetMapping("")
//    public double getBookRating(@RequestParam int id);
//
//    @GetMapping("")
//    public List<ReviewDto> getBookReviews(@RequestParam int id);
//}

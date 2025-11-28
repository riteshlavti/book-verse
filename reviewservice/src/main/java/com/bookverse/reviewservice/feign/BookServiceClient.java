package com.bookverse.reviewservice.feign;

import com.bookverse.reviewservice.dto.BookResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookServiceClient {
    @GetMapping("/api/book/{id}")
    BookResponseDto getBookById(@PathVariable("id") int bookId);
}


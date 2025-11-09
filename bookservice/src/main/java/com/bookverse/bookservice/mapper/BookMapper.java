package com.bookverse.bookservice.mapper;

import com.bookverse.bookservice.dto.BookRequestDto;
import com.bookverse.bookservice.dto.BookResponseDto;
import com.bookverse.bookservice.model.Book;
import org.springframework.stereotype.Component;


@Component
public class BookMapper {
    public Book toEntity(BookRequestDto dto) {
        return Book.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .genre(dto.getGenre())
                .publishedDate(dto.getPublishedDate())
                .build();
    }

    public BookResponseDto entityToDto(Book book) {
        return BookResponseDto.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genre(book.getGenre())
                .published_date(book.getPublishedDate())
                .build();
    }
}

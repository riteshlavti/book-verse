package com.bookverse.bookservice.service;

import com.bookverse.bookservice.dto.BookRequestDto;
import com.bookverse.bookservice.dto.BookResponseDto;
//import com.bookverse.bookservice.client.ReviewServiceClient;
import com.bookverse.bookservice.mapper.BookMapper;
import com.bookverse.bookservice.model.Book;
import com.bookverse.bookservice.repository.BookRepository;
import com.bookverse.bookservice.exception.BookServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookCrudService {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    BookMapper bookMapper;

    public BookResponseDto getBookById(int id) {
        Book book;
        try{
            book = bookRepository.findById(id).orElse(null);
        } catch (Exception e) {
            throw new BookServiceException("Failed to fetch book by id: " + id, e);
        }
        if (book == null) {
            throw new BookServiceException("Book with id " + id + " not found.");
        }
        return bookMapper.entityToDto(book);
    }

    public String addBook(BookRequestDto book) {
        try {
            bookRepository.save(bookMapper.toEntity(book));
        } catch (Exception e) {
            throw new BookServiceException("Failed to add book: " + book.getTitle(), e);
        }
        return "Book - "+book.getTitle()+"  added successfully.";
    }

    public String updateBookDetails(int id, BookRequestDto bookRequestDto) {
        try {
            Book existingBook = bookRepository.findById(id).orElse(null);
            if (existingBook != null) {
                Book updatedBook = bookMapper.toEntity(bookRequestDto);
                updatedBook.setBookId(id);
                bookRepository.save(updatedBook);
                return "Book updated successfully.";
            } else {
                throw new BookServiceException("Book not found");
            }
        } catch (Exception e) {
            throw new BookServiceException("Failed to update book: " + id, e);
        }
    }

    public String deleteBook(int id){
        try {
            if(!bookRepository.existsById(id)) {
                return "Book with id "+id+" not found.";
            }
            else {
                bookRepository.deleteById(id);
                return "Book with id " + id + " deleted successfully.";
            }
        } catch (Exception e) {
            throw new BookServiceException("Failed to delete book with id: " + id, e);
        }
    }
}

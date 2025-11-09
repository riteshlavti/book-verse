package com.bookverse.bookservice.service;

import com.bookverse.bookservice.dto.BookRequestDto;
import com.bookverse.bookservice.dto.BookResponseDto;
import com.bookverse.bookservice.exception.BookServiceException;
import com.bookverse.bookservice.mapper.BookMapper;
import com.bookverse.bookservice.model.Book;
import com.bookverse.bookservice.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookCrudServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookCrudService bookCrudService;

    @Test
    void testGetBookById_Success() {
        int bookId = 1;
        Book book = new Book();
        book.setBookId(bookId);
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setGenre("Software");
        book.setPublishedDate(LocalDate.of(2008, 8, 1));

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        BookResponseDto response = bookCrudService.getBookById(bookId);

        assertNotNull(response);
        assertEquals("Clean Code", response.getTitle());
        assertEquals("Robert C. Martin", response.getAuthor());
        verify(bookRepository, times(1)).findById(bookId);
    }


    @Test
    public void testGetBookById_NotFound() {
        int bookId = 99;
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(BookServiceException.class, () -> bookCrudService.getBookById(bookId));
        assertNotNull(exception.getMessage());
        verify(bookRepository, times(1)).findById(bookId);
    }

    @Test
    public void testAddBook_Success() {
        BookRequestDto bookRequestDto = new BookRequestDto();
        bookRequestDto.setTitle("Effective Java");
        bookRequestDto.setAuthor("Joshua Bloch");
        bookRequestDto.setGenre("Programming");
        bookRequestDto.setPublishedDate(LocalDate.of(2018, 1, 6));

        Book book = new Book();
        book.setTitle(bookRequestDto.getTitle());
        book.setAuthor(bookRequestDto.getAuthor());
        book.setGenre(bookRequestDto.getGenre());
        book.setPublishedDate(bookRequestDto.getPublishedDate());

        when(bookMapper.toEntity(bookRequestDto)).thenReturn(book);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        String result = bookCrudService.addBook(bookRequestDto);

        assertNotNull(result);
        verify(bookMapper, times(1)).toEntity(bookRequestDto);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    public void testAddBook_Failure() {
        BookRequestDto bookRequestDto = new BookRequestDto();
        Book book = new Book();
        when(bookMapper.toEntity(bookRequestDto)).thenReturn(book);
        when(bookRepository.save(any(Book.class))).thenThrow(new RuntimeException("DB error"));

        Exception exception = assertThrows(BookServiceException.class, () -> bookCrudService.addBook(bookRequestDto));

        assertNotNull(exception.getMessage());
        verify(bookMapper, times(1)).toEntity(bookRequestDto);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    public void testUpdateBookDetails_Success() {
        int bookId = 1;
        BookRequestDto bookRequestDto = new BookRequestDto();
        bookRequestDto.setTitle("Refactoring");
        bookRequestDto.setAuthor("Martin Fowler");
        bookRequestDto.setGenre("Software");
        bookRequestDto.setPublishedDate(LocalDate.of(1999, 7, 8));

        Book book = new Book();
        book.setBookId(bookId);
        book.setTitle(bookRequestDto.getTitle());
        book.setAuthor(bookRequestDto.getAuthor());
        book.setGenre(bookRequestDto.getGenre());
        book.setPublishedDate(bookRequestDto.getPublishedDate());

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookMapper.toEntity(bookRequestDto)).thenReturn(book);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        String result = bookCrudService.updateBookDetails(bookId, bookRequestDto);

        assertNotNull(result);
        verify(bookRepository, times(1)).findById(bookId);
        verify(bookMapper, times(1)).toEntity(bookRequestDto);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    public void testUpdateBookDetails_Failure() {
        int bookId = 2;
        BookRequestDto bookRequestDto = new BookRequestDto();

        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(BookServiceException.class, () -> bookCrudService.updateBookDetails(bookId, bookRequestDto));

        assertNotNull(exception.getMessage());
        verify(bookRepository, times(1)).findById(bookId);
    }

    @Test
    public void testDeleteBook_Success() {
        int bookId = 1;
        when(bookRepository.existsById(bookId)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(bookId);

        String result = bookCrudService.deleteBook(bookId);

        assertNotNull(result);
        verify(bookRepository, times(1)).existsById(bookId);
        verify(bookRepository, times(1)).deleteById(bookId);
    }

    @Test
    public void testDeleteBook_Failure() {
        int bookId = 2;
        when(bookRepository.existsById(bookId)).thenReturn(false);

        String result = bookCrudService.deleteBook(bookId);
        assertEquals("Book with id 2 not found.", result);
        verify(bookRepository, times(1)).existsById(bookId);
    }

}

package com.bookverse.bookservice.repository;

import com.bookverse.bookservice.model.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    // Search across multiple fields (title, author, genre)
    @Query(value = "SELECT * FROM book WHERE MATCH(title, author, genre) AGAINST (:query IN BOOLEAN MODE)",
            nativeQuery = true)
    List<Book> fullTextSearch(String query, Pageable pageable);

    // Search by author
    List<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);

    // Search by genre
    List<Book> findByGenreContainingIgnoreCase(String genre, Pageable pageable);

    // Search by author + genre
    List<Book> findByAuthorContainingIgnoreCaseAndGenreContainingIgnoreCase(String author, String genre, Pageable pageable);
}

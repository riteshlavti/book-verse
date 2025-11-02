package com.bookverse.bookservice.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class BookRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest(name = "Invalid DTO: title={0}, author={1}, genre={2}, publishedDate={3}")
    @MethodSource("invalidDtos")
    void testInvalidBookRequestDtos(String title, String author, String genre, LocalDate publishedDate) {
        BookRequestDto dto = new BookRequestDto();
        dto.setTitle(title);
        dto.setAuthor(author);
        dto.setGenre(genre);
        dto.setPublishedDate(publishedDate);

        Set<ConstraintViolation<BookRequestDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(), "Expected validation errors but got none.");
        violations.forEach(v ->
                System.out.println(v.getPropertyPath() + " → " + v.getMessage()));
    }

    private static Stream<Arguments> invalidDtos() {
        return Stream.of(
                Arguments.of(null, "Author", "Genre", LocalDate.now()),
                Arguments.of("", "Author", "Genre", LocalDate.now()),
                Arguments.of("Title", null, "Genre", LocalDate.now()),
                Arguments.of("Title", "", "Genre", LocalDate.now()),
                Arguments.of("Title", "Author", "Genre", LocalDate.now().plusDays(1))
        );
    }
}

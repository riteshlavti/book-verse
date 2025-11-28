package com.bookverse.reviewservice.dto;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookResponseDto {
    private int bookId;
    private String title;
}

package dev.shrekback.post.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DatePeriodDto {
	LocalDate dateFrom;
    LocalDate dateTo;
}

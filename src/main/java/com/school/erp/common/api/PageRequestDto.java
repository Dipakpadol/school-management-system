package com.school.erp.common.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public record PageRequestDto(
		@Min(0) Integer page,
		@Min(1) @Max(200) Integer size,
		String sortBy,
		Sort.Direction direction) {

	public PageRequestDto {
		page = page == null ? 0 : page;
		size = size == null ? 20 : size;
		direction = direction == null ? Sort.Direction.ASC : direction;
	}

	public Pageable toPageable(String defaultSort) {
		String sortProperty = StringUtils.hasText(sortBy) ? sortBy : defaultSort;
		return PageRequest.of(page, size, Sort.by(direction, sortProperty));
	}
}

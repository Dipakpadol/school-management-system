package com.school.erp.common.mapper;

import java.util.Collection;
import java.util.List;

public interface DtoMapper<E, D> {

	D toDto(E entity);

	E toEntity(D dto);

	default List<D> toDtoList(Collection<E> entities) {
		return entities.stream().map(this::toDto).toList();
	}
}

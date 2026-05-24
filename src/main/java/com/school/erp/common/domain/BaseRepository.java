package com.school.erp.common.domain;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {

	Optional<T> findByIdAndDeletedFalse(ID id);

	long countByDeletedFalse();

	Page<T> findAllByDeletedFalse(Pageable pageable);

	@Transactional
	default T softDelete(T entity, String actor) {
		entity.softDelete(actor);
		return save(entity);
	}
}

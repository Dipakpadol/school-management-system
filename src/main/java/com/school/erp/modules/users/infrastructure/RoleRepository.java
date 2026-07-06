package com.school.erp.modules.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;

import org.springframework.data.jpa.repository.EntityGraph;

public interface RoleRepository extends BaseRepository<Role, UUID> {

	@EntityGraph(attributePaths = "permissions")
	Optional<Role> findByNameIgnoreCaseAndDeletedFalse(String name);

	boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

	default Optional<Role> findByNameAndDeletedFalse(RoleName name) {
		return findByNameIgnoreCaseAndDeletedFalse(name.name());
	}
}

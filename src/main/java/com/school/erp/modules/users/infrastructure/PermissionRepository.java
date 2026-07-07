package com.school.erp.modules.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.users.domain.Permission;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends BaseRepository<Permission, UUID> {

	Optional<Permission> findByCodeAndDeletedFalse(String code);

	Optional<Permission> findByCodeIgnoreCaseAndDeletedFalse(String code);

	boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

	@Query("""
			select count(role)
			from Role role
			join role.permissions permission
			where role.deleted = false
			  and role.status = com.school.erp.modules.users.domain.RoleStatus.ACTIVE
			  and permission.id = :permissionId
			""")
	long countActiveRoleAssignments(@Param("permissionId") UUID permissionId);
}

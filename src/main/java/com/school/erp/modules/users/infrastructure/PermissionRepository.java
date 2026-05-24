package com.school.erp.modules.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.users.domain.Permission;

public interface PermissionRepository extends BaseRepository<Permission, UUID> {

	Optional<Permission> findByCodeAndDeletedFalse(String code);
}

package com.school.erp.modules.users.infrastructure;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.domain.UserStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends BaseRepository<UserAccount, UUID>, JpaSpecificationExecutor<UserAccount> {

	@EntityGraph(attributePaths = { "roles", "roles.permissions" })
	Optional<UserAccount> findByEmailIgnoreCaseAndDeletedFalse(String email);

	@EntityGraph(attributePaths = { "roles", "roles.permissions" })
	Optional<UserAccount> findWithRolesByIdAndDeletedFalse(UUID id);

	boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

	boolean existsByUsernameIgnoreCaseAndDeletedFalse(String username);

	boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

	long countByStatusAndDeletedFalse(UserStatus status);

	@Query("""
			select count(distinct user)
			from UserAccount user
			join user.roles role
			where user.deleted = false
			  and role.name in :roleNames
			""")
	long countByRoleNamesAndDeletedFalse(@Param("roleNames") Collection<RoleName> roleNames);
}

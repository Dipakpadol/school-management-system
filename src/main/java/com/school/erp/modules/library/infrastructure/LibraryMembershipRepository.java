package com.school.erp.modules.library.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryMemberType;
import com.school.erp.modules.library.domain.LibraryMembership;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryMembershipRepository extends BaseRepository<LibraryMembership, UUID> {

	Optional<LibraryMembership> findByMembershipNumberIgnoreCaseAndDeletedFalse(String membershipNumber);

	long countByActiveTrueAndDeletedFalse();

	@Query("""
			select membership
			from LibraryMembership membership
			where membership.deleted = false
			  and (:memberType is null or membership.memberType = :memberType)
			  and (:active is null or membership.active = :active)
			  and (:keyword is null
				or lower(membership.membershipNumber) like lower(concat('%', :keyword, '%')))
			""")
	Page<LibraryMembership> search(
			@Param("memberType") LibraryMemberType memberType,
			@Param("active") Boolean active,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query("""
			select count(membership) > 0
			from LibraryMembership membership
			where membership.deleted = false
			  and membership.active = true
			  and membership.memberType = :memberType
			  and (:excludedId is null or membership.id <> :excludedId)
			  and ((:studentId is not null and membership.studentId = :studentId)
				or (:teacherId is not null and membership.teacherId = :teacherId)
				or (:staffId is not null and membership.staffId = :staffId))
			""")
	boolean existsActiveMembershipForMember(
			@Param("memberType") LibraryMemberType memberType,
			@Param("studentId") UUID studentId,
			@Param("teacherId") UUID teacherId,
			@Param("staffId") UUID staffId,
			@Param("excludedId") UUID excludedId);
}

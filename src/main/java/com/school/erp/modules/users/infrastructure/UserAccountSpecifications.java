package com.school.erp.modules.users.infrastructure;

import java.util.ArrayList;
import java.util.List;

import com.school.erp.modules.users.api.dto.UserSearchRequest;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.UserAccount;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserAccountSpecifications {

	private UserAccountSpecifications() {
	}

	public static Specification<UserAccount> matching(UserSearchRequest request) {
		return (root, query, criteriaBuilder) -> {
			query.distinct(true);
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

			if (StringUtils.hasText(request.query())) {
				String text = contains(request.query());
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), text)));
			}
			if (request.status() != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), request.status()));
			}
			if (request.role() != null) {
				Join<UserAccount, Role> roleJoin = root.join("roles", JoinType.INNER);
				predicates.add(criteriaBuilder.equal(roleJoin.get("name"), Role.normalizeRoleName(request.role())));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String contains(String value) {
		return "%" + value.trim().toLowerCase() + "%";
	}
}

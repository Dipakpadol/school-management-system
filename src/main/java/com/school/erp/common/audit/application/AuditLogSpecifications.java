package com.school.erp.common.audit.application;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.school.erp.common.audit.api.dto.AuditLogSearchRequest;
import com.school.erp.common.audit.domain.AuditLog;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class AuditLogSpecifications {

	private AuditLogSpecifications() {
	}

	static Specification<AuditLog> matching(AuditLogSearchRequest request) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (StringUtils.hasText(request.query())) {
				String text = contains(request.query());
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("moduleName")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("entityName")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("entityId")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("action")), text),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("performedBy")), text)));
			}
			if (StringUtils.hasText(request.moduleName())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("moduleName")),
						request.moduleName().trim().toLowerCase()));
			}
			if (StringUtils.hasText(request.entityName())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("entityName")),
						request.entityName().trim().toLowerCase()));
			}
			if (StringUtils.hasText(request.entityId())) {
				predicates.add(criteriaBuilder.equal(root.get("entityId"), request.entityId().trim()));
			}
			if (StringUtils.hasText(request.action())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("action")),
						request.action().trim().toLowerCase()));
			}
			if (StringUtils.hasText(request.performedBy())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("performedBy")),
						request.performedBy().trim().toLowerCase()));
			}
			Instant performedFrom = request.performedFrom() != null
					? request.performedFrom()
					: request.fromDate() == null
							? null
							: request.fromDate().atStartOfDay().toInstant(ZoneOffset.UTC);
			Instant performedTo = request.performedTo() != null
					? request.performedTo()
					: request.toDate() == null
							? null
							: request.toDate().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
			if (performedFrom != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("performedAt"), performedFrom));
			}
			if (performedTo != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("performedAt"), performedTo));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String contains(String value) {
		return "%" + value.trim().toLowerCase() + "%";
	}
}

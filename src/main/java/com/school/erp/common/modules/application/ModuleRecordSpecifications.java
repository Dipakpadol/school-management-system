package com.school.erp.common.modules.application;

import java.util.ArrayList;
import java.util.List;

import com.school.erp.common.modules.api.dto.ModuleRecordSearchRequest;
import com.school.erp.common.modules.domain.ModuleRecord;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class ModuleRecordSpecifications {

	private ModuleRecordSpecifications() {
	}

	static Specification<ModuleRecord> matching(
			String moduleName,
			String recordType,
			ModuleRecordSearchRequest request) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("moduleName"), ModuleRecord.normalizeModule(moduleName)));
			predicates.add(criteriaBuilder.equal(root.get("recordType"), ModuleRecord.normalizeType(recordType)));

			if (StringUtils.hasText(request.query())) {
				String queryText = "%" + request.query().trim().toLowerCase() + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), queryText)));
			}
			if (StringUtils.hasText(request.status())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("status")),
						request.status().trim().toLowerCase()));
			}
			if (request.parentId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("parentId"), request.parentId()));
			}
			if (request.ownerId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("ownerId"), request.ownerId()));
			}
			if (request.active() != null) {
				predicates.add(criteriaBuilder.equal(root.get("active"), request.active()));
			}
			if (request.fromDate() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("recordDate"), request.fromDate()));
			}
			if (request.toDate() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("recordDate"), request.toDate()));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}

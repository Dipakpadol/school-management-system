package com.school.erp.modules.students.infrastructure;

import java.util.ArrayList;
import java.util.List;

import com.school.erp.modules.students.api.dto.StudentSearchRequest;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class StudentSpecifications {

	private StudentSpecifications() {
	}

	public static Specification<Student> matching(StudentSearchRequest request) {
		return (root, query, criteriaBuilder) -> {
			query.distinct(true);
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

			if (StringUtils.hasText(request.query())) {
				String queryText = contains(request.query());
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("admissionNumber")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("middleName")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), queryText),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), queryText)));
			}

			if (request.status() != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), request.status()));
			}
			if (StringUtils.hasText(request.admissionNumber())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("admissionNumber")),
						request.admissionNumber().trim().toLowerCase()));
			}
			if (request.admittedFrom() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("admissionDate"), request.admittedFrom()));
			}
			if (request.admittedTo() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("admissionDate"), request.admittedTo()));
			}

			Join<Student, StudentClassAssignment> assignmentJoin = null;
			if (StringUtils.hasText(request.className()) || StringUtils.hasText(request.sectionName())) {
				assignmentJoin = root.join("classAssignments", JoinType.LEFT);
				predicates.add(criteriaBuilder.isTrue(assignmentJoin.get("active")));
			}
			if (StringUtils.hasText(request.className())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(assignmentJoin.get("className")),
						request.className().trim().toLowerCase()));
			}
			if (StringUtils.hasText(request.sectionName())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(assignmentJoin.get("sectionName")),
						request.sectionName().trim().toLowerCase()));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String contains(String value) {
		return "%" + value.trim().toLowerCase() + "%";
	}
}

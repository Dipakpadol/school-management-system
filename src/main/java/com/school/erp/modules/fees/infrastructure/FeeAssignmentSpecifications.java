package com.school.erp.modules.fees.infrastructure;

import java.util.ArrayList;
import java.util.List;

import com.school.erp.modules.fees.api.dto.FeeAssignmentSearchRequest;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.FeeStructureItem;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class FeeAssignmentSpecifications {

	private FeeAssignmentSpecifications() {
	}

	public static Specification<StudentFeeAssignment> matching(FeeAssignmentSearchRequest request) {
		return (root, query, criteriaBuilder) -> {
			query.distinct(true);
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

			Join<StudentFeeAssignment, Student> student = root.join("student", JoinType.INNER);
			predicates.add(criteriaBuilder.isFalse(student.get("deleted")));
			Join<StudentFeeAssignment, FeeStructure> feeStructure = null;
			if (request.studentId() != null) {
				predicates.add(criteriaBuilder.equal(student.get("id"), request.studentId()));
			}
			if (request.academicYearId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("academicYearEntity").get("id"), request.academicYearId()));
			}
			if (request.classId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("classEntity").get("id"), request.classId()));
			}
			if (request.sectionId() != null) {
				Join<Student, StudentClassAssignment> classAssignment = student.join("classAssignments", JoinType.LEFT);
				predicates.add(criteriaBuilder.isFalse(classAssignment.get("deleted")));
				predicates.add(criteriaBuilder.isTrue(classAssignment.get("active")));
				predicates.add(criteriaBuilder.equal(classAssignment.get("sectionEntity").get("id"), request.sectionId()));
				if (request.academicYearId() != null) {
					predicates.add(criteriaBuilder.equal(
							classAssignment.get("academicYearEntity").get("id"),
							request.academicYearId()));
				}
				else {
					predicates.add(criteriaBuilder.or(
							criteriaBuilder.isNull(root.get("academicYearEntity")),
							criteriaBuilder.equal(
									classAssignment.get("academicYearEntity").get("id"),
									root.get("academicYearEntity").get("id"))));
				}
				if (request.classId() != null) {
					predicates.add(criteriaBuilder.equal(classAssignment.get("classEntity").get("id"), request.classId()));
				}
				else {
					predicates.add(criteriaBuilder.or(
							criteriaBuilder.isNull(root.get("classEntity")),
							criteriaBuilder.equal(
									classAssignment.get("classEntity").get("id"),
									root.get("classEntity").get("id"))));
				}
			}
			if (request.feeStructureId() != null) {
				feeStructure = feeStructure == null ? root.join("feeStructure", JoinType.INNER) : feeStructure;
				predicates.add(criteriaBuilder.equal(feeStructure.get("id"), request.feeStructureId()));
			}
			if (request.feeCategoryId() != null) {
				feeStructure = feeStructure == null ? root.join("feeStructure", JoinType.INNER) : feeStructure;
				Join<FeeStructure, FeeStructureItem> item = feeStructure.join("items", JoinType.INNER);
				predicates.add(criteriaBuilder.isFalse(item.get("deleted")));
				predicates.add(criteriaBuilder.equal(item.get("category").get("id"), request.feeCategoryId()));
			}
			if (StringUtils.hasText(request.admissionNumber())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(student.get("admissionNumber")),
						request.admissionNumber().trim().toLowerCase()));
			}
			if (StringUtils.hasText(request.studentName())) {
				String name = contains(request.studentName());
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(student.get("admissionNumber")), name),
						criteriaBuilder.like(criteriaBuilder.lower(student.get("firstName")), name),
						criteriaBuilder.like(criteriaBuilder.lower(student.get("middleName")), name),
						criteriaBuilder.like(criteriaBuilder.lower(student.get("lastName")), name)));
			}
			if (StringUtils.hasText(request.academicYear())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("academicYear")),
						request.academicYear().trim().toLowerCase()));
			}
			if (StringUtils.hasText(request.className())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("className")),
						request.className().trim().toLowerCase()));
			}
			if (StringUtils.hasText(request.sectionName())) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("sectionName")),
						request.sectionName().trim().toLowerCase()));
			}
			if (request.sourceType() != null) {
				predicates.add(criteriaBuilder.equal(root.get("sourceType"), request.sourceType()));
			}
			if (request.status() != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), request.status()));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String contains(String value) {
		return "%" + value.trim().toLowerCase() + "%";
	}
}

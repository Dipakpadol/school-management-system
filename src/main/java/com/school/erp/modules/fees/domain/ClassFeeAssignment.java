package com.school.erp.modules.fees.domain;

import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "class_fee_assignments")
@SQLRestriction("deleted = false")
public class ClassFeeAssignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private ClassEntity classEntity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fee_structure_id", nullable = false)
	private FeeStructure feeStructure;

	@Column(name = "assigned_date", nullable = false)
	private LocalDate assignedDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ClassFeeAssignmentStatus status = ClassFeeAssignmentStatus.ACTIVE;

	@Column(name = "assigned_by", length = 100)
	private String assignedBy;

	public ClassFeeAssignment(
			AcademicYear academicYear,
			ClassEntity classEntity,
			FeeStructure feeStructure,
			LocalDate assignedDate,
			String assignedBy) {
		this.academicYear = academicYear;
		this.classEntity = classEntity;
		this.feeStructure = feeStructure;
		this.assignedDate = assignedDate == null ? LocalDate.now() : assignedDate;
		this.assignedBy = trimToNull(assignedBy);
		this.status = ClassFeeAssignmentStatus.ACTIVE;
	}

	public boolean isActive() {
		return status == ClassFeeAssignmentStatus.ACTIVE;
	}

	public void deactivate() {
		status = ClassFeeAssignmentStatus.INACTIVE;
	}

	public void cancel() {
		status = ClassFeeAssignmentStatus.CANCELLED;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}

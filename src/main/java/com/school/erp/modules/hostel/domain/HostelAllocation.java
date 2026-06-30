package com.school.erp.modules.hostel.domain;

import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.students.domain.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hostel_allocations")
@SQLRestriction("deleted = false")
public class HostelAllocation extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hostel_id", nullable = false)
	private Hostel hostel;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private HostelRoom room;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bed_id")
	private HostelBed bed;

	@Column(name = "allocation_date", nullable = false)
	private LocalDate allocationDate;

	@Column(name = "vacate_date")
	private LocalDate vacateDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private HostelAllocationStatus status = HostelAllocationStatus.ACTIVE;

	public HostelAllocation(
			Student student,
			AcademicYear academicYear,
			Hostel hostel,
			HostelRoom room,
			HostelBed bed,
			LocalDate allocationDate) {
		this.student = student;
		this.academicYear = academicYear;
		this.hostel = hostel;
		this.room = room;
		this.bed = bed;
		this.allocationDate = allocationDate;
	}

	public boolean isActive() {
		return status == HostelAllocationStatus.ACTIVE;
	}

	public void transfer(LocalDate transferDate) {
		this.vacateDate = transferDate;
		this.status = HostelAllocationStatus.TRANSFERRED;
	}

	public void vacate(LocalDate vacateDate) {
		this.vacateDate = vacateDate;
		this.status = HostelAllocationStatus.VACATED;
	}
}

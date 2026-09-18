package com.school.erp.modules.staff.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.students.domain.Gender;

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
@Table(name = "staff")
@SQLRestriction("deleted = false")
public class Staff extends BaseEntity {

	@Column(name = "employee_code", nullable = false, length = 40)
	private String employeeCode;

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "middle_name", length = 80)
	private String middleName;

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Gender gender;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@Column(length = 160)
	private String email;

	@Column(name = "phone_number", length = 30)
	private String phoneNumber;

	@Column(name = "user_account_id")
	private UUID userAccountId;

	@Column(name = "teacher_id")
	private UUID teacherId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	private Department department;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "designation_id")
	private Designation designation;

	@Column(name = "joining_date", nullable = false)
	private LocalDate joiningDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "staff_type", nullable = false, length = 30)
	private StaffType staffType = StaffType.NON_TEACHING;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EmploymentStatus status = EmploymentStatus.ACTIVE;

	@Column(name = "relieving_date")
	private LocalDate relievingDate;

	@Column(name = "exit_reason", length = 500)
	private String exitReason;

	public Staff(
			String employeeCode,
			String firstName,
			String middleName,
			String lastName,
			Gender gender,
			LocalDate dateOfBirth,
			String email,
			String phoneNumber,
			UUID userAccountId,
			UUID teacherId,
			Department department,
			Designation designation,
			LocalDate joiningDate,
			StaffType staffType,
			EmploymentStatus status) {
		updateProfile(
				employeeCode,
				firstName,
				middleName,
				lastName,
				gender,
				dateOfBirth,
				email,
				phoneNumber,
				userAccountId,
				teacherId,
				department,
				designation,
				joiningDate,
				staffType,
				status);
	}

	public String getDisplayName() {
		StringBuilder name = new StringBuilder(firstName == null ? "" : firstName);
		if (StringUtils.hasText(middleName)) {
			name.append(" ").append(middleName);
		}
		if (StringUtils.hasText(lastName)) {
			name.append(" ").append(lastName);
		}
		return name.toString().trim();
	}

	public boolean isActive() {
		return status == EmploymentStatus.ACTIVE;
	}

	public boolean isEligibleOn(LocalDate date) {
		return isActive()
				&& (joiningDate == null || !joiningDate.isAfter(date))
				&& (relievingDate == null || !relievingDate.isBefore(date));
	}

	public void updateProfile(
			String employeeCode,
			String firstName,
			String middleName,
			String lastName,
			Gender gender,
			LocalDate dateOfBirth,
			String email,
			String phoneNumber,
			UUID userAccountId,
			UUID teacherId,
			Department department,
			Designation designation,
			LocalDate joiningDate,
			StaffType staffType,
			EmploymentStatus status) {
		this.employeeCode = normalizeEmployeeCode(employeeCode);
		this.firstName = trim(firstName);
		this.middleName = trimToNull(middleName);
		this.lastName = trimToNull(lastName);
		this.gender = gender;
		this.dateOfBirth = dateOfBirth;
		this.email = normalizeEmail(email);
		this.phoneNumber = trimToNull(phoneNumber);
		this.userAccountId = userAccountId;
		this.teacherId = teacherId;
		this.department = department;
		this.designation = designation;
		this.joiningDate = joiningDate;
		this.staffType = staffType == null ? StaffType.NON_TEACHING : staffType;
		this.status = status == null ? EmploymentStatus.ACTIVE : status;
		if (this.status == EmploymentStatus.ACTIVE) {
			this.relievingDate = null;
			this.exitReason = null;
		}
	}

	public void linkTeacher(UUID teacherId) {
		this.teacherId = teacherId;
		if (teacherId != null) {
			this.staffType = StaffType.TEACHING;
		}
	}

	public void deactivate() {
		this.status = EmploymentStatus.INACTIVE;
	}

	public void exit(LocalDate relievingDate, String reason) {
		this.status = EmploymentStatus.EXITED;
		this.relievingDate = relievingDate;
		this.exitReason = trimToNull(reason);
	}

	private String normalizeEmployeeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String normalizeEmail(String value) {
		return value == null ? null : trimToNull(value.toLowerCase());
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}

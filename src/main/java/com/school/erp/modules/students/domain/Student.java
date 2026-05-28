package com.school.erp.modules.students.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "students")
@SQLRestriction("deleted = false")
public class Student extends BaseEntity {

	@Column(name = "admission_number", nullable = false, unique = true, length = 40)
	private String admissionNumber;

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "middle_name", length = 80)
	private String middleName;

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Gender gender = Gender.UNSPECIFIED;

	@Column(name = "blood_group", length = 10)
	private String bloodGroup;

	@Column(length = 160)
	private String email;

	@Column(name = "phone_number", length = 30)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private StudentStatus status = StudentStatus.ACTIVE;

	@Column(name = "admission_date", nullable = false)
	private LocalDate admissionDate;

	@Column(name = "previous_school", length = 160)
	private String previousSchool;

	@Column(name = "address_line_1", length = 160)
	private String addressLine1;

	@Column(name = "address_line_2", length = 160)
	private String addressLine2;

	@Column(length = 80)
	private String city;

	@Column(length = 80)
	private String state;

	@Column(name = "postal_code", length = 20)
	private String postalCode;

	@Column(length = 80)
	private String country;

	@Column(name = "photo_storage_key", length = 300)
	private String photoStorageKey;

	@Column(name = "photo_url", length = 500)
	private String photoUrl;

	@Column(name = "photo_content_type", length = 120)
	private String photoContentType;

	@Column(name = "photo_file_name", length = 180)
	private String photoFileName;

	@OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
	private Set<StudentParent> parents = new LinkedHashSet<>();

	@OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
	private Set<StudentDocument> documents = new LinkedHashSet<>();

	@OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
	private Set<StudentClassAssignment> classAssignments = new LinkedHashSet<>();

	public Student(
			String admissionNumber,
			String firstName,
			LocalDate dateOfBirth,
			Gender gender,
			LocalDate admissionDate) {
		this.admissionNumber = normalizeAdmissionNumber(admissionNumber);
		this.firstName = firstName;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender == null ? Gender.UNSPECIFIED : gender;
		this.admissionDate = admissionDate;
	}

	public String getDisplayName() {
		StringBuilder displayName = new StringBuilder(firstName);
		if (StringUtils.hasText(middleName)) {
			displayName.append(" ").append(middleName);
		}
		if (StringUtils.hasText(lastName)) {
			displayName.append(" ").append(lastName);
		}
		return displayName.toString();
	}

	public String getFullName() {
		return getDisplayName();
	}

	public Optional<StudentClassAssignment> getCurrentAssignment() {
		return classAssignments.stream()
				.filter(assignment -> !assignment.isDeleted())
				.filter(StudentClassAssignment::isActive)
				.max(Comparator.comparing(
						StudentClassAssignment::getEffectiveFrom,
						Comparator.nullsLast(Comparator.naturalOrder())));
	}

	public void updateProfile(
			String firstName,
			String middleName,
			String lastName,
			LocalDate dateOfBirth,
			Gender gender,
			String bloodGroup,
			String email,
			String phoneNumber,
			LocalDate admissionDate,
			String previousSchool,
			String addressLine1,
			String addressLine2,
			String city,
			String state,
			String postalCode,
			String country) {
		this.firstName = firstName;
		this.middleName = trimToNull(middleName);
		this.lastName = trimToNull(lastName);
		this.dateOfBirth = dateOfBirth;
		this.gender = gender == null ? Gender.UNSPECIFIED : gender;
		this.bloodGroup = trimToNull(bloodGroup);
		this.email = normalizeEmail(email);
		this.phoneNumber = trimToNull(phoneNumber);
		this.admissionDate = admissionDate;
		this.previousSchool = trimToNull(previousSchool);
		this.addressLine1 = trimToNull(addressLine1);
		this.addressLine2 = trimToNull(addressLine2);
		this.city = trimToNull(city);
		this.state = trimToNull(state);
		this.postalCode = trimToNull(postalCode);
		this.country = trimToNull(country);
	}

	public StudentParent addParent(
			ParentGuardian parent,
			ParentRelation relationType,
			boolean primaryContact,
			boolean emergencyContact,
			boolean pickupAllowed) {
		if (primaryContact) {
			parents.forEach(StudentParent::markSecondary);
		}
		StudentParent mapping = new StudentParent(this, parent, relationType, primaryContact, emergencyContact, pickupAllowed);
		parents.add(mapping);
		return mapping;
	}

	public StudentDocument addDocument(
			DocumentType documentType,
			String documentNumber,
			String fileName,
			String contentType,
			Long fileSize,
			String storageKey,
			String fileUrl,
			String remarks) {
		StudentDocument document = new StudentDocument(
				this,
				documentType,
				documentNumber,
				fileName,
				contentType,
				fileSize,
				storageKey,
				fileUrl,
				remarks);
		documents.add(document);
		return document;
	}

	public StudentClassAssignment assignClassSection(
			String academicYear,
			String className,
			String sectionName,
			String rollNumber,
			LocalDate effectiveFrom) {
		LocalDate previousEffectiveTo = effectiveFrom == null ? null : effectiveFrom.minusDays(1);
		classAssignments.stream()
				.filter(StudentClassAssignment::isActive)
				.filter(assignment -> assignment.isForAcademicYear(academicYear))
				.forEach(assignment -> assignment.deactivate(previousEffectiveTo));
		StudentClassAssignment assignment = new StudentClassAssignment(
				this,
				academicYear,
				className,
				sectionName,
				rollNumber,
				effectiveFrom);
		classAssignments.add(assignment);
		return assignment;
	}

	public StudentClassAssignment assignClassSection(
			AcademicYear academicYear,
			ClassEntity classEntity,
			SectionEntity sectionEntity,
			String rollNumber,
			LocalDate effectiveFrom) {
		LocalDate previousEffectiveTo = effectiveFrom == null ? null : effectiveFrom.minusDays(1);
		classAssignments.stream()
				.filter(StudentClassAssignment::isActive)
				.filter(assignment -> assignment.isForAcademicYear(academicYear))
				.forEach(assignment -> assignment.deactivate(previousEffectiveTo));
		StudentClassAssignment assignment = new StudentClassAssignment(
				this,
				academicYear,
				classEntity,
				sectionEntity,
				rollNumber,
				effectiveFrom);
		classAssignments.add(assignment);
		return assignment;
	}

	public void changeStatus(StudentStatus status) {
		this.status = status;
	}

	public void updatePhoto(
			String photoStorageKey,
			String photoUrl,
			String photoContentType,
			String photoFileName) {
		this.photoStorageKey = trimToNull(photoStorageKey);
		this.photoUrl = trimToNull(photoUrl);
		this.photoContentType = trimToNull(photoContentType);
		this.photoFileName = trimToNull(photoFileName);
	}

	public boolean hasParentMapping(ParentGuardian parent, ParentRelation relationType) {
		return parents.stream()
				.anyMatch(mapping -> mapping.matches(parent, relationType));
	}

	private String normalizeAdmissionNumber(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String normalizeEmail(String value) {
		return value == null ? null : trimToNull(value.toLowerCase());
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}

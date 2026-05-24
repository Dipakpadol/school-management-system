package com.school.erp.modules.students.domain;

import java.util.Objects;

import com.school.erp.common.domain.BaseEntity;

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
@Table(name = "student_parents")
@SQLRestriction("deleted = false")
public class StudentParent extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parent_id", nullable = false)
	private ParentGuardian parent;

	@Enumerated(EnumType.STRING)
	@Column(name = "relation_type", nullable = false, length = 30)
	private ParentRelation relationType;

	@Column(name = "primary_contact", nullable = false)
	private boolean primaryContact;

	@Column(name = "emergency_contact", nullable = false)
	private boolean emergencyContact;

	@Column(name = "pickup_allowed", nullable = false)
	private boolean pickupAllowed;

	StudentParent(
			Student student,
			ParentGuardian parent,
			ParentRelation relationType,
			boolean primaryContact,
			boolean emergencyContact,
			boolean pickupAllowed) {
		this.student = student;
		this.parent = parent;
		this.relationType = relationType;
		this.primaryContact = primaryContact;
		this.emergencyContact = emergencyContact;
		this.pickupAllowed = pickupAllowed;
	}

	public void markSecondary() {
		primaryContact = false;
	}

	public void update(
			ParentGuardian parent,
			ParentRelation relationType,
			boolean primaryContact,
			boolean emergencyContact,
			boolean pickupAllowed) {
		this.parent = parent;
		this.relationType = relationType;
		this.primaryContact = primaryContact;
		this.emergencyContact = emergencyContact;
		this.pickupAllowed = pickupAllowed;
	}

	public boolean matches(ParentGuardian parent, ParentRelation relationType) {
		if (parent.getId() == null || this.parent.getId() == null) {
			return false;
		}
		return Objects.equals(this.parent.getId(), parent.getId()) && this.relationType == relationType;
	}
}

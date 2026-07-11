package com.school.erp.modules.fees.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.fees.api.dto.FeeAutoAssignmentResult;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.domain.FeeScope;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;
import com.school.erp.modules.hostel.domain.HostelFeeStructure;
import com.school.erp.modules.hostel.infrastructure.HostelAllocationRepository;
import com.school.erp.modules.hostel.infrastructure.HostelFeeStructureRepository;
import com.school.erp.modules.transport.domain.TransportFeeStructure;
import com.school.erp.modules.transport.domain.TransportStatus;
import com.school.erp.modules.transport.infrastructure.StudentTransportAssignmentRepository;
import com.school.erp.modules.transport.infrastructure.TransportFeeStructureRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentFeeAutoAssignmentService {

	private static final String MODULE_NAME = "FEES";
	private static final String ENTITY_NAME = "StudentFeeAssignment";

	private final AcademicHierarchyService academicHierarchyService;
	private final FeeStructureRepository feeStructureRepository;
	private final HostelFeeStructureRepository hostelFeeStructureRepository;
	private final TransportFeeStructureRepository transportFeeStructureRepository;
	private final HostelAllocationRepository hostelAllocationRepository;
	private final StudentTransportAssignmentRepository transportAssignmentRepository;
	private final StudentFeeAssignmentRepository assignmentRepository;
	private final FeeService feeService;
	private final AuditLogService auditLogService;

	@Transactional
	public FeeAutoAssignmentResult assignClassFees(UUID studentId, UUID academicYearId, UUID classId) {
		return assignClassFees(studentId, academicYearId, classId, LocalDate.now());
	}

	@Transactional
	public FeeAutoAssignmentResult assignClassFees(
			UUID studentId,
			UUID academicYearId,
			UUID classId,
			LocalDate assignedDate) {
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(academicYearId);
		ClassEntity classEntity = academicHierarchyService.loadClass(classId);
		if (!classEntity.getAcademicYear().getId().equals(academicYear.getId())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Class does not belong to the selected academic year.");
		}

		List<UUID> feeStructureIds = feeStructureRepository
				.findByAcademicYearEntityIdAndClassEntityIdAndFeeScopeAndStatusAndDeletedFalseOrderByCreatedAtAsc(
						academicYearId,
						classId,
						FeeScope.CLASS,
						FeeStructureStatus.ACTIVE)
				.stream()
				.map(FeeStructure::getId)
				.toList();
		if (feeStructureIds.isEmpty()) {
			return warning(studentId, FeeScope.CLASS, "No class fee structure found.");
		}

		int skippedDuplicates = auditDuplicateSkips(studentId, feeStructureIds, FeeScope.CLASS);
		List<StudentFeeAssignmentResponse> created = feeService.assignActiveClassFeesToStudent(
				studentId,
				academicYearId,
				classId,
				assignedDate);
		return result(studentId, FeeScope.CLASS, created.size(), skippedDuplicates, List.of());
	}

	@Transactional
	public FeeAutoAssignmentResult assignHostelFees(
			UUID studentId,
			UUID academicYearId,
			UUID hostelId,
			UUID roomId,
			String roomType) {
		return assignHostelFees(studentId, academicYearId, hostelId, roomId, roomType, LocalDate.now());
	}

	@Transactional
	public FeeAutoAssignmentResult assignHostelFees(
			UUID studentId,
			UUID academicYearId,
			UUID hostelId,
			UUID roomId,
			String roomType,
			LocalDate assignedDate) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		if (!hostelAllocationRepository.existsByStudentIdAndAcademicYearIdAndStatusAndDeletedFalse(
				studentId,
				academicYearId,
				HostelAllocationStatus.ACTIVE)) {
			return warning(studentId, FeeScope.HOSTEL, "Hostel allocation is not active.");
		}

		List<UUID> feeStructureIds = hostelFeeStructureRepository.findApplicable(
						academicYearId,
						hostelId,
						roomId,
						normalize(roomType),
						FeeStructureStatus.ACTIVE)
				.stream()
				.map(HostelFeeStructure::getBackingFeeStructure)
				.map(FeeStructure::getId)
				.toList();
		if (feeStructureIds.isEmpty()) {
			return warning(studentId, FeeScope.HOSTEL, "No hostel fee structure found.");
		}

		int skippedDuplicates = auditDuplicateSkips(studentId, feeStructureIds, FeeScope.HOSTEL);
		List<StudentFeeAssignmentResponse> created = feeService.assignActiveHostelFeesToStudent(
				studentId,
				feeStructureIds,
				assignedDate);
		return result(studentId, FeeScope.HOSTEL, created.size(), skippedDuplicates, List.of());
	}

	@Transactional
	public FeeAutoAssignmentResult assignTransportFees(
			UUID studentId,
			UUID academicYearId,
			UUID routeId,
			UUID pickupPointId) {
		return assignTransportFees(studentId, academicYearId, routeId, pickupPointId, LocalDate.now());
	}

	@Transactional
	public FeeAutoAssignmentResult assignTransportFees(
			UUID studentId,
			UUID academicYearId,
			UUID routeId,
			UUID pickupPointId,
			LocalDate assignedDate) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		if (!transportAssignmentRepository.existsByStudentIdAndAcademicYearIdAndStatusAndDeletedFalse(
				studentId,
				academicYearId,
				TransportStatus.ASSIGNED)) {
			return warning(studentId, FeeScope.TRANSPORT, "Transport assignment is not active.");
		}

		List<UUID> feeStructureIds = transportFeeStructureRepository.findApplicable(
						academicYearId,
						routeId,
						pickupPointId,
						FeeStructureStatus.ACTIVE)
				.stream()
				.map(TransportFeeStructure::getBackingFeeStructure)
				.map(FeeStructure::getId)
				.toList();
		if (feeStructureIds.isEmpty()) {
			return warning(studentId, FeeScope.TRANSPORT, "No transport fee structure found.");
		}

		int skippedDuplicates = auditDuplicateSkips(studentId, feeStructureIds, FeeScope.TRANSPORT);
		List<StudentFeeAssignmentResponse> created = feeService.assignActiveTransportFeesToStudent(
				studentId,
				feeStructureIds,
				assignedDate);
		if (!created.isEmpty() || skippedDuplicates > 0) {
			transportAssignmentRepository
					.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAssignmentDateDesc(
							studentId,
							academicYearId,
							TransportStatus.ASSIGNED)
					.ifPresent(assignment -> assignment.markFeeAssigned(true));
		}
		return result(studentId, FeeScope.TRANSPORT, created.size(), skippedDuplicates, List.of());
	}

	@Transactional
	public FeeAutoAssignmentResult assignAllApplicableFees(UUID studentId, StudentFeeAutoAssignmentContext context) {
		FeeAutoAssignmentResult result = FeeAutoAssignmentResult.empty(studentId);
		if (context.academicYearId() != null && context.classId() != null) {
			result = result.merge(assignClassFees(
					studentId,
					context.academicYearId(),
					context.classId(),
					context.assignedDate()));
		}
		if (context.hostelFeeApplicable()
				&& context.academicYearId() != null
				&& context.hostelId() != null) {
			result = result.merge(assignHostelFees(
					studentId,
					context.academicYearId(),
					context.hostelId(),
					context.roomId(),
					context.roomType(),
					context.assignedDate()));
		}
		if (context.transportFeeApplicable()
				&& context.academicYearId() != null
				&& context.routeId() != null) {
			result = result.merge(assignTransportFees(
					studentId,
					context.academicYearId(),
					context.routeId(),
					context.pickupPointId(),
					context.assignedDate()));
		}
		return result;
	}

	private int auditDuplicateSkips(UUID studentId, List<UUID> feeStructureIds, FeeScope sourceType) {
		int skipped = 0;
		for (UUID feeStructureId : feeStructureIds) {
			if (assignmentRepository.existsByStudentIdAndFeeStructureIdAndDeletedFalse(studentId, feeStructureId)) {
				skipped++;
				auditLogService.record(new AuditLogEvent(
						MODULE_NAME,
						ENTITY_NAME,
						studentId.toString(),
						"AUTO_FEE_ASSIGNMENT_DUPLICATE_SKIPPED",
						null,
						java.util.Map.of(
								"studentId", studentId,
								"feeStructureId", feeStructureId,
								"sourceType", sourceType,
								"skipReason", "DUPLICATE_ASSIGNMENT")));
			}
		}
		return skipped;
	}

	private FeeAutoAssignmentResult warning(UUID studentId, FeeScope sourceType, String message) {
		return result(studentId, sourceType, 0, 0, List.of(message));
	}

	private FeeAutoAssignmentResult result(
			UUID studentId,
			FeeScope sourceType,
			int assignedCount,
			int skippedDuplicates,
			List<String> warnings) {
		return new FeeAutoAssignmentResult(
				studentId,
				sourceType == FeeScope.CLASS ? assignedCount : 0,
				sourceType == FeeScope.HOSTEL ? assignedCount : 0,
				sourceType == FeeScope.TRANSPORT ? assignedCount : 0,
				new ArrayList<>(warnings),
				skippedDuplicates);
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
	}

	public record StudentFeeAutoAssignmentContext(
			UUID academicYearId,
			UUID classId,
			UUID hostelId,
			UUID roomId,
			String roomType,
			UUID routeId,
			UUID pickupPointId,
			LocalDate assignedDate,
			boolean hostelFeeApplicable,
			boolean transportFeeApplicable) {

		public StudentFeeAutoAssignmentContext {
			assignedDate = assignedDate == null ? LocalDate.now() : assignedDate;
		}
	}
}

package com.school.erp.modules.hostel.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.fees.application.StudentFeeAutoAssignmentService;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeeScope;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.fees.infrastructure.FeeCategoryRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.hostel.api.dto.ChangeRoomRequest;
import com.school.erp.modules.hostel.api.dto.HostelAllocationResponse;
import com.school.erp.modules.hostel.api.dto.HostelAssignmentRequest;
import com.school.erp.modules.hostel.api.dto.HostelFeeAssignmentRequest;
import com.school.erp.modules.hostel.api.dto.HostelFeeAssignmentResponse;
import com.school.erp.modules.hostel.api.dto.HostelFeeStructureRequest;
import com.school.erp.modules.hostel.api.dto.HostelFeeStructureResponse;
import com.school.erp.modules.hostel.api.dto.HostelRequest;
import com.school.erp.modules.hostel.api.dto.HostelRoomRequest;
import com.school.erp.modules.hostel.api.dto.HostelRoomDetailsResponse;
import com.school.erp.modules.hostel.api.dto.HostelRoomSummaryResponse;
import com.school.erp.modules.hostel.api.dto.HostelStudentRoomResponse;
import com.school.erp.modules.hostel.api.dto.HostelSummaryResponse;
import com.school.erp.modules.hostel.api.dto.RoomStudentAssignmentRequest;
import com.school.erp.modules.hostel.api.dto.VacateHostelRequest;
import com.school.erp.modules.hostel.domain.Hostel;
import com.school.erp.modules.hostel.domain.HostelAllocation;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;
import com.school.erp.modules.hostel.domain.HostelBed;
import com.school.erp.modules.hostel.domain.HostelFeeStructure;
import com.school.erp.modules.hostel.domain.HostelRoom;
import com.school.erp.modules.hostel.infrastructure.HostelAllocationRepository;
import com.school.erp.modules.hostel.infrastructure.HostelBedRepository;
import com.school.erp.modules.hostel.infrastructure.HostelFeeStructureRepository;
import com.school.erp.modules.hostel.infrastructure.HostelRepository;
import com.school.erp.modules.hostel.infrastructure.HostelRoomRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostelService {

	private static final String MODULE_NAME = "HOSTEL";

	private final HostelRepository hostelRepository;
	private final HostelRoomRepository roomRepository;
	private final HostelBedRepository bedRepository;
	private final HostelAllocationRepository allocationRepository;
	private final HostelFeeStructureRepository hostelFeeStructureRepository;
	private final FeeCategoryRepository feeCategoryRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
	private final StudentRepository studentRepository;
	private final AcademicHierarchyService academicHierarchyService;
	private final FeeService feeService;
	private final StudentFeeAutoAssignmentService feeAutoAssignmentService;
	private final HostelMapper hostelMapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<AcademicYearResponse> academicYears() {
		return academicHierarchyService.getAcademicYears();
	}

	@Transactional(readOnly = true)
	public List<HostelSummaryResponse> hostels() {
		return hostels(true);
	}

	@Transactional(readOnly = true)
	public List<HostelSummaryResponse> hostels(boolean activeOnly) {
		List<Hostel> hostels = activeOnly
				? hostelRepository.findAllByActiveTrueAndDeletedFalseOrderByNameAsc()
				: hostelRepository.findAllByDeletedFalseOrderByNameAsc();
		return hostels.stream()
				.map(hostelMapper::toHostelSummary)
				.toList();
	}

	@Transactional
	public HostelSummaryResponse createHostel(HostelRequest request) {
		ensureHostelCodeUnique(request.code(), null);
		Hostel hostel = hostelRepository.save(new Hostel(request.code(), request.name(), request.address()));
		if (!request.activeFlag()) {
			hostel.update(request.code(), request.name(), request.address(), false);
		}
		HostelSummaryResponse response = hostelMapper.toHostelSummary(hostel);
		audit("Hostel", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public HostelSummaryResponse updateHostel(UUID hostelId, HostelRequest request) {
		Hostel hostel = loadHostel(hostelId);
		HostelSummaryResponse oldValue = hostelMapper.toHostelSummary(hostel);
		ensureHostelCodeUnique(request.code(), hostelId);
		hostel.update(request.code(), request.name(), request.address(), request.activeFlag());
		HostelSummaryResponse response = hostelMapper.toHostelSummary(hostel);
		audit("Hostel", response.id(), "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public HostelSummaryResponse deleteHostel(UUID hostelId) {
		Hostel hostel = loadHostel(hostelId);
		if (allocationRepository.existsByHostelIdAndStatusAndDeletedFalse(hostelId, HostelAllocationStatus.ACTIVE)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Hostel with active allocations cannot be deleted.");
		}
		HostelSummaryResponse oldValue = hostelMapper.toHostelSummary(hostel);
		String actor = currentActor();
		hostel.getRooms().stream()
				.filter(room -> !room.isDeleted())
				.forEach(room -> {
					room.getBeds().stream()
							.filter(bed -> !bed.isDeleted())
							.forEach(bed -> bed.softDelete(actor));
					room.softDelete(actor);
				});
		hostel.softDelete(actor);
		audit("Hostel", hostelId, "DELETE", oldValue, Map.of("deleted", true, "hostelId", hostelId));
		return hostelMapper.toHostelSummary(hostel);
	}

	@Transactional(readOnly = true)
	public List<HostelRoomSummaryResponse> allRooms(boolean activeOnly) {
		List<HostelRoom> rooms = activeOnly
				? roomRepository.findByActiveTrueAndDeletedFalseOrderByHostelNameAscRoomNumberAsc()
				: roomRepository.findByDeletedFalseOrderByHostelNameAscRoomNumberAsc();
		return rooms.stream()
				.map(room -> hostelMapper.toRoomSummary(room, 0, Set.of()))
				.toList();
	}

	@Transactional
	public HostelRoomSummaryResponse createRoom(HostelRoomRequest request) {
		Hostel hostel = loadHostel(request.hostelId());
		ensureRoomNumberUnique(hostel.getId(), request.roomNumber(), null);
		HostelRoom room = roomRepository.save(new HostelRoom(
				hostel,
				request.roomNumber(),
				request.roomType(),
				request.capacity(),
				request.usesBeds()));
		if (!request.activeFlag()) {
			room.update(request.roomNumber(), request.roomType(), request.capacity(), request.usesBeds(), false);
		}
		syncBeds(room, request.capacity(), request.usesBeds());
		HostelRoomSummaryResponse response = hostelMapper.toRoomSummary(room, 0, Set.of());
		audit("HostelRoom", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public HostelRoomSummaryResponse updateRoom(UUID roomId, HostelRoomRequest request) {
		HostelRoom room = loadRoom(roomId);
		HostelRoomSummaryResponse oldValue = hostelMapper.toRoomSummary(room, 0, Set.of());
		Hostel hostel = loadHostel(request.hostelId());
		if (!room.getHostel().getId().equals(hostel.getId())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Rooms cannot be moved to another hostel after creation.");
		}
		ensureRoomNumberUnique(hostel.getId(), request.roomNumber(), roomId);
		ensureRoomCapacity(room, request.capacity());
		if (room.isHasBeds() && !request.usesBeds()
				&& allocationRepository.existsActiveBedAllocation(roomId, HostelAllocationStatus.ACTIVE)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Bed concept cannot be disabled while active bed allocations exist.");
		}
		room.update(request.roomNumber(), request.roomType(), request.capacity(), request.usesBeds(), request.activeFlag());
		syncBeds(room, request.capacity(), request.usesBeds());
		HostelRoomSummaryResponse response = hostelMapper.toRoomSummary(room, 0, Set.of());
		audit("HostelRoom", response.id(), "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public HostelRoomSummaryResponse deleteRoom(UUID roomId) {
		HostelRoom room = loadRoom(roomId);
		if (allocationRepository.existsByRoomIdAndStatusAndDeletedFalse(roomId, HostelAllocationStatus.ACTIVE)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Room with active allocations cannot be deleted.");
		}
		HostelRoomSummaryResponse oldValue = hostelMapper.toRoomSummary(room, 0, Set.of());
		String actor = currentActor();
		room.getBeds().stream()
				.filter(bed -> !bed.isDeleted())
				.forEach(bed -> bed.softDelete(actor));
		room.softDelete(actor);
		audit("HostelRoom", roomId, "DELETE", oldValue, Map.of("deleted", true, "roomId", roomId));
		return oldValue;
	}

	@Transactional(readOnly = true)
	public List<HostelSummaryResponse> activeHostels() {
		return hostelRepository.findAllByActiveTrueAndDeletedFalseOrderByNameAsc().stream()
				.map(hostelMapper::toHostelSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<HostelRoomSummaryResponse> roomsForAcademicYear(UUID academicYearId) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		return roomRepository.findByActiveTrueAndDeletedFalseOrderByHostelNameAscRoomNumberAsc().stream()
				.map(room -> hostelMapper.toRoomSummary(
						room,
						occupiedCount(academicYearId, room.getId()),
						occupiedBedIds(academicYearId, room.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public HostelRoomDetailsResponse roomDetails(UUID roomId, UUID academicYearId) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		HostelRoom room = loadRoom(roomId);
		List<HostelStudentRoomResponse> students = roomAllocations(roomId, academicYearId).stream()
				.map(hostelMapper::toStudentRoomResponse)
				.toList();
		return hostelMapper.toRoomDetails(
				room,
				occupiedCount(academicYearId, roomId),
				occupiedBedIds(academicYearId, roomId),
				students);
	}

	@Transactional(readOnly = true)
	public List<HostelStudentRoomResponse> studentsInRoom(UUID roomId, UUID academicYearId) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		loadRoom(roomId);
		return roomAllocations(roomId, academicYearId).stream()
				.map(hostelMapper::toStudentRoomResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<HostelAllocationResponse> studentAllocations(UUID studentId) {
		studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		return allocationRepository.findByStudentIdAndDeletedFalseOrderByAllocationDateDesc(studentId).stream()
				.map(this::toAllocationResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<HostelAllocationResponse> currentStudentAllocation(UUID studentId, UUID academicYearId) {
		studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		if (academicYearId != null) {
			academicHierarchyService.loadAcademicYear(academicYearId);
			return allocationRepository
					.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
							studentId,
							academicYearId,
							HostelAllocationStatus.ACTIVE)
					.map(this::toAllocationResponse);
		}
		return allocationRepository
				.findFirstByStudentIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
						studentId,
						HostelAllocationStatus.ACTIVE)
				.map(this::toAllocationResponse);
	}

	@Transactional
	public HostelAllocationResponse assignStudentToRoom(UUID roomId, RoomStudentAssignmentRequest request) {
		Student student = loadStudent(request.studentId());
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		HostelRoom room = loadRoom(roomId);
		HostelBed bed = resolveBed(room, request.bedId(), request.bedNumber());
		HostelAllocation allocation = createActiveAllocation(
				student,
				academicYear,
				room,
				bed,
				request.allocationDate(),
				true);
		if (request.hostelFeeApplicable()) {
			assignApplicableHostelFees(student, academicYear, room, request.allocationDate());
		}
		HostelAllocationResponse response = toAllocationResponse(allocation);
		audit("HostelAllocation", response.id(), "HOSTEL_ASSIGNED", null, response);
		return response;
	}

	@Transactional
	public Optional<HostelAllocationResponse> assignStudentDuringAdmission(
			Student student,
			AcademicYear fallbackAcademicYear,
			HostelAssignmentRequest request) {
		return assignStudentDuringAdmission(student, fallbackAcademicYear, request, true);
	}

	@Transactional
	public Optional<HostelAllocationResponse> assignStudentDuringAdmission(
			Student student,
			AcademicYear fallbackAcademicYear,
			HostelAssignmentRequest request,
			boolean assignFees) {
		if (!hostelRequested(request)) {
			return Optional.empty();
		}
		AcademicYear academicYear = request.academicYearId() == null
				? fallbackAcademicYear
				: academicHierarchyService.loadAcademicYear(request.academicYearId());
		if (academicYear == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year is required for hostel allocation.");
		}
		HostelRoom room = resolveRoom(request);
		HostelBed bed = resolveBed(room, request.bedId(), request.bedNumber());
		HostelAllocation allocation = createActiveAllocation(
				student,
				academicYear,
				room,
				bed,
				defaultDate(request.allocationDate(), student.getAdmissionDate()),
				true);
		if (assignFees && request.appliesHostelFee()) {
			assignApplicableHostelFees(student, academicYear, room, allocation.getAllocationDate());
		}
		HostelAllocationResponse response = toAllocationResponse(allocation);
		audit("HostelAllocation", response.id(), "HOSTEL_ASSIGNED", null, response);
		return Optional.of(response);
	}

	@Transactional
	public Optional<HostelAllocationResponse> assignStudentByRequest(
			UUID studentId,
			UUID fallbackAcademicYearId,
			HostelAssignmentRequest request) {
		if (!hostelRequested(request)) {
			return Optional.empty();
		}
		Student student = loadStudent(studentId);
		AcademicYear fallbackAcademicYear = fallbackAcademicYearId == null
				? null
				: academicHierarchyService.loadAcademicYear(fallbackAcademicYearId);
		return assignStudentDuringAdmission(student, fallbackAcademicYear, request);
	}

	@Transactional
	public HostelAllocationResponse assignStudentFromProfile(UUID studentId, HostelAssignmentRequest request) {
		return assignStudentByRequest(studentId, null, request)
				.orElseThrow(() -> new BusinessException(
						ErrorCode.VALIDATION_ERROR,
						"Hostel, room, and academic year are required for hostel allocation."));
	}

	@Transactional
	public HostelAllocationResponse changeRoom(UUID allocationId, ChangeRoomRequest request) {
		HostelAllocation current = loadAllocation(allocationId);
		if (!current.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active hostel allocations can be changed.");
		}
		HostelAllocationResponse oldValue = hostelMapper.toAllocationResponse(current);
		HostelRoom room = loadRoom(request.roomId());
		HostelBed bed = resolveBed(room, request.bedId(), request.bedNumber());
		current.transfer(request.allocationDate());
		allocationRepository.flush();
		validateRoomAvailable(current.getAcademicYear(), room, bed);
		HostelAllocation replacement = new HostelAllocation(
				current.getStudent(),
				current.getAcademicYear(),
				room.getHostel(),
				room,
				bed,
				request.allocationDate());
		HostelAllocation saved = allocationRepository.save(replacement);
		feeAutoAssignmentService.reconcileHostelFees(
				current.getStudent().getId(),
				current.getAcademicYear().getId(),
				request.allocationDate(),
				request.hostelFeeApplicable());
		HostelAllocationResponse response = toAllocationResponse(saved);
		audit("HostelAllocation", allocationId, "HOSTEL_ROOM_CHANGED", oldValue, response);
		return response;
	}

	@Transactional
	public HostelAllocationResponse changeStudentRoom(UUID studentId, UUID allocationId, ChangeRoomRequest request) {
		HostelAllocation allocation = loadAllocation(allocationId);
		ensureAllocationBelongsToStudent(allocation, studentId);
		return changeRoom(allocationId, request);
	}

	@Transactional
	public HostelAllocationResponse vacate(UUID allocationId, VacateHostelRequest request) {
		HostelAllocation allocation = loadAllocation(allocationId);
		if (!allocation.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active hostel allocations can be vacated.");
		}
		HostelAllocationResponse oldValue = hostelMapper.toAllocationResponse(allocation);
		allocation.vacate(request.vacateDate());
		feeAutoAssignmentService.reconcileHostelFees(
				allocation.getStudent().getId(),
				allocation.getAcademicYear().getId(),
				request.vacateDate(),
				false);
		HostelAllocationResponse response = toAllocationResponse(allocation);
		audit("HostelAllocation", allocationId, "HOSTEL_VACATED", oldValue, response);
		return response;
	}

	@Transactional
	public HostelAllocationResponse vacateStudentAllocation(UUID studentId, UUID allocationId, VacateHostelRequest request) {
		HostelAllocation allocation = loadAllocation(allocationId);
		ensureAllocationBelongsToStudent(allocation, studentId);
		return vacate(allocationId, request);
	}

	@Transactional
	public HostelFeeStructureResponse createFeeStructure(HostelFeeStructureRequest request) {
		ResolvedHostelFeeStructure resolved = resolveFeeStructureRequest(request);
		validateFeeStructureBusinessRules(null, resolved, request);
		FeeStructure backing = createOrUpdateBackingFeeStructure(null, resolved, request);
		HostelFeeStructure structure = new HostelFeeStructure(
				resolved.academicYear(),
				resolved.hostel(),
				resolved.room(),
				resolved.roomType(),
				resolved.category(),
				backing,
				money(request.amount()),
				request.dueDate(),
				request.installmentAllowed(),
				installmentCount(request),
				statusOrDraft(request.status()));
		HostelFeeStructureResponse response = hostelMapper.toFeeStructureResponse(hostelFeeStructureRepository.save(structure));
		audit("HostelFeeStructure", response.id(), "HOSTEL_FEE_STRUCTURE_CREATED", null, response);
		return response;
	}

	@Transactional
	public HostelFeeStructureResponse updateFeeStructure(UUID structureId, HostelFeeStructureRequest request) {
		HostelFeeStructure structure = loadHostelFeeStructure(structureId);
		HostelFeeStructureResponse oldValue = hostelMapper.toFeeStructureResponse(structure);
		ensureFeeStructureNotAssigned(structure.getBackingFeeStructure().getId());
		ResolvedHostelFeeStructure resolved = resolveFeeStructureRequest(request);
		validateFeeStructureBusinessRules(structureId, resolved, request);
		FeeStructure backing = createOrUpdateBackingFeeStructure(structure.getBackingFeeStructure(), resolved, request);
		structure.update(
				resolved.academicYear(),
				resolved.hostel(),
				resolved.room(),
				resolved.roomType(),
				resolved.category(),
				backing,
				money(request.amount()),
				request.dueDate(),
				request.installmentAllowed(),
				installmentCount(request),
				statusOrDraft(request.status()));
		HostelFeeStructureResponse response = hostelMapper.toFeeStructureResponse(structure);
		audit("HostelFeeStructure", structureId, "HOSTEL_FEE_STRUCTURE_UPDATED", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public HostelFeeStructureResponse getFeeStructure(UUID structureId) {
		return hostelMapper.toFeeStructureResponse(loadHostelFeeStructure(structureId));
	}

	@Transactional(readOnly = true)
	public List<HostelFeeStructureResponse> listFeeStructures(UUID academicYearId, UUID hostelId, String roomType) {
		return hostelFeeStructureRepository.search(academicYearId, hostelId, normalizeRoomType(roomType)).stream()
				.map(hostelMapper::toFeeStructureResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<HostelFeeStructureResponse> listFeeStructures(
			UUID academicYearId,
			UUID hostelId,
			String roomType,
			FeeStructureStatus status,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				hostelFeeStructureRepository.search(
						academicYearId,
						hostelId,
						normalizeRoomType(roomType),
						status,
						pageRequest.toPageable("createdAt")),
				hostelMapper::toFeeStructureResponse);
	}

	@Transactional
	public HostelFeeStructureResponse deleteFeeStructure(UUID structureId) {
		HostelFeeStructure structure = loadHostelFeeStructure(structureId);
		HostelFeeStructureResponse oldValue = hostelMapper.toFeeStructureResponse(structure);
		ensureFeeStructureNotAssigned(structure.getBackingFeeStructure().getId());
		structure.softDelete(currentActor());
		structure.getBackingFeeStructure().softDelete(currentActor());
		audit("HostelFeeStructure", structureId, "DELETE", oldValue, Map.of("deleted", true, "structureId", structureId));
		return hostelMapper.toFeeStructureResponse(structure);
	}

	@Transactional
	public HostelFeeAssignmentResponse assignHostelFees(HostelFeeAssignmentRequest request) {
		LocalDate assignedDate = defaultDate(request.assignedDate(), LocalDate.now());
		if (request.hostelFeeStructureId() != null) {
			HostelFeeStructure structure = loadHostelFeeStructure(request.hostelFeeStructureId());
			StudentFeeAssignmentResponse assignment = feeService.assignHostelFeeToStudent(
					request.studentId(),
					structure.getBackingFeeStructure().getId(),
					assignedDate,
					"Assigned from hostel fee management.");
			HostelFeeAssignmentResponse response = new HostelFeeAssignmentResponse(1, 0, List.of(assignment));
			audit("HostelFeeAssignment", assignment.id(), "HOSTEL_FEE_ASSIGNED_TO_STUDENT", null, response);
			return response;
		}
		if (request.academicYearId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year or hostel fee structure is required.");
		}
		HostelAllocation allocation = allocationRepository
				.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
						request.studentId(),
						request.academicYearId(),
						HostelAllocationStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("Active hostel allocation", request.studentId()));
		List<StudentFeeAssignmentResponse> assignments = assignApplicableHostelFees(
				allocation.getStudent(),
				allocation.getAcademicYear(),
				allocation.getRoom(),
				assignedDate);
		HostelFeeAssignmentResponse response = new HostelFeeAssignmentResponse(assignments.size(), 0, assignments);
		audit("HostelFeeAssignment", allocation.getId(), "HOSTEL_FEE_ASSIGNED_TO_STUDENT", null, response);
		return response;
	}

	private void ensureHostelCodeUnique(String code, UUID existingId) {
		hostelRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.filter(existing -> existingId == null || !existing.getId().equals(existingId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Hostel code already exists.");
				});
	}

	private void ensureRoomNumberUnique(UUID hostelId, String roomNumber, UUID existingId) {
		roomRepository.findByHostelIdAndRoomNumberIgnoreCaseAndDeletedFalse(hostelId, roomNumber)
				.filter(existing -> existingId == null || !existing.getId().equals(existingId))
				.ifPresent(existing -> {
					throw new BusinessException(
							ErrorCode.CONFLICT,
							"Room number already exists for the selected hostel.");
				});
	}

	private void ensureRoomCapacity(HostelRoom room, int capacity) {
		roomRepository.lockDetailedByIdAndDeletedFalse(room.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Hostel room", room.getId()));
		long maxOccupancy = allocationRepository.activeOccupancyCountsByRoom(room.getId(), HostelAllocationStatus.ACTIVE)
				.stream()
				.max(Long::compareTo)
				.orElse(0L);
		if (maxOccupancy > capacity) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Room capacity cannot be lower than active occupancy.");
		}
	}

	private void syncBeds(HostelRoom room, int capacity, boolean usesBeds) {
		List<HostelBed> beds = room.getBeds().stream()
				.filter(bed -> !bed.isDeleted())
				.sorted(Comparator.comparing(HostelBed::getBedNumber))
				.toList();
		if (!usesBeds) {
			beds.forEach(bed -> bed.update(bed.getBedNumber(), false));
			return;
		}
		Set<String> expected = new LinkedHashSet<>();
		for (int bedIndex = 1; bedIndex <= capacity; bedIndex++) {
			String bedNumber = "B" + bedIndex;
			expected.add(bedNumber.toUpperCase());
			Optional<HostelBed> existing = beds.stream()
					.filter(bed -> bedNumber.equalsIgnoreCase(bed.getBedNumber()))
					.findFirst();
			if (existing.isPresent()) {
				existing.get().update(existing.get().getBedNumber(), true);
			}
			else {
				bedRepository.save(room.addBed(bedNumber));
			}
		}
		for (HostelBed bed : beds) {
			if (!expected.contains(firstText(bed.getBedNumber(), "").toUpperCase())) {
				if (allocationRepository.existsByBedIdAndStatusAndDeletedFalse(bed.getId(), HostelAllocationStatus.ACTIVE)) {
					throw new BusinessException(
							ErrorCode.BUSINESS_RULE_VIOLATION,
							"Capacity cannot remove beds that have active allocations.");
				}
				bed.update(bed.getBedNumber(), false);
			}
		}
	}

	private HostelAllocation createActiveAllocation(
			Student student,
			AcademicYear academicYear,
			HostelRoom room,
			HostelBed bed,
			LocalDate allocationDate,
			boolean checkDuplicate) {
		validateRoomAvailable(academicYear, room, bed);
		if (checkDuplicate && allocationRepository.existsByStudentIdAndAcademicYearIdAndStatusAndDeletedFalse(
				student.getId(),
				academicYear.getId(),
				HostelAllocationStatus.ACTIVE)) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Student already has an active hostel allocation for this academic year.");
		}
		return allocationRepository.save(new HostelAllocation(
				student,
				academicYear,
				room.getHostel(),
				room,
				bed,
				allocationDate));
	}

	private void validateRoomAvailable(AcademicYear academicYear, HostelRoom room, HostelBed bed) {
		UUID roomId = room.getId();
		HostelRoom lockedRoom = roomRepository.lockDetailedByIdAndDeletedFalse(roomId)
				.orElseThrow(() -> new ResourceNotFoundException("Hostel room", roomId));
		if (!lockedRoom.getHostel().isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Hostel is inactive.");
		}
		if (!lockedRoom.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Room is inactive.");
		}
		if (lockedRoom.isHasBeds()) {
			if (bed == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Bed is required for this room.");
			}
			if (!bed.isActive()) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Bed is inactive.");
			}
			if (allocationRepository.existsByAcademicYearIdAndBedIdAndStatusAndDeletedFalse(
					academicYear.getId(),
					bed.getId(),
					HostelAllocationStatus.ACTIVE)) {
				throw new BusinessException(ErrorCode.CONFLICT, "Bed is already occupied for this academic year.");
			}
		}
		long occupied = allocationRepository.countByAcademicYearIdAndRoomIdAndStatusAndDeletedFalse(
				academicYear.getId(),
				lockedRoom.getId(),
				HostelAllocationStatus.ACTIVE);
		if (occupied >= lockedRoom.getCapacity()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Room is already full.");
		}
	}

	private List<StudentFeeAssignmentResponse> assignApplicableHostelFees(
			Student student,
			AcademicYear academicYear,
			HostelRoom room,
			LocalDate assignedDate) {
		List<UUID> feeStructureIds = hostelFeeStructureRepository.findApplicable(
				academicYear.getId(),
				room.getHostel().getId(),
				room.getId(),
				room.getRoomType(),
				FeeStructureStatus.ACTIVE).stream()
				.map(structure -> structure.getBackingFeeStructure().getId())
				.toList();
		if (feeStructureIds.isEmpty()) {
			return List.of();
		}
		return feeService.assignActiveHostelFeesToStudent(student.getId(), feeStructureIds, assignedDate);
	}

	private HostelAllocationResponse toAllocationResponse(HostelAllocation allocation) {
		return hostelMapper.toAllocationResponse(allocation, feeAssignedStatus(allocation));
	}

	private String feeAssignedStatus(HostelAllocation allocation) {
		boolean assigned = studentFeeAssignmentRepository.existsActiveHostelAssignmentForAllocation(
				allocation.getStudent().getId(),
				allocation.getAcademicYear().getId(),
				allocation.getHostel().getId(),
				allocation.getRoom().getId(),
				allocation.getRoom().getRoomType(),
				FeeScope.HOSTEL,
				FeeAssignmentStatus.CANCELLED);
		return assigned ? "ASSIGNED" : "NOT_ASSIGNED";
	}

	private ResolvedHostelFeeStructure resolveFeeStructureRequest(HostelFeeStructureRequest request) {
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		Hostel hostel = loadHostel(request.hostelId());
		HostelRoom room = request.roomId() == null ? null : loadRoom(request.roomId());
		if (room != null && !room.getHostel().getId().equals(hostel.getId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Room does not belong to the selected hostel.");
		}
		FeeCategory category = feeCategoryRepository.findByIdAndDeletedFalse(request.feeCategoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Fee category", request.feeCategoryId()));
		if (!category.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Fee category is inactive.");
		}
		return new ResolvedHostelFeeStructure(
				academicYear,
				hostel,
				room,
				room == null ? normalizeRoomType(request.roomType()) : room.getRoomType(),
				category);
	}

	private void validateFeeStructureBusinessRules(
			UUID existingId,
			ResolvedHostelFeeStructure resolved,
			HostelFeeStructureRequest request) {
		if (money(request.amount()).signum() <= 0) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Hostel fee amount must be greater than zero.");
		}
		if (hostelFeeStructureRepository.existsDuplicateScope(
				resolved.academicYear().getId(),
				resolved.hostel().getId(),
				resolved.room() == null ? null : resolved.room().getId(),
				resolved.roomType(),
				resolved.category().getId(),
				existingId)) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"A hostel fee structure already exists for this academic year, hostel, room scope, and fee category.");
		}
	}

	private FeeStructure createOrUpdateBackingFeeStructure(
			FeeStructure existing,
			ResolvedHostelFeeStructure resolved,
			HostelFeeStructureRequest request) {
		FeeStructure structure = existing == null
				? new FeeStructure(
						resolved.academicYear().getName(),
						"HOSTEL-" + resolved.hostel().getCode(),
						scopeLabel(resolved),
						firstText(request.name(), defaultFeeStructureName(resolved)),
						request.description())
				: existing;
		if (existing != null) {
			String actor = currentActor();
			structure.updateDetails(
					resolved.academicYear().getName(),
					"HOSTEL-" + resolved.hostel().getCode(),
					scopeLabel(resolved),
					firstText(request.name(), defaultFeeStructureName(resolved)),
					request.description());
			structure.clearItems(actor);
			structure.clearInstallments(actor);
		}
		structure.updateHostelMapping(
				resolved.academicYear(),
				resolved.hostel(),
				resolved.room(),
				resolved.roomType());
		structure.addItem(resolved.category(), request.amount(), true, 1);
		addInstallments(structure, request);
		applyStatus(structure, statusOrDraft(request.status()));
		return feeStructureRepository.save(structure);
	}

	private void addInstallments(FeeStructure structure, HostelFeeStructureRequest request) {
		int count = installmentCount(request);
		BigDecimal total = money(request.amount());
		BigDecimal base = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
		BigDecimal allocated = BigDecimal.ZERO;
		for (int index = 1; index <= count; index++) {
			BigDecimal amount = index == count ? total.subtract(allocated) : base;
			allocated = allocated.add(amount);
			structure.addInstallment(
					index,
					count == 1 ? "Hostel Fee" : "Hostel Fee Installment " + index,
					request.dueDate().plusMonths(index - 1L),
					amount);
		}
	}

	private void applyStatus(FeeStructure structure, FeeStructureStatus status) {
		if (status == FeeStructureStatus.ACTIVE) {
			structure.activate();
		}
		else if (status == FeeStructureStatus.INACTIVE) {
			structure.deactivate();
		}
		else {
			structure.draft();
		}
	}

	private void ensureFeeStructureNotAssigned(UUID feeStructureId) {
		if (studentFeeAssignmentRepository.existsByFeeStructureIdAndDeletedFalse(feeStructureId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Assigned hostel fee structures cannot be updated or deleted.");
		}
	}

	private HostelRoom resolveRoom(HostelAssignmentRequest request) {
		if (request.roomId() != null) {
			HostelRoom room = loadRoom(request.roomId());
			if (request.hostelId() != null && !room.getHostel().getId().equals(request.hostelId())) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Room does not belong to the selected hostel.");
			}
			return room;
		}
		if (!StringUtils.hasText(request.roomNumber())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Room is required for hostel allocation.");
		}
		Hostel hostel = null;
		if (request.hostelId() != null) {
			hostel = loadHostel(request.hostelId());
		}
		else if (StringUtils.hasText(request.hostelName())) {
			hostel = hostelRepository.findByNameIgnoreCaseAndDeletedFalse(request.hostelName())
					.or(() -> hostelRepository.findByCodeIgnoreCaseAndDeletedFalse(request.hostelName()))
					.orElseThrow(() -> new ResourceNotFoundException("Hostel", request.hostelName()));
		}
		List<HostelRoom> matches = roomRepository.findActiveByRoomNumber(
				hostel == null ? null : hostel.getId(),
				request.roomNumber());
		if (matches.isEmpty()) {
			throw new ResourceNotFoundException("Hostel room", request.roomNumber());
		}
		if (matches.size() > 1) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Hostel is required when room number is not unique.");
		}
		return matches.getFirst();
	}

	private HostelBed resolveBed(HostelRoom room, UUID bedId, String bedNumber) {
		if (!room.isHasBeds()) {
			return null;
		}
		HostelBed bed = bedId == null
				? null
				: bedRepository.findByIdAndDeletedFalse(bedId)
						.orElseThrow(() -> new ResourceNotFoundException("Hostel bed", bedId));
		if (bed == null && StringUtils.hasText(bedNumber)) {
			bed = bedRepository.findByRoomIdAndBedNumberIgnoreCaseAndDeletedFalse(room.getId(), bedNumber)
					.orElseThrow(() -> new ResourceNotFoundException("Hostel bed", bedNumber));
		}
		if (bed == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Bed is required for this room.");
		}
		if (!bed.getRoom().getId().equals(room.getId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Bed does not belong to the selected room.");
		}
		return bed;
	}

	private Hostel loadHostel(UUID hostelId) {
		return hostelRepository.findByIdAndDeletedFalse(hostelId)
				.orElseThrow(() -> new ResourceNotFoundException("Hostel", hostelId));
	}

	private HostelRoom loadRoom(UUID roomId) {
		return roomRepository.findDetailedByIdAndDeletedFalse(roomId)
				.orElseThrow(() -> new ResourceNotFoundException("Hostel room", roomId));
	}

	private HostelAllocation loadAllocation(UUID allocationId) {
		return allocationRepository.findDetailedByIdAndDeletedFalse(allocationId)
				.orElseThrow(() -> new ResourceNotFoundException("Hostel allocation", allocationId));
	}

	private HostelFeeStructure loadHostelFeeStructure(UUID structureId) {
		return hostelFeeStructureRepository.findDetailedByIdAndDeletedFalse(structureId)
				.orElseThrow(() -> new ResourceNotFoundException("Hostel fee structure", structureId));
	}

	private Student loadStudent(UUID studentId) {
		return studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
	}

	private void ensureAllocationBelongsToStudent(HostelAllocation allocation, UUID studentId) {
		if (!allocation.getStudent().getId().equals(studentId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Hostel allocation does not belong to the selected student.");
		}
	}

	private List<HostelAllocation> roomAllocations(UUID roomId, UUID academicYearId) {
		return allocationRepository.findByRoomIdAndAcademicYearIdAndDeletedFalseOrderByAllocationDateAsc(roomId, academicYearId);
	}

	private int occupiedCount(UUID academicYearId, UUID roomId) {
		return (int) allocationRepository.countByAcademicYearIdAndRoomIdAndStatusAndDeletedFalse(
				academicYearId,
				roomId,
				HostelAllocationStatus.ACTIVE);
	}

	private Set<UUID> occupiedBedIds(UUID academicYearId, UUID roomId) {
		Set<UUID> occupied = new LinkedHashSet<>();
		roomAllocations(roomId, academicYearId).stream()
				.filter(allocation -> allocation.getStatus() == HostelAllocationStatus.ACTIVE)
				.map(HostelAllocation::getBed)
				.filter(java.util.Objects::nonNull)
				.map(HostelBed::getId)
				.forEach(occupied::add);
		return occupied;
	}

	private boolean hostelRequested(HostelAssignmentRequest request) {
		return request != null
				&& (request.requiresHostel()
						|| request.hostelId() != null
						|| StringUtils.hasText(request.hostelName())
						|| request.roomId() != null
						|| StringUtils.hasText(request.roomNumber()));
	}

	private int installmentCount(HostelFeeStructureRequest request) {
		return request.installmentAllowed() ? Math.max(request.numberOfInstallments(), 1) : 1;
	}

	private FeeStructureStatus statusOrDraft(FeeStructureStatus status) {
		return status == null ? FeeStructureStatus.DRAFT : status;
	}

	private BigDecimal money(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
	}

	private String normalizeRoomType(String value) {
		return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
	}

	private String scopeLabel(ResolvedHostelFeeStructure resolved) {
		if (resolved.room() != null) {
			return resolved.room().getRoomNumber();
		}
		return firstText(resolved.roomType(), "ALL");
	}

	private String defaultFeeStructureName(ResolvedHostelFeeStructure resolved) {
		String scope = resolved.room() == null ? firstText(resolved.roomType(), "All Rooms") : "Room " + resolved.room().getRoomNumber();
		return resolved.hostel().getName() + " - " + scope + " Hostel Fee";
	}

	private LocalDate defaultDate(LocalDate value, LocalDate fallback) {
		if (value != null) {
			return value;
		}
		return fallback == null ? LocalDate.now() : fallback;
	}

	private String firstText(String primary, String fallback) {
		return StringUtils.hasText(primary) ? primary.trim() : fallback;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue));
	}

	private record ResolvedHostelFeeStructure(
			AcademicYear academicYear,
			Hostel hostel,
			HostelRoom room,
			String roomType,
			FeeCategory category) {
	}
}

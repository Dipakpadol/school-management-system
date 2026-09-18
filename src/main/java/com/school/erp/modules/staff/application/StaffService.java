package com.school.erp.modules.staff.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.staff.api.dto.DepartmentRequest;
import com.school.erp.modules.staff.api.dto.DepartmentResponse;
import com.school.erp.modules.staff.api.dto.DesignationRequest;
import com.school.erp.modules.staff.api.dto.DesignationResponse;
import com.school.erp.modules.staff.api.dto.StaffDocumentRequest;
import com.school.erp.modules.staff.api.dto.StaffDocumentResponse;
import com.school.erp.modules.staff.api.dto.StaffExitRequest;
import com.school.erp.modules.staff.api.dto.StaffRequest;
import com.school.erp.modules.staff.api.dto.StaffResponse;
import com.school.erp.modules.staff.domain.Department;
import com.school.erp.modules.staff.domain.Designation;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffDocument;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.DepartmentRepository;
import com.school.erp.modules.staff.infrastructure.DesignationRepository;
import com.school.erp.modules.staff.infrastructure.StaffDocumentRepository;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffService {

	private static final String MODULE_NAME = "STAFF";

	private final DepartmentRepository departmentRepository;
	private final DesignationRepository designationRepository;
	private final StaffRepository staffRepository;
	private final StaffDocumentRepository documentRepository;
	private final UserAccountRepository userAccountRepository;
	private final TeacherRepository teacherRepository;
	private final StaffMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<DepartmentResponse> departments() {
		return departmentRepository.findAllByDeletedFalseOrderByNameAsc().stream()
				.map(mapper::toDepartmentResponse)
				.toList();
	}

	@Transactional
	public DepartmentResponse createDepartment(DepartmentRequest request) {
		validateDepartmentName(request.name(), null, active(request.active()));
		Department department = departmentRepository.save(new Department(
				request.name(),
				request.description(),
				active(request.active())));
		DepartmentResponse response = mapper.toDepartmentResponse(department);
		audit("Department", department.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest request) {
		Department department = loadDepartment(departmentId);
		DepartmentResponse oldValue = mapper.toDepartmentResponse(department);
		boolean active = active(request.active());
		validateDepartmentName(request.name(), departmentId, active);
		department.update(request.name(), request.description(), active);
		DepartmentResponse response = mapper.toDepartmentResponse(department);
		audit("Department", departmentId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public List<DesignationResponse> designations() {
		return designationRepository.findAllByDeletedFalseOrderByNameAsc().stream()
				.map(mapper::toDesignationResponse)
				.toList();
	}

	@Transactional
	public DesignationResponse createDesignation(DesignationRequest request) {
		validateDesignationName(request.name(), null, active(request.active()));
		Department department = request.departmentId() == null ? null : loadActiveDepartment(request.departmentId());
		Designation designation = designationRepository.save(new Designation(
				request.name(),
				department,
				request.description(),
				active(request.active())));
		DesignationResponse response = mapper.toDesignationResponse(designation);
		audit("Designation", designation.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public DesignationResponse updateDesignation(UUID designationId, DesignationRequest request) {
		Designation designation = loadDesignation(designationId);
		DesignationResponse oldValue = mapper.toDesignationResponse(designation);
		boolean active = active(request.active());
		validateDesignationName(request.name(), designationId, active);
		Department department = request.departmentId() == null ? null : loadDepartmentForAssignment(request.departmentId(), designation.getDepartment());
		designation.update(request.name(), department, request.description(), active);
		DesignationResponse response = mapper.toDesignationResponse(designation);
		audit("Designation", designationId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<StaffResponse> staff(
			EmploymentStatus status,
			StaffType staffType,
			UUID departmentId,
			UUID designationId,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				staffRepository.search(
						status,
						staffType,
						departmentId,
						designationId,
						pageRequest.toPageable("firstName")),
				mapper::toStaffResponse);
	}

	@Transactional(readOnly = true)
	public StaffResponse getStaff(UUID staffId) {
		return mapper.toStaffResponse(loadStaff(staffId));
	}

	@Transactional
	public StaffResponse createStaff(StaffRequest request) {
		validateEmployeeCode(request.employeeCode(), null);
		Department department = loadActiveDepartment(request.departmentId());
		Designation designation = loadActiveDesignation(request.designationId());
		validateUserAccount(request.userAccountId());
		Teacher teacher = resolveTeacher(request.teacherId(), null);
		Staff staff = staffRepository.save(new Staff(
				request.employeeCode(),
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.gender(),
				request.dateOfBirth(),
				request.email(),
				request.phoneNumber(),
				request.userAccountId(),
				teacher == null ? null : teacher.getId(),
				department,
				designation,
				request.joiningDate(),
				staffType(request.staffType(), teacher),
				status(request.status())));
		linkTeacher(staff, teacher, null);
		StaffResponse response = mapper.toStaffResponse(staff);
		audit("Staff", staff.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public StaffResponse updateStaff(UUID staffId, StaffRequest request) {
		Staff staff = loadStaff(staffId);
		StaffResponse oldValue = mapper.toStaffResponse(staff);
		UUID oldTeacherId = staff.getTeacherId();
		validateEmployeeCode(request.employeeCode(), staffId);
		Department department = loadDepartmentForAssignment(request.departmentId(), staff.getDepartment());
		Designation designation = loadDesignationForAssignment(request.designationId(), staff.getDesignation());
		validateUserAccount(request.userAccountId());
		Teacher teacher = resolveTeacher(request.teacherId(), staffId);
		staff.updateProfile(
				request.employeeCode(),
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.gender(),
				request.dateOfBirth(),
				request.email(),
				request.phoneNumber(),
				request.userAccountId(),
				teacher == null ? null : teacher.getId(),
				department,
				designation,
				request.joiningDate(),
				staffType(request.staffType(), teacher),
				status(request.status()));
		linkTeacher(staff, teacher, oldTeacherId);
		StaffResponse response = mapper.toStaffResponse(staff);
		audit("Staff", staffId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public StaffResponse deactivateStaff(UUID staffId) {
		Staff staff = loadStaff(staffId);
		StaffResponse oldValue = mapper.toStaffResponse(staff);
		staff.deactivate();
		StaffResponse response = mapper.toStaffResponse(staff);
		audit("Staff", staffId, "DEACTIVATE", oldValue, response);
		return response;
	}

	@Transactional
	public StaffResponse exitStaff(UUID staffId, StaffExitRequest request) {
		Staff staff = loadStaff(staffId);
		StaffResponse oldValue = mapper.toStaffResponse(staff);
		if (request.relievingDate().isBefore(staff.getJoiningDate())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Relieving date cannot be before joining date.");
		}
		staff.exit(request.relievingDate(), request.reason());
		StaffResponse response = mapper.toStaffResponse(staff);
		audit("Staff", staffId, "EXIT", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public List<StaffDocumentResponse> documents(UUID staffId) {
		loadStaff(staffId);
		return documentRepository.findByStaffIdAndDeletedFalseOrderByUploadedAtDesc(staffId).stream()
				.map(mapper::toDocumentResponse)
				.toList();
	}

	@Transactional
	public StaffDocumentResponse createDocument(UUID staffId, StaffDocumentRequest request) {
		Staff staff = loadStaff(staffId);
		validateDocumentLocation(request);
		StaffDocument document = documentRepository.save(new StaffDocument(
				staff,
				request.documentType(),
				request.fileName(),
				request.fileUrl(),
				request.filePath(),
				currentActor(),
				request.status()));
		StaffDocumentResponse response = mapper.toDocumentResponse(document);
		audit("StaffDocument", document.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public StaffDocumentResponse updateDocument(UUID staffId, UUID documentId, StaffDocumentRequest request) {
		StaffDocument document = loadDocument(documentId);
		ensureDocumentBelongsToStaff(document, staffId);
		validateDocumentLocation(request);
		StaffDocumentResponse oldValue = mapper.toDocumentResponse(document);
		document.update(
				request.documentType(),
				request.fileName(),
				request.fileUrl(),
				request.filePath(),
				request.status());
		StaffDocumentResponse response = mapper.toDocumentResponse(document);
		audit("StaffDocument", documentId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public void deleteDocument(UUID staffId, UUID documentId) {
		StaffDocument document = loadDocument(documentId);
		ensureDocumentBelongsToStaff(document, staffId);
		StaffDocumentResponse oldValue = mapper.toDocumentResponse(document);
		document.softDelete(currentActor());
		audit("StaffDocument", documentId, "DELETE", oldValue, Map.of("deleted", true, "documentId", documentId));
	}

	public Staff loadStaff(UUID staffId) {
		return staffRepository.findByIdAndDeletedFalse(staffId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
	}

	public Staff loadActiveStaff(UUID staffId) {
		Staff staff = loadStaff(staffId);
		if (!staff.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Staff is inactive.");
		}
		return staff;
	}

	private Department loadDepartment(UUID departmentId) {
		return departmentRepository.findByIdAndDeletedFalse(departmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
	}

	private Department loadActiveDepartment(UUID departmentId) {
		Department department = loadDepartment(departmentId);
		if (!department.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Department is inactive.");
		}
		return department;
	}

	private Department loadDepartmentForAssignment(UUID departmentId, Department currentDepartment) {
		Department department = loadDepartment(departmentId);
		if (!department.isActive() && (currentDepartment == null || !currentDepartment.getId().equals(departmentId))) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Department is inactive.");
		}
		return department;
	}

	private Designation loadDesignation(UUID designationId) {
		return designationRepository.findByIdAndDeletedFalse(designationId)
				.orElseThrow(() -> new ResourceNotFoundException("Designation", designationId));
	}

	private Designation loadActiveDesignation(UUID designationId) {
		Designation designation = loadDesignation(designationId);
		if (!designation.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Designation is inactive.");
		}
		return designation;
	}

	private Designation loadDesignationForAssignment(UUID designationId, Designation currentDesignation) {
		Designation designation = loadDesignation(designationId);
		if (!designation.isActive() && (currentDesignation == null || !currentDesignation.getId().equals(designationId))) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Designation is inactive.");
		}
		return designation;
	}

	private void validateDepartmentName(String name, UUID excludedId, boolean active) {
		if (active) {
			departmentRepository.findByNameIgnoreCaseAndDeletedFalse(name)
					.filter(existing -> existing.isActive())
					.filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
					.ifPresent(existing -> {
						throw new BusinessException(ErrorCode.CONFLICT, "Active department name already exists.");
					});
		}
	}

	private void validateDesignationName(String name, UUID excludedId, boolean active) {
		if (active) {
			designationRepository.findByNameIgnoreCaseAndDeletedFalse(name)
					.filter(existing -> existing.isActive())
					.filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
					.ifPresent(existing -> {
						throw new BusinessException(ErrorCode.CONFLICT, "Active designation name already exists.");
					});
		}
	}

	private void validateEmployeeCode(String employeeCode, UUID excludedStaffId) {
		staffRepository.findByEmployeeCodeIgnoreCaseAndDeletedFalse(employeeCode)
				.filter(existing -> excludedStaffId == null || !existing.getId().equals(excludedStaffId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Staff employee code already exists.");
				});
	}

	private void validateUserAccount(UUID userAccountId) {
		if (userAccountId != null && userAccountRepository.findByIdAndDeletedFalse(userAccountId).isEmpty()) {
			throw new ResourceNotFoundException("User account", userAccountId);
		}
	}

	private Teacher resolveTeacher(UUID teacherId, UUID currentStaffId) {
		if (teacherId == null) {
			return null;
		}
		Teacher teacher = teacherRepository.findByIdAndDeletedFalse(teacherId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
		staffRepository.findByTeacherIdAndDeletedFalse(teacherId)
				.filter(existing -> currentStaffId == null || !existing.getId().equals(currentStaffId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Teacher is already linked to a staff profile.");
				});
		return teacher;
	}

	private void linkTeacher(Staff staff, Teacher teacher, UUID oldTeacherId) {
		UUID newTeacherId = teacher == null ? null : teacher.getId();
		if (oldTeacherId != null && !oldTeacherId.equals(newTeacherId)) {
			teacherRepository.findByIdAndDeletedFalse(oldTeacherId).ifPresent(previousTeacher -> {
				previousTeacher.linkStaff(null);
				teacherRepository.save(previousTeacher);
			});
		}
		if (teacher == null) {
			staff.linkTeacher(null);
			return;
		}
		staff.linkTeacher(teacher.getId());
		teacher.linkStaff(staff.getId());
		teacherRepository.save(teacher);
	}

	private StaffType staffType(StaffType requested, Teacher teacher) {
		if (teacher != null) {
			return StaffType.TEACHING;
		}
		return requested == null ? StaffType.NON_TEACHING : requested;
	}

	private EmploymentStatus status(EmploymentStatus requested) {
		return requested == null ? EmploymentStatus.ACTIVE : requested;
	}

	private boolean active(Boolean active) {
		return active == null || active;
	}

	private void validateDocumentLocation(StaffDocumentRequest request) {
		if (!StringUtils.hasText(request.fileUrl()) && !StringUtils.hasText(request.filePath())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Document file URL or file path is required.");
		}
	}

	private StaffDocument loadDocument(UUID documentId) {
		return documentRepository.findByIdAndDeletedFalse(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Staff document", documentId));
	}

	private void ensureDocumentBelongsToStaff(StaffDocument document, UUID staffId) {
		if (!document.getStaff().getId().equals(staffId)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Document does not belong to the selected staff profile.");
		}
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
				newValue == null ? Map.of() : newValue));
	}
}

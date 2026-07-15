package com.school.erp.modules.documents.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.documents.api.dto.CertificateGenerationRequest;
import com.school.erp.modules.documents.api.dto.GeneratedDocumentResponse;
import com.school.erp.modules.documents.domain.GeneratedDocument;
import com.school.erp.modules.documents.domain.GeneratedDocumentStatus;
import com.school.erp.modules.documents.domain.GeneratedDocumentType;
import com.school.erp.modules.documents.infrastructure.GeneratedDocumentRepository;
import com.school.erp.modules.students.application.StudentGeneratedDocumentService;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneratedDocumentService {

	private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final StudentRepository studentRepository;
	private final AcademicHierarchyService academicHierarchyService;
	private final GeneratedDocumentRepository generatedDocumentRepository;
	private final StudentGeneratedDocumentService studentGeneratedDocumentService;
	private final AuditLogService auditLogService;

	@Transactional
	public StudentGeneratedDocumentService.GeneratedStudentDocument preview(CertificateGenerationRequest request) {
		Student student = loadStudent(request.studentId());
		academicHierarchyService.loadAcademicYear(request.academicYearId());
		var document = studentGeneratedDocumentService.generate(student.getId(), request.documentType().name());
		audit("GeneratedDocument", student.getId(), "CERTIFICATE_PREVIEWED", null, Map.of(
				"studentId", student.getId(),
				"documentType", request.documentType()));
		return document;
	}

	@Transactional
	public GeneratedDocumentResponse generate(CertificateGenerationRequest request) {
		Student student = loadStudent(request.studentId());
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		if (request.documentType() == GeneratedDocumentType.LEAVING_CERTIFICATE
				&& generatedDocumentRepository.existsByStudentIdAndDocumentTypeAndStatusAndDeletedFalse(
						student.getId(),
						GeneratedDocumentType.LEAVING_CERTIFICATE,
						GeneratedDocumentStatus.GENERATED)) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Leaving certificate is already generated for this student. Use reprint instead.");
		}
		var generated = studentGeneratedDocumentService.generate(student.getId(), request.documentType().name());
		GeneratedDocument document = new GeneratedDocument(
				student,
				academicYear,
				request.documentType(),
				documentNumber(request.documentType()),
				request.issueDate() == null ? LocalDate.now() : request.issueDate(),
				request.purpose(),
				request.remarks(),
				generated.filename(),
				generated.contentType(),
				generated.content());
		GeneratedDocument saved = generatedDocumentRepository.save(document);
		GeneratedDocumentResponse response = toResponse(saved);
		audit("GeneratedDocument", saved.getId(), "CERTIFICATE_GENERATED", null, response);
		return response;
	}

	@Transactional(readOnly = true)
	public List<GeneratedDocumentResponse> history(UUID studentId) {
		loadStudent(studentId);
		return generatedDocumentRepository.findByStudentIdOrderByIssueDateDesc(studentId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public GeneratedDocumentResponse get(UUID documentId) {
		return toResponse(loadDocument(documentId));
	}

	@Transactional
	public GeneratedDocumentResponse reprint(UUID documentId) {
		GeneratedDocument original = loadDocument(documentId);
		GeneratedDocument reprint = generatedDocumentRepository.save(original.reprint(documentNumber(original.getDocumentType())));
		GeneratedDocumentResponse response = toResponse(reprint);
		audit("GeneratedDocument", reprint.getId(), "CERTIFICATE_REPRINTED", toResponse(original), response);
		return response;
	}

	@Transactional
	public GeneratedDocumentContent download(UUID documentId) {
		GeneratedDocument document = loadDocument(documentId);
		audit("GeneratedDocument", document.getId(), "CERTIFICATE_DOWNLOADED", null, Map.of(
				"documentId", document.getId(),
				"documentType", document.getDocumentType(),
				"downloadedBy", currentActor()));
		return new GeneratedDocumentContent(document.getContent(), document.getFileName(), document.getContentType());
	}

	private Student loadStudent(UUID studentId) {
		return studentRepository.findProfileByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
	}

	private GeneratedDocument loadDocument(UUID documentId) {
		return generatedDocumentRepository.findDetailedByIdAndDeletedFalse(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Generated document", documentId));
	}

	private GeneratedDocumentResponse toResponse(GeneratedDocument document) {
		return new GeneratedDocumentResponse(
				document.getId(),
				document.getStudent().getId(),
				document.getStudent().getAdmissionNumber(),
				document.getStudent().getDisplayName(),
				document.getAcademicYear().getId(),
				document.getAcademicYear().getName(),
				document.getDocumentType(),
				document.getDocumentNumber(),
				document.getIssueDate(),
				document.getPurpose(),
				document.getRemarks(),
				document.getFileName(),
				document.getContentType(),
				document.getFileSize(),
				document.getStatus(),
				document.getReprintOf() == null ? null : document.getReprintOf().getId(),
				document.getCreatedAt());
	}

	private String documentNumber(GeneratedDocumentType documentType) {
		String prefix = switch (documentType) {
			case BONAFIDE_CERTIFICATE -> "BON";
			case LEAVING_CERTIFICATE -> "LC";
			case STUDENT_ID_CARD -> "SID";
		};
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
		return prefix + "-" + LocalDate.now().format(NUMBER_DATE) + "-" + suffix;
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
				"DOCUMENTS",
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue));
	}

	public record GeneratedDocumentContent(byte[] content, String filename, String contentType) {
	}
}

package com.school.erp.modules.students.application;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.domain.StudentParent;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentGeneratedDocumentService {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
	private static final String CONTENT_TYPE = "application/pdf";

	private final StudentRepository studentRepository;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public GeneratedStudentDocument generate(UUID studentId, String type) {
		GeneratedDocumentType documentType = parseType(type);
		Student student = studentRepository.findProfileByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		byte[] content = switch (documentType) {
			case LEAVING_CERTIFICATE -> leavingCertificate(student);
			case BONAFIDE_CERTIFICATE -> bonafideCertificate(student);
			case STUDENT_ID_CARD -> studentIdCard(student);
		};
		auditLogService.recordStandalone(new AuditLogEvent(
				"STUDENTS",
				"Student",
				studentId.toString(),
				AuditAction.EXPORT,
				null,
				documentType.name()));
		return new GeneratedStudentDocument(content, filename(student, documentType), CONTENT_TYPE);
	}

	private byte[] leavingCertificate(Student student) {
		return certificate("Leaving Certificate", student, """
				This is to certify that %s, admission number %s, was admitted on %s and has been enrolled in this institution.
				As per the school records, the student's current/last class is %s. This certificate is generated for official school records.
				""".formatted(
				student.getDisplayName(),
				student.getAdmissionNumber(),
				date(student.getAdmissionDate()),
				classLabel(student)));
	}

	private byte[] bonafideCertificate(Student student) {
		return certificate("Bonafide Certificate", student, """
				This is to certify that %s, admission number %s, is a bonafide student of this institution.
				The student is currently recorded in %s for the academic year %s.
				""".formatted(
				student.getDisplayName(),
				student.getAdmissionNumber(),
				classLabel(student),
				academicYear(student)));
	}

	private byte[] certificate(String title, Student student, String body) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A4, 48, 48, 48, 48);
			PdfWriter.getInstance(document, outputStream);
			document.open();
			addSchoolHeader(document, title);
			Paragraph content = new Paragraph(body.trim(), normalFont(12));
			content.setAlignment(Element.ALIGN_JUSTIFIED);
			content.setSpacingBefore(24);
			content.setLeading(20);
			document.add(content);
			document.add(profileTable(student));
			Paragraph footer = new Paragraph("Generated on " + date(LocalDate.now()), normalFont(10));
			footer.setSpacingBefore(36);
			document.add(footer);
			Paragraph signature = new Paragraph("Principal / Authorized Signatory", boldFont(11));
			signature.setAlignment(Element.ALIGN_RIGHT);
			signature.setSpacingBefore(48);
			document.add(signature);
			document.close();
			return outputStream.toByteArray();
		}
		catch (java.io.IOException | RuntimeException ex) {
			throw new IllegalStateException("Unable to generate student document.", ex);
		}
	}

	private byte[] studentIdCard(Student student) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A5.rotate(), 36, 36, 36, 36);
			PdfWriter.getInstance(document, outputStream);
			document.open();
			addSchoolHeader(document, "Student ID Card");
			PdfPTable table = new PdfPTable(new float[] { 1.1f, 2.2f });
			table.setWidthPercentage(100);
			PdfPCell photo = new PdfPCell(new Phrase("PHOTO", boldFont(13)));
			photo.setFixedHeight(132);
			photo.setHorizontalAlignment(Element.ALIGN_CENTER);
			photo.setVerticalAlignment(Element.ALIGN_MIDDLE);
			photo.setBorderColor(new Color(120, 130, 145));
			table.addCell(photo);
			PdfPTable details = new PdfPTable(2);
			details.setWidthPercentage(100);
			addRow(details, "Name", student.getDisplayName());
			addRow(details, "Admission No.", student.getAdmissionNumber());
			addRow(details, "Class", classLabel(student));
			addRow(details, "Roll No.", rollNumber(student));
			addRow(details, "DOB", date(student.getDateOfBirth()));
			addRow(details, "Phone", value(student.getPhoneNumber()));
			addRow(details, "Guardian", primaryGuardian(student));
			PdfPCell detailsCell = new PdfPCell(details);
			detailsCell.setPadding(0);
			table.addCell(detailsCell);
			document.add(table);
			Paragraph footer = new Paragraph("Valid as per active school records. Generated on " + date(LocalDate.now()), normalFont(9));
			footer.setSpacingBefore(18);
			document.add(footer);
			document.close();
			return outputStream.toByteArray();
		}
		catch (java.io.IOException | RuntimeException ex) {
			throw new IllegalStateException("Unable to generate student ID card.", ex);
		}
	}

	private void addSchoolHeader(Document document, String title) {
		Paragraph school = new Paragraph("School Management System", boldFont(18));
		school.setAlignment(Element.ALIGN_CENTER);
		document.add(school);
		Paragraph heading = new Paragraph(title, boldFont(15));
		heading.setAlignment(Element.ALIGN_CENTER);
		heading.setSpacingBefore(8);
		document.add(heading);
	}

	private PdfPTable profileTable(Student student) {
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(100);
		table.setSpacingBefore(20);
		addRow(table, "Admission Number", student.getAdmissionNumber());
		addRow(table, "Student Name", student.getDisplayName());
		addRow(table, "Class / Section", classLabel(student));
		addRow(table, "Date of Birth", date(student.getDateOfBirth()));
		addRow(table, "Admission Date", date(student.getAdmissionDate()));
		addRow(table, "Status", student.getStatus().name());
		return table;
	}

	private void addRow(PdfPTable table, String label, String value) {
		PdfPCell left = new PdfPCell(new Phrase(label, boldFont(9)));
		left.setBackgroundColor(new Color(238, 242, 247));
		left.setPadding(6);
		left.setBorder(Rectangle.BOX);
		table.addCell(left);
		PdfPCell right = new PdfPCell(new Phrase(value(value), normalFont(9)));
		right.setPadding(6);
		right.setBorder(Rectangle.BOX);
		table.addCell(right);
	}

	private GeneratedDocumentType parseType(String type) {
		String normalized = StringUtils.hasText(type)
				? type.trim().replace('-', '_').replace(' ', '_').toUpperCase()
				: "";
		return switch (normalized) {
			case "LC", "LEAVING", "LEAVING_CERTIFICATE" -> GeneratedDocumentType.LEAVING_CERTIFICATE;
			case "BONAFIDE", "BONAFIDE_CERTIFICATE" -> GeneratedDocumentType.BONAFIDE_CERTIFICATE;
			case "ID", "ID_CARD", "STUDENT_ID", "STUDENT_ID_CARD" -> GeneratedDocumentType.STUDENT_ID_CARD;
			default -> throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Supported generated document types are LEAVING_CERTIFICATE, BONAFIDE_CERTIFICATE, and STUDENT_ID_CARD.");
		};
	}

	private String filename(Student student, GeneratedDocumentType type) {
		String suffix = switch (type) {
			case LEAVING_CERTIFICATE -> "leaving-certificate";
			case BONAFIDE_CERTIFICATE -> "bonafide-certificate";
			case STUDENT_ID_CARD -> "student-id-card";
		};
		return student.getAdmissionNumber().toLowerCase() + "-" + suffix + ".pdf";
	}

	private String classLabel(Student student) {
		return student.getCurrentAssignment()
				.map(assignment -> String.join(" ",
						value(assignment.getClassName()),
						value(assignment.getSectionName())).trim())
				.filter(StringUtils::hasText)
				.orElse("Not assigned");
	}

	private String academicYear(Student student) {
		return student.getCurrentAssignment()
				.map(StudentClassAssignment::getAcademicYear)
				.filter(StringUtils::hasText)
				.orElse("Not assigned");
	}

	private String rollNumber(Student student) {
		return student.getCurrentAssignment()
				.map(StudentClassAssignment::getRollNumber)
				.filter(StringUtils::hasText)
				.orElse("-");
	}

	private String primaryGuardian(Student student) {
		return student.getParents().stream()
				.filter(parent -> !parent.isDeleted())
				.filter(StudentParent::isPrimaryContact)
				.findFirst()
				.map(parent -> parent.getParent().getDisplayName())
				.orElse("-");
	}

	private String date(LocalDate value) {
		return value == null ? "-" : DATE_FORMAT.format(value);
	}

	private String value(String value) {
		return StringUtils.hasText(value) ? value.trim() : "-";
	}

	private Font boldFont(float size) {
		return FontFactory.getFont(FontFactory.HELVETICA_BOLD, size);
	}

	private Font normalFont(float size) {
		return FontFactory.getFont(FontFactory.HELVETICA, size);
	}

	public record GeneratedStudentDocument(byte[] content, String filename, String contentType) {
	}

	private enum GeneratedDocumentType {
		LEAVING_CERTIFICATE,
		BONAFIDE_CERTIFICATE,
		STUDENT_ID_CARD
	}
}

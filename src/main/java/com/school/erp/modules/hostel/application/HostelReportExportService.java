package com.school.erp.modules.hostel.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.hostel.api.dto.HostelAllocationResponse;
import com.school.erp.modules.hostel.api.dto.HostelRoomSummaryResponse;
import com.school.erp.modules.hostel.api.dto.HostelSummaryResponse;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostelReportExportService {

	public static final List<String> HOSTEL_HEADERS = List.of(
			"id",
			"code",
			"name",
			"address",
			"active");
	public static final List<String> OCCUPANCY_HEADERS = List.of(
			"academicYearId",
			"hostelId",
			"hostelName",
			"roomId",
			"roomNumber",
			"roomType",
			"capacity",
			"occupiedCount",
			"availableBeds",
			"bedConceptEnabled",
			"active");
	public static final List<String> ALLOCATION_HEADERS = List.of(
			"allocationId",
			"studentId",
			"admissionNumber",
			"studentName",
			"academicYearId",
			"academicYear",
			"hostelId",
			"hostelName",
			"roomId",
			"roomNumber",
			"roomType",
			"bedNumber",
			"allocationDate",
			"vacateDate",
			"status",
			"feeAssignedStatus");

	private final HostelService hostelService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;

	@Transactional(readOnly = true)
	public byte[] hostelListReport(String format) {
		return export(
				format,
				"Hostel List Report",
				"hostel-list-report",
				HOSTEL_HEADERS,
				hostelListRows(),
				filters(null, null, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> hostelListRows() {
		return hostelService.hostels(false).stream()
				.map(this::hostelRow)
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] occupancyReport(String format, UUID academicYearId, UUID hostelId, UUID roomId) {
		return export(
				format,
				"Hostel Occupancy Report",
				"hostel-occupancy-report",
				OCCUPANCY_HEADERS,
				occupancyRows(academicYearId, hostelId, roomId),
				filters(academicYearId, hostelId, roomId, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> occupancyRows(UUID academicYearId, UUID hostelId, UUID roomId) {
		if (academicYearId == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year is required.");
		}
		return hostelService.roomsForAcademicYear(academicYearId).stream()
				.filter(room -> hostelId == null || room.hostelId().equals(hostelId))
				.filter(room -> roomId == null || room.id().equals(roomId))
				.map(room -> occupancyRow(academicYearId, room))
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] allocationReport(
			String format,
			UUID academicYearId,
			UUID hostelId,
			UUID roomId,
			UUID studentId,
			HostelAllocationStatus status) {
		return export(
				format,
				"Hostel Allocation Report",
				"hostel-allocation-report",
				ALLOCATION_HEADERS,
				allocationRows(academicYearId, hostelId, roomId, studentId, status),
				filters(academicYearId, hostelId, roomId, studentId, status));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> allocationRows(
			UUID academicYearId,
			UUID hostelId,
			UUID roomId,
			UUID studentId,
			HostelAllocationStatus status) {
		return hostelService.allocationReport(academicYearId, hostelId, roomId, studentId, status).stream()
				.map(this::allocationRow)
				.toList();
	}

	private Map<String, Object> hostelRow(HostelSummaryResponse hostel) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", hostel.id());
		row.put("code", hostel.code());
		row.put("name", hostel.name());
		row.put("address", hostel.address());
		row.put("active", hostel.active());
		return row;
	}

	private Map<String, Object> occupancyRow(UUID academicYearId, HostelRoomSummaryResponse room) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("academicYearId", academicYearId);
		row.put("hostelId", room.hostelId());
		row.put("hostelName", room.hostelName());
		row.put("roomId", room.id());
		row.put("roomNumber", room.roomNumber());
		row.put("roomType", room.roomType());
		row.put("capacity", room.capacity());
		row.put("occupiedCount", room.occupiedCount());
		row.put("availableBeds", room.availableBeds());
		row.put("bedConceptEnabled", room.bedConceptEnabled());
		row.put("active", room.active());
		return row;
	}

	private Map<String, Object> allocationRow(HostelAllocationResponse allocation) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("allocationId", allocation.allocationId());
		row.put("studentId", allocation.studentId());
		row.put("admissionNumber", allocation.admissionNumber());
		row.put("studentName", allocation.studentName());
		row.put("academicYearId", allocation.academicYearId());
		row.put("academicYear", allocation.academicYearName());
		row.put("hostelId", allocation.hostelId());
		row.put("hostelName", allocation.hostelName());
		row.put("roomId", allocation.roomId());
		row.put("roomNumber", allocation.roomNumber());
		row.put("roomType", allocation.roomType());
		row.put("bedNumber", allocation.bedNumber());
		row.put("allocationDate", allocation.allocationDate());
		row.put("vacateDate", allocation.vacateDate());
		row.put("status", allocation.status());
		row.put("feeAssignedStatus", allocation.feeAssignedStatus());
		return row;
	}

	private byte[] export(
			String format,
			String title,
			String sheetName,
			List<String> headers,
			List<Map<String, Object>> rows,
			Map<String, Object> filters) {
		return switch (normalizeFormat(format)) {
			case "CSV" -> csvExportService.export(headers, rows);
			case "PDF" -> pdfExportService.exportTable(title, filters, headers, rows);
			case "EXCEL" -> excelExportService.export(sheetName, headers, rows);
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, title + " does not support " + format + " export.");
		};
	}

	private Map<String, Object> filters(
			UUID academicYearId,
			UUID hostelId,
			UUID roomId,
			UUID studentId,
			HostelAllocationStatus status) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("Academic Year ID", academicYearId);
		filters.put("Hostel ID", hostelId);
		filters.put("Room ID", roomId);
		filters.put("Student ID", studentId);
		filters.put("Status", status);
		return filters;
	}

	private String normalizeFormat(String format) {
		return format == null ? "EXCEL" : format.trim().toUpperCase(Locale.ROOT);
	}
}

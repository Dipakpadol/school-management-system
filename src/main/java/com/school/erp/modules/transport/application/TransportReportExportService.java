package com.school.erp.modules.transport.application;

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
import com.school.erp.modules.transport.api.dto.TransportDriverResponse;
import com.school.erp.modules.transport.api.dto.TransportRouteResponse;
import com.school.erp.modules.transport.api.dto.TransportStudentAssignmentResponse;
import com.school.erp.modules.transport.api.dto.TransportVehicleResponse;
import com.school.erp.modules.transport.domain.TransportStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportReportExportService {

	public static final List<String> ROUTE_HEADERS = List.of(
			"academicYearId",
			"academicYear",
			"routeId",
			"routeName",
			"routeCode",
			"startLocation",
			"endLocation",
			"vehicleId",
			"vehicleNumber",
			"vehicleName",
			"status");
	public static final List<String> VEHICLE_HEADERS = List.of(
			"academicYearId",
			"academicYear",
			"vehicleId",
			"vehicleNumber",
			"vehicleName",
			"vehicleType",
			"capacity",
			"occupiedCount",
			"availableSeats",
			"driverName",
			"driverMobile",
			"status");
	public static final List<String> DRIVER_HEADERS = List.of(
			"driverId",
			"displayName",
			"mobileNumber",
			"licenseNumber",
			"licenseExpiryDate",
			"address",
			"status");
	public static final List<String> ASSIGNMENT_HEADERS = List.of(
			"assignmentId",
			"studentId",
			"admissionNumber",
			"studentName",
			"academicYearId",
			"academicYear",
			"classId",
			"className",
			"sectionId",
			"sectionName",
			"vehicleId",
			"vehicleNumber",
			"vehicleName",
			"routeId",
			"routeName",
			"pickupPointId",
			"pickupPointName",
			"assignmentDate",
			"endDate",
			"status",
			"feeAssignedStatus");

	private final TransportService transportService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;

	@Transactional(readOnly = true)
	public byte[] routesReport(String format, UUID academicYearId, UUID vehicleId) {
		return export(
				format,
				"Transport Routes Report",
				"transport-routes-report",
				ROUTE_HEADERS,
				routeRows(academicYearId, vehicleId),
				filters(academicYearId, vehicleId, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> routeRows(UUID academicYearId, UUID vehicleId) {
		if (academicYearId == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year is required.");
		}
		return transportService.routes(academicYearId).stream()
				.filter(route -> vehicleId == null || vehicleId.equals(route.vehicleId()))
				.map(this::routeRow)
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] vehiclesReport(String format, UUID academicYearId, UUID vehicleId) {
		return export(
				format,
				"Transport Vehicles Report",
				"transport-vehicles-report",
				VEHICLE_HEADERS,
				vehicleRows(academicYearId, vehicleId),
				filters(academicYearId, vehicleId, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> vehicleRows(UUID academicYearId, UUID vehicleId) {
		if (academicYearId == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year is required.");
		}
		if (vehicleId != null) {
			return List.of(vehicleRow(transportService.getVehicle(vehicleId)));
		}
		return transportService.vehicles(academicYearId).stream()
				.map(this::vehicleRow)
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] driversReport(String format) {
		return export(
				format,
				"Transport Drivers Report",
				"transport-drivers-report",
				DRIVER_HEADERS,
				driverRows(),
				filters(null, null, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> driverRows() {
		return transportService.drivers().stream()
				.map(this::driverRow)
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] assignmentsReport(
			String format,
			UUID academicYearId,
			UUID vehicleId,
			UUID routeId,
			UUID studentId,
			TransportStatus status) {
		return export(
				format,
				"Transport Assignment Report",
				"transport-assignment-report",
				ASSIGNMENT_HEADERS,
				assignmentRows(academicYearId, vehicleId, routeId, studentId, status),
				filters(academicYearId, vehicleId, routeId, studentId, status));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> assignmentRows(
			UUID academicYearId,
			UUID vehicleId,
			UUID routeId,
			UUID studentId,
			TransportStatus status) {
		return transportService.assignmentReport(academicYearId, vehicleId, routeId, studentId, status).stream()
				.map(this::assignmentRow)
				.toList();
	}

	private Map<String, Object> routeRow(TransportRouteResponse route) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("academicYearId", route.academicYearId());
		row.put("academicYear", route.academicYear());
		row.put("routeId", route.id());
		row.put("routeName", route.routeName());
		row.put("routeCode", route.routeCode());
		row.put("startLocation", route.startLocation());
		row.put("endLocation", route.endLocation());
		row.put("vehicleId", route.vehicleId());
		row.put("vehicleNumber", route.vehicleNumber());
		row.put("vehicleName", route.vehicleName());
		row.put("status", route.status());
		return row;
	}

	private Map<String, Object> vehicleRow(TransportVehicleResponse vehicle) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("academicYearId", vehicle.academicYearId());
		row.put("academicYear", vehicle.academicYear());
		row.put("vehicleId", vehicle.id());
		row.put("vehicleNumber", vehicle.vehicleNumber());
		row.put("vehicleName", vehicle.vehicleName());
		row.put("vehicleType", vehicle.vehicleType());
		row.put("capacity", vehicle.capacity());
		row.put("occupiedCount", vehicle.occupiedCount());
		row.put("availableSeats", vehicle.availableSeats());
		row.put("driverName", vehicle.driverName());
		row.put("driverMobile", vehicle.driverMobile());
		row.put("status", vehicle.status());
		return row;
	}

	private Map<String, Object> driverRow(TransportDriverResponse driver) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("driverId", driver.id());
		row.put("displayName", driver.displayName());
		row.put("mobileNumber", driver.mobileNumber());
		row.put("licenseNumber", driver.licenseNumber());
		row.put("licenseExpiryDate", driver.licenseExpiryDate());
		row.put("address", driver.address());
		row.put("status", driver.status());
		return row;
	}

	private Map<String, Object> assignmentRow(TransportStudentAssignmentResponse assignment) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("assignmentId", assignment.assignmentId());
		row.put("studentId", assignment.studentId());
		row.put("admissionNumber", assignment.admissionNumber());
		row.put("studentName", assignment.studentName());
		row.put("academicYearId", assignment.academicYearId());
		row.put("academicYear", assignment.academicYear());
		row.put("classId", assignment.classId());
		row.put("className", assignment.className());
		row.put("sectionId", assignment.sectionId());
		row.put("sectionName", assignment.sectionName());
		row.put("vehicleId", assignment.vehicleId());
		row.put("vehicleNumber", assignment.vehicleNumber());
		row.put("vehicleName", assignment.vehicleName());
		row.put("routeId", assignment.routeId());
		row.put("routeName", assignment.routeName());
		row.put("pickupPointId", assignment.pickupPointId());
		row.put("pickupPointName", assignment.pickupPointName());
		row.put("assignmentDate", assignment.assignmentDate());
		row.put("endDate", assignment.endDate());
		row.put("status", assignment.status());
		row.put("feeAssignedStatus", assignment.feeAssignedStatus());
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
			UUID vehicleId,
			UUID routeId,
			UUID studentId,
			TransportStatus status) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("Academic Year ID", academicYearId);
		filters.put("Vehicle ID", vehicleId);
		filters.put("Route ID", routeId);
		filters.put("Student ID", studentId);
		filters.put("Status", status);
		return filters;
	}

	private String normalizeFormat(String format) {
		return format == null ? "EXCEL" : format.trim().toUpperCase(Locale.ROOT);
	}
}

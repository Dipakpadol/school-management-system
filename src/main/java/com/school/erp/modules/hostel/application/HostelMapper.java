package com.school.erp.modules.hostel.application;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.school.erp.modules.hostel.api.dto.HostelAllocationResponse;
import com.school.erp.modules.hostel.api.dto.HostelBedResponse;
import com.school.erp.modules.hostel.api.dto.HostelFeeStructureResponse;
import com.school.erp.modules.hostel.api.dto.HostelRoomDetailsResponse;
import com.school.erp.modules.hostel.api.dto.HostelRoomSummaryResponse;
import com.school.erp.modules.hostel.api.dto.HostelStudentRoomResponse;
import com.school.erp.modules.hostel.api.dto.HostelSummaryResponse;
import com.school.erp.modules.hostel.domain.Hostel;
import com.school.erp.modules.hostel.domain.HostelAllocation;
import com.school.erp.modules.hostel.domain.HostelBed;
import com.school.erp.modules.hostel.domain.HostelFeeStructure;
import com.school.erp.modules.hostel.domain.HostelRoom;
import com.school.erp.modules.students.domain.StudentClassAssignment;

import org.springframework.stereotype.Component;

@Component
public class HostelMapper {

	public HostelSummaryResponse toHostelSummary(Hostel hostel) {
		return new HostelSummaryResponse(
				hostel.getId(),
				hostel.getCode(),
				hostel.getName(),
				hostel.getAddress(),
				hostel.isActive());
	}

	public HostelRoomSummaryResponse toRoomSummary(
			HostelRoom room,
			int occupiedCount,
			Set<UUID> occupiedBedIds) {
		return new HostelRoomSummaryResponse(
				room.getId(),
				room.getHostel().getId(),
				room.getHostel().getName(),
				room.getRoomNumber(),
				room.getRoomType(),
				room.getCapacity(),
				occupiedCount,
				Math.max(room.getCapacity() - occupiedCount, 0),
				room.isHasBeds(),
				room.isActive(),
				bedResponses(room, occupiedBedIds));
	}

	public HostelRoomDetailsResponse toRoomDetails(
			HostelRoom room,
			int occupiedCount,
			Set<UUID> occupiedBedIds,
			List<HostelStudentRoomResponse> students) {
		return new HostelRoomDetailsResponse(
				room.getId(),
				room.getHostel().getId(),
				room.getHostel().getName(),
				room.getRoomNumber(),
				room.getRoomType(),
				room.getCapacity(),
				occupiedCount,
				Math.max(room.getCapacity() - occupiedCount, 0),
				room.isHasBeds(),
				bedResponses(room, occupiedBedIds),
				students);
	}

	public HostelAllocationResponse toAllocationResponse(HostelAllocation allocation) {
		return toAllocationResponse(allocation, "NOT_ASSIGNED");
	}

	public HostelAllocationResponse toAllocationResponse(HostelAllocation allocation, String feeAssignedStatus) {
		return new HostelAllocationResponse(
				allocation.getId(),
				allocation.getId(),
				allocation.getStudent().getId(),
				allocation.getStudent().getAdmissionNumber(),
				allocation.getStudent().getDisplayName(),
				allocation.getAcademicYear().getId(),
				allocation.getAcademicYear().getName(),
				allocation.getAcademicYear().getName(),
				allocation.getHostel().getId(),
				allocation.getHostel().getName(),
				allocation.getRoom().getId(),
				allocation.getRoom().getRoomNumber(),
				allocation.getRoom().getRoomType(),
				allocation.getBed() == null ? null : allocation.getBed().getId(),
				allocation.getBed() == null ? null : allocation.getBed().getBedNumber(),
				allocation.getAllocationDate(),
				allocation.getVacateDate(),
				allocation.getStatus(),
				feeAssignedStatus,
				allocation.getCreatedAt(),
				allocation.getUpdatedAt());
	}

	public HostelStudentRoomResponse toStudentRoomResponse(HostelAllocation allocation) {
		StudentClassAssignment assignment = allocation.getStudent().getClassAssignments().stream()
				.filter(existing -> !existing.isDeleted())
				.filter(StudentClassAssignment::isActive)
				.filter(existing -> existing.isForAcademicYear(allocation.getAcademicYear()))
				.findFirst()
				.orElseGet(() -> allocation.getStudent().getCurrentAssignment().orElse(null));
		return new HostelStudentRoomResponse(
				allocation.getId(),
				allocation.getStudent().getId(),
				allocation.getStudent().getDisplayName(),
				allocation.getStudent().getAdmissionNumber(),
				allocation.getAcademicYear().getId(),
				allocation.getAcademicYear().getName(),
				assignment == null || assignment.getClassEntity() == null ? null : assignment.getClassEntity().getId(),
				assignment == null || assignment.getSectionEntity() == null ? null : assignment.getSectionEntity().getId(),
				assignment == null ? null : assignment.getClassName(),
				assignment == null ? null : assignment.getSectionName(),
				allocation.getBed() == null ? null : allocation.getBed().getBedNumber(),
				allocation.getAllocationDate(),
				allocation.getVacateDate(),
				allocation.getStatus());
	}

	public HostelFeeStructureResponse toFeeStructureResponse(HostelFeeStructure structure) {
		return new HostelFeeStructureResponse(
				structure.getId(),
				structure.getAcademicYear().getId(),
				structure.getAcademicYear().getName(),
				structure.getHostel().getId(),
				structure.getHostel().getName(),
				structure.getRoom() == null ? null : structure.getRoom().getId(),
				structure.getRoom() == null ? null : structure.getRoom().getRoomNumber(),
				structure.getRoomType(),
				structure.getFeeCategory().getId(),
				structure.getFeeCategory().getCode(),
				structure.getFeeCategory().getName(),
				structure.getBackingFeeStructure().getId(),
				structure.getBackingFeeStructure().getName(),
				structure.getAmount(),
				structure.getDueDate(),
				structure.isInstallmentAllowed(),
				structure.getNumberOfInstallments(),
				structure.getStatus(),
				structure.getCreatedAt(),
				structure.getUpdatedAt());
	}

	private List<HostelBedResponse> bedResponses(HostelRoom room, Set<UUID> occupiedBedIds) {
		return room.getBeds().stream()
				.filter(bed -> !bed.isDeleted())
				.sorted(Comparator.comparing(HostelBed::getBedNumber))
				.map(bed -> new HostelBedResponse(
						bed.getId(),
						bed.getBedNumber(),
						bed.isActive(),
						occupiedBedIds.contains(bed.getId())))
				.toList();
	}
}

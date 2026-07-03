package com.school.erp.modules.attendance.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.domain.TeacherAttendanceRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherAttendanceRecordRepository extends BaseRepository<TeacherAttendanceRecord, UUID> {

	@EntityGraph(attributePaths = { "teacher", "academicYear" })
	@Query("""
			select record
			from TeacherAttendanceRecord record
			where record.deleted = false
			  and record.academicYear.id = :academicYearId
			  and record.attendanceDate = :attendanceDate
			order by record.teacher.firstName asc, record.teacher.lastName asc
			""")
	List<TeacherAttendanceRecord> findDailyRecords(
			@Param("academicYearId") UUID academicYearId,
			@Param("attendanceDate") LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "teacher", "academicYear" })
	@Query("""
			select record
			from TeacherAttendanceRecord record
			where record.deleted = false
			  and record.teacher.id = :teacherId
			  and (:academicYearId is null or record.academicYear.id = :academicYearId)
			  and (:fromDate is null or record.attendanceDate >= :fromDate)
			  and (:toDate is null or record.attendanceDate <= :toDate)
			  and (:status is null or record.status = :status)
			""")
	Page<TeacherAttendanceRecord> findTeacherHistory(
			@Param("teacherId") UUID teacherId,
			@Param("academicYearId") UUID academicYearId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("status") AttendanceStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = { "teacher", "academicYear" })
	@Query("""
			select record
			from TeacherAttendanceRecord record
			where record.deleted = false
			  and record.teacher.id = :teacherId
			  and (:academicYearId is null or record.academicYear.id = :academicYearId)
			  and (:fromDate is null or record.attendanceDate >= :fromDate)
			  and (:toDate is null or record.attendanceDate <= :toDate)
			  and (:status is null or record.status = :status)
			order by record.attendanceDate desc
			""")
	List<TeacherAttendanceRecord> findTeacherHistoryForSummary(
			@Param("teacherId") UUID teacherId,
			@Param("academicYearId") UUID academicYearId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("status") AttendanceStatus status);

	@EntityGraph(attributePaths = { "teacher", "academicYear" })
	@Query("""
			select record
			from TeacherAttendanceRecord record
			where record.deleted = false
			  and record.academicYear.id = :academicYearId
			  and record.attendanceDate between :fromDate and :toDate
			order by record.attendanceDate asc, record.teacher.firstName asc, record.teacher.lastName asc
			""")
	List<TeacherAttendanceRecord> findForExport(
			@Param("academicYearId") UUID academicYearId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);
}

package com.school.erp.modules.attendance.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.domain.AttendanceRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends BaseRepository<AttendanceRecord, UUID> {

	@EntityGraph(attributePaths = { "student" })
	@Query("""
			select record
			from AttendanceRecord record
			where record.deleted = false
			  and record.academicYear.id = :academicYearId
			  and record.classEntity.id = :classId
			  and record.section.id = :sectionId
			  and record.attendanceDate = :attendanceDate
			order by record.student.firstName asc, record.student.lastName asc
			""")
	List<AttendanceRecord> findDailyRecords(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("attendanceDate") LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "student", "academicYear", "classEntity", "section" })
	@Query("""
			select record
			from AttendanceRecord record
			where record.deleted = false
			  and record.attendanceDate = :attendanceDate
			  and (:academicYearId is null or record.academicYear.id = :academicYearId)
			  and (:classId is null or record.classEntity.id = :classId)
			  and (:sectionId is null or record.section.id = :sectionId)
			order by record.classEntity.displayOrder asc, record.section.displayOrder asc, record.student.firstName asc
			""")
	List<AttendanceRecord> findTodayDashboardRecords(
			@Param("attendanceDate") LocalDate attendanceDate,
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId);

	@EntityGraph(attributePaths = { "student" })
	@Query("""
			select record
			from AttendanceRecord record
			where record.deleted = false
			  and record.student.id = :studentId
			  and record.academicYear.id = :academicYearId
			order by record.attendanceDate asc
			""")
	List<AttendanceRecord> findByStudentAndAcademicYear(
			@Param("studentId") UUID studentId,
			@Param("academicYearId") UUID academicYearId);

	@EntityGraph(attributePaths = { "student", "academicYear", "classEntity", "section" })
	@Query("""
			select record
			from AttendanceRecord record
			where record.deleted = false
			  and record.student.id = :studentId
			  and (:academicYearId is null or record.academicYear.id = :academicYearId)
			  and (:fromDate is null or record.attendanceDate >= :fromDate)
			  and (:toDate is null or record.attendanceDate <= :toDate)
			  and (:status is null or record.status = :status)
			""")
	Page<AttendanceRecord> findStudentHistory(
			@Param("studentId") UUID studentId,
			@Param("academicYearId") UUID academicYearId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("status") AttendanceStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = { "student", "academicYear", "classEntity", "section" })
	@Query("""
			select record
			from AttendanceRecord record
			where record.deleted = false
			  and record.student.id = :studentId
			  and (:academicYearId is null or record.academicYear.id = :academicYearId)
			  and (:fromDate is null or record.attendanceDate >= :fromDate)
			  and (:toDate is null or record.attendanceDate <= :toDate)
			  and (:status is null or record.status = :status)
			order by record.attendanceDate desc
			""")
	List<AttendanceRecord> findStudentHistoryForSummary(
			@Param("studentId") UUID studentId,
			@Param("academicYearId") UUID academicYearId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("status") AttendanceStatus status);

	@EntityGraph(attributePaths = { "student" })
	@Query("""
			select record
			from AttendanceRecord record
			where record.deleted = false
			  and record.academicYear.id = :academicYearId
			  and record.classEntity.id = :classId
			  and record.section.id = :sectionId
			  and record.attendanceDate between :fromDate and :toDate
			order by record.attendanceDate asc, record.student.firstName asc, record.student.lastName asc
			""")
	List<AttendanceRecord> findForExport(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);
}

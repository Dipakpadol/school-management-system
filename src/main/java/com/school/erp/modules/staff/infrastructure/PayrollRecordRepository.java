package com.school.erp.modules.staff.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.PayrollRecord;
import com.school.erp.modules.staff.domain.PayrollStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRecordRepository extends BaseRepository<PayrollRecord, UUID> {

	boolean existsByStaffIdAndPayrollYearAndPayrollMonthAndDeletedFalse(UUID staffId, int payrollYear, int payrollMonth);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation" })
	@Query("""
			select record
			from PayrollRecord record
			where record.deleted = false
			  and (:staffId is null or record.staff.id = :staffId)
			  and (:payrollYear is null or record.payrollYear = :payrollYear)
			  and (:payrollMonth is null or record.payrollMonth = :payrollMonth)
			  and (:status is null or record.status = :status)
			""")
	Page<PayrollRecord> search(
			@Param("staffId") UUID staffId,
			@Param("payrollYear") Integer payrollYear,
			@Param("payrollMonth") Integer payrollMonth,
			@Param("status") PayrollStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation" })
	@Query("""
			select record
			from PayrollRecord record
			where record.deleted = false
			  and (:staffId is null or record.staff.id = :staffId)
			  and (:payrollYear is null or record.payrollYear = :payrollYear)
			  and (:payrollMonth is null or record.payrollMonth = :payrollMonth)
			  and (:status is null or record.status = :status)
			order by record.payrollYear desc, record.payrollMonth desc, record.staff.firstName asc
			""")
	List<PayrollRecord> findReportRows(
			@Param("staffId") UUID staffId,
			@Param("payrollYear") Integer payrollYear,
			@Param("payrollMonth") Integer payrollMonth,
			@Param("status") PayrollStatus status);
}

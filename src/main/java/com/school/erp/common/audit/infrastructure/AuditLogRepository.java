package com.school.erp.common.audit.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.audit.domain.AuditLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

	@Query("select distinct log.moduleName from AuditLog log order by log.moduleName")
	List<String> findDistinctModuleNames();

	@Query("select distinct log.action from AuditLog log order by log.action")
	List<String> findDistinctActions();

	@Query("select distinct log.performedBy from AuditLog log where log.performedBy is not null order by log.performedBy")
	List<String> findDistinctPerformedBy();
}

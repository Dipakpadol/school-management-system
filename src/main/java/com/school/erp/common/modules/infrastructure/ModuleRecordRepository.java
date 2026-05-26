package com.school.erp.common.modules.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.modules.domain.ModuleRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ModuleRecordRepository
		extends JpaRepository<ModuleRecord, UUID>, JpaSpecificationExecutor<ModuleRecord> {

	Optional<ModuleRecord> findByIdAndModuleNameAndRecordTypeAndDeletedFalse(
			UUID id,
			String moduleName,
			String recordType);

	boolean existsByModuleNameAndRecordTypeAndCodeIgnoreCaseAndDeletedFalse(
			String moduleName,
			String recordType,
			String code);

	boolean existsByModuleNameAndRecordTypeAndCodeIgnoreCaseAndIdNotAndDeletedFalse(
			String moduleName,
			String recordType,
			String code,
			UUID id);

	long countByModuleNameAndRecordTypeAndDeletedFalse(String moduleName, String recordType);

	long countByModuleNameAndRecordTypeAndActiveTrueAndDeletedFalse(String moduleName, String recordType);

	@Query("""
			select r.recordType, count(r.id)
			from ModuleRecord r
			where r.moduleName = :moduleName and r.deleted = false
			group by r.recordType
			order by r.recordType
			""")
	List<Object[]> countByRecordType(String moduleName);
}

package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.transport.domain.TransportDriver;

public interface TransportDriverRepository extends BaseRepository<TransportDriver, UUID> {

	List<TransportDriver> findAllByDeletedFalseOrderByFirstNameAscLastNameAsc();

	Optional<TransportDriver> findByLicenseNumberIgnoreCaseAndDeletedFalse(String licenseNumber);

	boolean existsByMobileNumberAndDeletedFalse(String mobileNumber);
}

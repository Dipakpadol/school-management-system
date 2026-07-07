package com.school.erp.modules.settings.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.settings.domain.ApplicationSetting;

public interface ApplicationSettingRepository extends BaseRepository<ApplicationSetting, UUID> {

	List<ApplicationSetting> findAllByDeletedFalseOrderByGroupNameAscSettingKeyAsc();

	Optional<ApplicationSetting> findByGroupNameIgnoreCaseAndSettingKeyIgnoreCaseAndDeletedFalse(
			String groupName,
			String settingKey);
}

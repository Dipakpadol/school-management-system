package com.school.erp.modules.notifications.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.notifications.domain.NotificationTemplate;

public interface NotificationTemplateRepository extends BaseRepository<NotificationTemplate, UUID> {

	boolean existsByTemplateCodeIgnoreCaseAndDeletedFalse(String templateCode);

	boolean existsByTemplateCodeIgnoreCaseAndIdNotAndDeletedFalse(String templateCode, UUID id);

	Optional<NotificationTemplate> findByTemplateCodeIgnoreCaseAndDeletedFalse(String templateCode);
}

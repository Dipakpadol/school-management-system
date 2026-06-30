package com.school.erp.modules.notifications.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationChannelConfig;

public interface NotificationChannelConfigRepository extends BaseRepository<NotificationChannelConfig, UUID> {

	Optional<NotificationChannelConfig> findByChannelAndDeletedFalse(NotificationChannel channel);
}

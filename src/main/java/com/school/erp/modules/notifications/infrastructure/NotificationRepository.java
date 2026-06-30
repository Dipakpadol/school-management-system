package com.school.erp.modules.notifications.infrastructure;

import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.notifications.domain.Notification;

public interface NotificationRepository extends BaseRepository<Notification, UUID> {
}

import '../../../../core/result/result.dart';
import '../../data/models/notification_models.dart';

abstract interface class NotificationsRepository {
  Future<Result<NotificationLogModel>> sendTest({
    required String channel,
    required String recipient,
    required String subject,
    required String message,
  });

  Future<Result<List<NotificationLogModel>>> logs();

  Future<Result<List<NotificationTemplateModel>>> templates();

  Future<Result<NotificationTemplateModel>> createTemplate(
    Map<String, dynamic> payload,
  );

  Future<Result<NotificationTemplateModel>> updateTemplate(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<void>> deleteTemplate(String id);
}

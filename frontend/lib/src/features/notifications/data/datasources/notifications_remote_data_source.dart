import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/notification_models.dart';

final notificationsRemoteDataSourceProvider =
    Provider<NotificationsRemoteDataSource>((ref) {
      return NotificationsRemoteDataSource(ref.watch(apiClientProvider));
    });

class NotificationsRemoteDataSource {
  const NotificationsRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<NotificationLogModel> sendTest({
    required String channel,
    required String recipient,
    required String subject,
    required String message,
  }) async {
    final normalized = channel.toUpperCase();
    final path = switch (normalized) {
      'SMS' => ApiPaths.notificationTestSms,
      'WHATSAPP' => ApiPaths.notificationTestWhatsApp,
      _ => ApiPaths.notificationTestEmail,
    };
    final payload = switch (normalized) {
      'SMS' => {'mobileNumber': recipient, 'message': message},
      'WHATSAPP' => {'mobileNumber': recipient, 'message': message},
      _ => {'to': recipient, 'subject': subject, 'message': message},
    };
    final response = await _apiClient.post<Map<String, dynamic>>(
      path,
      data: payload,
    );
    return NotificationLogModel.fromJson(_unwrapData(response.data));
  }

  Future<List<NotificationLogModel>> logs() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.notificationLogs,
      queryParameters: {'size': 50, 'sortBy': 'createdAt', 'direction': 'DESC'},
    );
    return _unwrapList(response.data, NotificationLogModel.fromJson);
  }

  Future<List<NotificationTemplateModel>> templates() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.notificationTemplates,
      queryParameters: {'size': 100},
    );
    return _unwrapList(response.data, NotificationTemplateModel.fromJson);
  }

  Future<NotificationTemplateModel> createTemplate(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.notificationTemplates,
      data: payload,
    );
    return NotificationTemplateModel.fromJson(_unwrapData(response.data));
  }

  Future<NotificationTemplateModel> updateTemplate(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.notificationTemplate(id),
      data: payload,
    );
    return NotificationTemplateModel.fromJson(_unwrapData(response.data));
  }

  Future<void> deleteTemplate(String id) async {
    await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.notificationTemplate(id),
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }

  List<T> _unwrapList<T>(
    Map<String, dynamic>? body,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final data = body?['data'];
    final content = data is Map<String, dynamic> ? data['content'] : data;
    if (content is List) {
      return content.whereType<Map<String, dynamic>>().map(mapper).toList();
    }
    throw const FormatException('Response payload is invalid.');
  }
}

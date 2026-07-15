import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/network/dio_provider.dart';
import '../models/public_website_models.dart';

final publicEnquiryServiceProvider = Provider<PublicEnquiryService>((ref) {
  return ApiPublicEnquiryService(ref.watch(dioProvider));
});

abstract interface class PublicEnquiryService {
  Future<void> submit(EnquiryRequest request);
}

class ApiPublicEnquiryService implements PublicEnquiryService {
  const ApiPublicEnquiryService(this._dio);

  final Dio _dio;

  @override
  Future<void> submit(EnquiryRequest request) async {
    try {
      await _dio.post<void>(
        ApiPaths.publicEnquiries,
        data: {
          'studentName': request.studentName.trim(),
          'parentName': request.parentName.trim(),
          'mobileNumber': request.mobileNumber.trim(),
          'email': request.email.trim(),
          'classInterested': request.classInterested.trim(),
          'message': request.message.trim(),
        },
      );
    } on DioException catch (error) {
      if (kDebugMode) {
        debugPrint(
          'Public enquiry failed with status: ${error.response?.statusCode}',
        );
        debugPrint('Public enquiry response body: ${error.response?.data}');
        debugPrint('Public enquiry Dio exception type: ${error.type}');
      }

      throw PublicEnquiryException(_extractMessage(error), cause: error);
    } catch (error) {
      if (kDebugMode) {
        debugPrint('Unexpected public enquiry error: $error');
      }

      throw PublicEnquiryException(
        'Unable to submit the enquiry. Please try again.',
        cause: error,
      );
    }
  }

  String _extractMessage(DioException error) {
    if (error.response?.statusCode == 429) {
      return 'Too many enquiry requests. Please try again later.';
    }

    final data = error.response?.data;

    if (data is Map) {
      final message =
          data['message'] ?? data['displayMessage'] ?? data['error'];

      if (message != null && message.toString().trim().isNotEmpty) {
        return message.toString();
      }
    }

    return 'Unable to submit the enquiry. Please try again.';
  }
}

class PublicEnquiryException implements Exception {
  const PublicEnquiryException(this.message, {this.cause});

  final String message;
  final Object? cause;

  @override
  String toString() => message;
}

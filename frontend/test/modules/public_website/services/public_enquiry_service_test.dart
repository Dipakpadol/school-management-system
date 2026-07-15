import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:school_erp/src/core/constants/api_paths.dart';
import 'package:school_erp/src/modules/public_website/models/public_website_models.dart';
import 'package:school_erp/src/modules/public_website/services/public_enquiry_service.dart';

void main() {
  group('ApiPublicEnquiryService', () {
    test('successful POST submission sends trimmed request payload', () async {
      late RequestOptions captured;
      final dio = _dio((options) async {
        captured = options;
        return ResponseBody.fromString('', 204);
      });

      await ApiPublicEnquiryService(dio).submit(_request());

      expect(captured.method, 'POST');
      expect(captured.path, ApiPaths.publicEnquiries);
      expect(captured.data, {
        'studentName': 'Asha Student',
        'parentName': 'Riya Parent',
        'mobileNumber': '+91 98765 43210',
        'email': 'parent@example.com',
        'classInterested': 'Primary',
        'message': 'Please call after 4 PM.',
      });
    });

    test('backend validation error extracts message', () async {
      final service = ApiPublicEnquiryService(
        _dio(
          (_) async =>
              _jsonResponse(400, {'message': 'Request validation failed'}),
        ),
      );

      await expectLater(
        service.submit(_request()),
        throwsA(
          isA<PublicEnquiryException>().having(
            (error) => error.message,
            'message',
            'Request validation failed',
          ),
        ),
      );
    });

    test('backend SMTP failure extracts displayMessage', () async {
      final service = ApiPublicEnquiryService(
        _dio(
          (_) async => _jsonResponse(422, {
            'displayMessage': 'Unable to submit the enquiry. Please try again.',
          }),
        ),
      );

      await expectLater(
        service.submit(_request()),
        throwsA(
          isA<PublicEnquiryException>().having(
            (error) => error.message,
            'message',
            'Unable to submit the enquiry. Please try again.',
          ),
        ),
      );
    });

    test('network failure uses generic fallback message', () async {
      final service = ApiPublicEnquiryService(
        _dio((options) async {
          throw DioException.connectionError(
            requestOptions: options,
            reason: 'offline',
          );
        }),
      );

      await expectLater(
        service.submit(_request()),
        throwsA(
          isA<PublicEnquiryException>().having(
            (error) => error.message,
            'message',
            'Unable to submit the enquiry. Please try again.',
          ),
        ),
      );
    });

    test('HTTP 429 response uses rate-limit message', () async {
      final service = ApiPublicEnquiryService(
        _dio(
          (_) async => _jsonResponse(429, {'message': 'internal rate message'}),
        ),
      );

      await expectLater(
        service.submit(_request()),
        throwsA(
          isA<PublicEnquiryException>().having(
            (error) => error.message,
            'message',
            'Too many enquiry requests. Please try again later.',
          ),
        ),
      );
    });

    test('backend error field is extracted', () async {
      final service = ApiPublicEnquiryService(
        _dio(
          (_) async =>
              _jsonResponse(500, {'error': 'Email provider unavailable'}),
        ),
      );

      await expectLater(
        service.submit(_request()),
        throwsA(
          isA<PublicEnquiryException>().having(
            (error) => error.message,
            'message',
            'Email provider unavailable',
          ),
        ),
      );
    });

    test('empty backend error response uses generic fallback', () async {
      final service = ApiPublicEnquiryService(
        _dio((_) async => ResponseBody.fromString('', 500)),
      );

      await expectLater(
        service.submit(_request()),
        throwsA(
          isA<PublicEnquiryException>().having(
            (error) => error.message,
            'message',
            'Unable to submit the enquiry. Please try again.',
          ),
        ),
      );
    });
  });
}

Dio _dio(Future<ResponseBody> Function(RequestOptions options) handler) {
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api'));
  dio.httpClientAdapter = _FakeAdapter(handler);
  return dio;
}

ResponseBody _jsonResponse(int statusCode, Object body) {
  return ResponseBody.fromString(
    jsonEncode(body),
    statusCode,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}

EnquiryRequest _request() {
  return const EnquiryRequest(
    studentName: ' Asha Student ',
    parentName: ' Riya Parent ',
    mobileNumber: ' +91 98765 43210 ',
    email: ' parent@example.com ',
    classInterested: ' Primary ',
    message: ' Please call after 4 PM. ',
  );
}

class _FakeAdapter implements HttpClientAdapter {
  _FakeAdapter(this.handler);

  final Future<ResponseBody> Function(RequestOptions options) handler;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    return handler(options);
  }

  @override
  void close({bool force = false}) {}
}

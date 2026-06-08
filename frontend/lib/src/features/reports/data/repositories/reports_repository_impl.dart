import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/download/file_downloader.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/reports_repository.dart';
import '../datasources/reports_remote_data_source.dart';
import '../models/report_options_model.dart';

final reportsRepositoryProvider = Provider<ReportsRepository>((ref) {
  return ReportsRepositoryImpl(ref.watch(reportsRemoteDataSourceProvider));
});

class ReportsRepositoryImpl implements ReportsRepository {
  const ReportsRepositoryImpl(this._remoteDataSource);

  final ReportsRemoteDataSource _remoteDataSource;

  @override
  Future<Result<ReportOptionsModel>> options() {
    return _guard(_remoteDataSource.options);
  }

  @override
  Future<Result<void>> export(Map<String, dynamic> query) {
    return _guard(() async {
      final bytes = await _remoteDataSource.export(query);
      final reportType = (query['reportType'] as String? ?? 'report')
          .toLowerCase()
          .replaceAll('_', '-');
      final format = (query['format'] as String? ?? 'CSV').toLowerCase();
      await downloadBytes(bytes, '$reportType.$format', _contentType(format));
    });
  }

  Future<Result<T>> _guard<T>(Future<T> Function() action) async {
    try {
      return Success(await action());
    } on DioException catch (error) {
      return FailureResult(_failureFromDio(error));
    } on FormatException catch (error) {
      return FailureResult(ValidationFailure(error.message));
    } catch (_) {
      return const FailureResult(UnexpectedFailure());
    }
  }

  Failure _failureFromDio(DioException error) {
    final statusCode = error.response?.statusCode;
    if (statusCode == 401) {
      return const UnauthorizedFailure();
    }
    if (statusCode == 400 ||
        statusCode == 403 ||
        statusCode == 409 ||
        statusCode == 422) {
      return ValidationFailure(
        _serverMessage(error) ?? 'Report request is invalid.',
      );
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(_serverMessage(error) ?? 'Report export failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }

  String _contentType(String format) {
    return switch (format) {
      'pdf' => 'application/pdf',
      'xlsx' || 'excel' =>
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      _ => 'text/csv',
    };
  }
}

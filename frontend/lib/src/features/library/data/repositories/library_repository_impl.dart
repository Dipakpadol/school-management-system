import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/library_repository.dart';
import '../datasources/library_remote_data_source.dart';
import '../models/library_models.dart';

final libraryRepositoryProvider = Provider<LibraryRepository>((ref) {
  return LibraryRepositoryImpl(ref.watch(libraryRemoteDataSourceProvider));
});

class LibraryRepositoryImpl implements LibraryRepository {
  const LibraryRepositoryImpl(this._remote);

  final LibraryRemoteDataSource _remote;

  @override
  Future<Result<LibrarySummaryModel>> summary() => _guard(_remote.summary);

  @override
  Future<Result<List<LibraryCategoryModel>>> categories() =>
      _guard(_remote.categories);

  @override
  Future<Result<LibraryCategoryModel>> createCategory(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createCategory(payload));

  @override
  Future<Result<LibraryCategoryModel>> updateCategory(
    String categoryId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateCategory(categoryId, payload));

  @override
  Future<Result<List<LibraryAuthorModel>>> authors() => _guard(_remote.authors);

  @override
  Future<Result<LibraryAuthorModel>> createAuthor(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createAuthor(payload));

  @override
  Future<Result<LibraryAuthorModel>> updateAuthor(
    String authorId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateAuthor(authorId, payload));

  @override
  Future<Result<List<LibraryPublisherModel>>> publishers() =>
      _guard(_remote.publishers);

  @override
  Future<Result<LibraryPublisherModel>> createPublisher(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createPublisher(payload));

  @override
  Future<Result<LibraryPublisherModel>> updatePublisher(
    String publisherId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updatePublisher(publisherId, payload));

  @override
  Future<Result<PagePayload<LibraryBookModel>>> books(
    LibraryBookFilter filter,
  ) => _guard(() => _remote.books(filter));

  @override
  Future<Result<LibraryBookModel>> createBook(Map<String, dynamic> payload) =>
      _guard(() => _remote.createBook(payload));

  @override
  Future<Result<LibraryBookModel>> updateBook(
    String bookId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateBook(bookId, payload));

  @override
  Future<Result<LibraryBookModel>> deactivateBook(String bookId) =>
      _guard(() => _remote.deactivateBook(bookId));

  @override
  Future<Result<PagePayload<LibraryBookCopyModel>>> copies(
    LibraryCopyFilter filter,
  ) => _guard(() => _remote.copies(filter));

  @override
  Future<Result<LibraryBookCopyModel>> createCopy(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createCopy(payload));

  @override
  Future<Result<LibraryBookCopyModel>> updateCopy(
    String copyId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateCopy(copyId, payload));

  @override
  Future<Result<LibraryBookCopyModel>> markCopy(
    String copyId,
    String action, {
    String? note,
  }) => _guard(() => _remote.markCopy(copyId, action, note: note));

  @override
  Future<Result<PagePayload<LibraryMembershipModel>>> memberships(
    LibraryMembershipFilter filter,
  ) => _guard(() => _remote.memberships(filter));

  @override
  Future<Result<LibraryMembershipModel>> createMembership(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createMembership(payload));

  @override
  Future<Result<LibraryMembershipModel>> updateMembership(
    String membershipId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateMembership(membershipId, payload));

  @override
  Future<Result<LibraryMembershipModel>> deactivateMembership(
    String membershipId,
  ) => _guard(() => _remote.deactivateMembership(membershipId));

  @override
  Future<Result<PagePayload<LibraryLoanModel>>> loans(
    LibraryLoanFilter filter,
  ) => _guard(() => _remote.loans(filter));

  @override
  Future<Result<LibraryLoanModel>> issue(Map<String, dynamic> payload) =>
      _guard(() => _remote.issue(payload));

  @override
  Future<Result<LibraryLoanModel>> returnLoan(
    String loanId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.returnLoan(loanId, payload));

  @override
  Future<Result<LibraryLoanModel>> markLoanLost(
    String loanId, {
    String? note,
  }) => _guard(() => _remote.markLoanLost(loanId, note: note));

  @override
  Future<Result<PagePayload<LibraryFineModel>>> fines(
    LibraryFineFilter filter,
  ) => _guard(() => _remote.fines(filter));

  @override
  Future<Result<LibraryFineModel>> payFine(
    String fineId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.payFine(fineId, payload));

  @override
  Future<Result<LibraryFineModel>> waiveFine(String fineId) =>
      _guard(() => _remote.waiveFine(fineId));

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
    if (statusCode == 403) {
      return const ValidationFailure('You do not have permission.');
    }
    if (statusCode == 400 || statusCode == 409 || statusCode == 422) {
      return ValidationFailure(_serverMessage(error) ?? 'Request is invalid.');
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(_serverMessage(error) ?? 'Request failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }
}

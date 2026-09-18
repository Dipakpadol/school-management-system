import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart';
import '../models/library_models.dart';

final libraryRemoteDataSourceProvider = Provider<LibraryRemoteDataSource>((
  ref,
) {
  return LibraryRemoteDataSource(ref.watch(apiClientProvider));
});

class LibraryRemoteDataSource {
  const LibraryRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<LibrarySummaryModel> summary() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.librarySummary,
    );
    return LibrarySummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<List<LibraryCategoryModel>> categories() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryCategories,
    );
    return _unwrapList(response.data, LibraryCategoryModel.fromJson);
  }

  Future<LibraryCategoryModel> createCategory(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.libraryCategories,
      data: payload,
    );
    return LibraryCategoryModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryCategoryModel> updateCategory(
    String categoryId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.libraryCategory(categoryId),
      data: payload,
    );
    return LibraryCategoryModel.fromJson(_unwrapData(response.data));
  }

  Future<List<LibraryAuthorModel>> authors() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryAuthors,
    );
    return _unwrapList(response.data, LibraryAuthorModel.fromJson);
  }

  Future<LibraryAuthorModel> createAuthor(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.libraryAuthors,
      data: payload,
    );
    return LibraryAuthorModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryAuthorModel> updateAuthor(
    String authorId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.libraryAuthor(authorId),
      data: payload,
    );
    return LibraryAuthorModel.fromJson(_unwrapData(response.data));
  }

  Future<List<LibraryPublisherModel>> publishers() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryPublishers,
    );
    return _unwrapList(response.data, LibraryPublisherModel.fromJson);
  }

  Future<LibraryPublisherModel> createPublisher(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.libraryPublishers,
      data: payload,
    );
    return LibraryPublisherModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryPublisherModel> updatePublisher(
    String publisherId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.libraryPublisher(publisherId),
      data: payload,
    );
    return LibraryPublisherModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryBookPage> books(LibraryBookFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryBooks,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(_unwrapData(response.data), LibraryBookModel.fromJson);
  }

  Future<LibraryBookModel> createBook(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.libraryBooks,
      data: payload,
    );
    return LibraryBookModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryBookModel> updateBook(
    String bookId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.libraryBook(bookId),
      data: payload,
    );
    return LibraryBookModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryBookModel> deactivateBook(String bookId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.libraryBookDeactivate(bookId),
    );
    return LibraryBookModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryCopyPage> copies(LibraryCopyFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryCopies,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      LibraryBookCopyModel.fromJson,
    );
  }

  Future<LibraryBookCopyModel> createCopy(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.libraryCopies,
      data: payload,
    );
    return LibraryBookCopyModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryBookCopyModel> updateCopy(
    String copyId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.libraryCopy(copyId),
      data: payload,
    );
    return LibraryBookCopyModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryBookCopyModel> markCopy(
    String copyId,
    String action, {
    String? note,
  }) async {
    final path = switch (action) {
      'lost' => ApiPaths.libraryCopyLost(copyId),
      'damaged' => ApiPaths.libraryCopyDamaged(copyId),
      _ => ApiPaths.libraryCopyAvailable(copyId),
    };
    final response = await _apiClient.patch<Map<String, dynamic>>(
      path,
      data: note == null || note.trim().isEmpty ? null : {'note': note.trim()},
    );
    return LibraryBookCopyModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryMembershipPage> memberships(
    LibraryMembershipFilter filter,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryMemberships,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      LibraryMembershipModel.fromJson,
    );
  }

  Future<LibraryMembershipModel> createMembership(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.libraryMemberships,
      data: payload,
    );
    return LibraryMembershipModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryMembershipModel> updateMembership(
    String membershipId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.libraryMembership(membershipId),
      data: payload,
    );
    return LibraryMembershipModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryMembershipModel> deactivateMembership(
    String membershipId,
  ) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.libraryMembershipDeactivate(membershipId),
    );
    return LibraryMembershipModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryLoanPage> loans(LibraryLoanFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryLoans,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      LibraryLoanModel.fromJson,
    );
  }

  Future<LibraryLoanModel> issue(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.libraryIssueLoan,
      data: payload,
    );
    return LibraryLoanModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryLoanModel> returnLoan(
    String loanId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.libraryLoanReturn(loanId),
      data: payload,
    );
    return LibraryLoanModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryLoanModel> markLoanLost(String loanId, {String? note}) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.libraryLoanLost(loanId),
      data: note == null || note.trim().isEmpty ? null : {'note': note.trim()},
    );
    return LibraryLoanModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryFinePage> fines(LibraryFineFilter filter) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.libraryFines,
      queryParameters: filter.toQuery(),
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      LibraryFineModel.fromJson,
    );
  }

  Future<LibraryFineModel> payFine(
    String fineId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.libraryFinePay(fineId),
      data: payload,
    );
    return LibraryFineModel.fromJson(_unwrapData(response.data));
  }

  Future<LibraryFineModel> waiveFine(String fineId) async {
    final response = await _apiClient.patch<Map<String, dynamic>>(
      ApiPaths.libraryFineWaive(fineId),
    );
    return LibraryFineModel.fromJson(_unwrapData(response.data));
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Library response payload is invalid.');
  }

  List<T> _unwrapList<T>(
    Map<String, dynamic>? body,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final data = body?['data'];
    if (data is List) {
      return data
          .whereType<Map<String, dynamic>>()
          .map(mapper)
          .toList(growable: false);
    }
    throw const FormatException('Library response payload is invalid.');
  }
}

import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../data/models/library_models.dart';

abstract interface class LibraryRepository {
  Future<Result<LibrarySummaryModel>> summary();

  Future<Result<List<LibraryCategoryModel>>> categories();

  Future<Result<LibraryCategoryModel>> createCategory(
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryCategoryModel>> updateCategory(
    String categoryId,
    Map<String, dynamic> payload,
  );

  Future<Result<List<LibraryAuthorModel>>> authors();

  Future<Result<LibraryAuthorModel>> createAuthor(
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryAuthorModel>> updateAuthor(
    String authorId,
    Map<String, dynamic> payload,
  );

  Future<Result<List<LibraryPublisherModel>>> publishers();

  Future<Result<LibraryPublisherModel>> createPublisher(
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryPublisherModel>> updatePublisher(
    String publisherId,
    Map<String, dynamic> payload,
  );

  Future<Result<PagePayload<LibraryBookModel>>> books(
    LibraryBookFilter filter,
  );

  Future<Result<LibraryBookModel>> createBook(Map<String, dynamic> payload);

  Future<Result<LibraryBookModel>> updateBook(
    String bookId,
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryBookModel>> deactivateBook(String bookId);

  Future<Result<PagePayload<LibraryBookCopyModel>>> copies(
    LibraryCopyFilter filter,
  );

  Future<Result<LibraryBookCopyModel>> createCopy(
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryBookCopyModel>> updateCopy(
    String copyId,
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryBookCopyModel>> markCopy(
    String copyId,
    String action, {
    String? note,
  });

  Future<Result<PagePayload<LibraryMembershipModel>>> memberships(
    LibraryMembershipFilter filter,
  );

  Future<Result<LibraryMembershipModel>> createMembership(
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryMembershipModel>> updateMembership(
    String membershipId,
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryMembershipModel>> deactivateMembership(
    String membershipId,
  );

  Future<Result<PagePayload<LibraryLoanModel>>> loans(
    LibraryLoanFilter filter,
  );

  Future<Result<LibraryLoanModel>> issue(Map<String, dynamic> payload);

  Future<Result<LibraryLoanModel>> returnLoan(
    String loanId,
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryLoanModel>> markLoanLost(
    String loanId, {
    String? note,
  });

  Future<Result<PagePayload<LibraryFineModel>>> fines(
    LibraryFineFilter filter,
  );

  Future<Result<LibraryFineModel>> payFine(
    String fineId,
    Map<String, dynamic> payload,
  );

  Future<Result<LibraryFineModel>> waiveFine(String fineId);
}

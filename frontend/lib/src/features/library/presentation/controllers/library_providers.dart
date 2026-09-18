import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../data/models/library_models.dart';
import '../../data/repositories/library_repository_impl.dart';

final librarySummaryProvider = FutureProvider<LibrarySummaryModel>((ref) {
  return _resolve(ref.watch(libraryRepositoryProvider).summary());
});

final libraryCategoriesProvider = FutureProvider<List<LibraryCategoryModel>>((
  ref,
) {
  return _resolve(ref.watch(libraryRepositoryProvider).categories());
});

final libraryAuthorsProvider = FutureProvider<List<LibraryAuthorModel>>((ref) {
  return _resolve(ref.watch(libraryRepositoryProvider).authors());
});

final libraryPublishersProvider = FutureProvider<List<LibraryPublisherModel>>((
  ref,
) {
  return _resolve(ref.watch(libraryRepositoryProvider).publishers());
});

final libraryBooksProvider =
    FutureProvider.family<PagePayload<LibraryBookModel>, LibraryBookFilter>((
      ref,
      filter,
    ) {
      return _resolve(ref.watch(libraryRepositoryProvider).books(filter));
    });

final libraryCopiesProvider =
    FutureProvider.family<PagePayload<LibraryBookCopyModel>, LibraryCopyFilter>(
  (ref, filter) {
    return _resolve(ref.watch(libraryRepositoryProvider).copies(filter));
  },
);

final libraryMembershipsProvider = FutureProvider.family<
    PagePayload<LibraryMembershipModel>, LibraryMembershipFilter>((ref, filter) {
  return _resolve(ref.watch(libraryRepositoryProvider).memberships(filter));
});

final libraryLoansProvider =
    FutureProvider.family<PagePayload<LibraryLoanModel>, LibraryLoanFilter>((
      ref,
      filter,
    ) {
      return _resolve(ref.watch(libraryRepositoryProvider).loans(filter));
    });

final libraryFinesProvider =
    FutureProvider.family<PagePayload<LibraryFineModel>, LibraryFineFilter>((
      ref,
      filter,
    ) {
      return _resolve(ref.watch(libraryRepositoryProvider).fines(filter));
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

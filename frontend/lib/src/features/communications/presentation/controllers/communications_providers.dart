import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/communication_models.dart';
import '../../data/repositories/communications_repository_impl.dart';

final communicationsPageProvider =
    FutureProvider.family<CommunicationPage, CommunicationFilter>((
      ref,
      filter,
    ) {
      return _resolve(
        ref.watch(communicationsRepositoryProvider).communications(filter),
      );
    });

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

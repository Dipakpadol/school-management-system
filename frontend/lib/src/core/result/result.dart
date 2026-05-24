import '../error/failure.dart';

sealed class Result<T> {
  const Result();

  R when<R>({
    required R Function(T value) success,
    required R Function(Failure failure) failure,
  });
}

final class Success<T> extends Result<T> {
  const Success(this.value);

  final T value;

  @override
  R when<R>({
    required R Function(T value) success,
    required R Function(Failure failure) failure,
  }) {
    return success(value);
  }
}

final class FailureResult<T> extends Result<T> {
  const FailureResult(this.error);

  final Failure error;

  @override
  R when<R>({
    required R Function(T value) success,
    required R Function(Failure failure) failure,
  }) {
    return failure(error);
  }
}

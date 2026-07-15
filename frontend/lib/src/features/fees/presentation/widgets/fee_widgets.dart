import 'package:flutter/material.dart';

import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';

class FeeStatusChip extends StatelessWidget {
  const FeeStatusChip({required this.status, super.key});

  final String status;

  @override
  Widget build(BuildContext context) {
    final color = switch (status) {
      'ACTIVE' || 'PAID' || 'COMPLETED' || 'ISSUED' || 'ASSIGNED' => const Color(0xFF16A34A),
      'OVERDUE' || 'CANCELLED' || 'REJECTED' => const Color(0xFFDC2626),
      'PARTIALLY_PAID' => const Color(0xFFF59E0B),
      'UNASSIGNED' => const Color(0xFF2563EB),
      _ => const Color(0xFF64748B),
    };

    return Container(
      height: 26,
      padding: const EdgeInsets.symmetric(horizontal: 9),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(13),
      ),
      child: Text(
        status.replaceAll('_', ' '),
        overflow: TextOverflow.ellipsis,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class MoneyText extends StatelessWidget {
  const MoneyText(this.value, {this.emphasized = false, super.key});

  final double value;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    return Text(
      'INR ${value.toStringAsFixed(2)}',
      overflow: TextOverflow.ellipsis,
      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
        fontWeight: emphasized ? FontWeight.w800 : FontWeight.w600,
      ),
    );
  }
}

class FeeAsyncView<T> extends StatelessWidget {
  const FeeAsyncView({
    required this.value,
    required this.builder,
    required this.onRetry,
    super.key,
  });

  final AsyncSnapshotLike<T> value;
  final Widget Function(T data) builder;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    if (value.isLoading) {
      return const AppLoadingState();
    }
    if (value.errorMessage != null) {
      return AppErrorState(message: value.errorMessage!, onRetry: onRetry);
    }
    return builder(value.data as T);
  }
}

class AsyncSnapshotLike<T> {
  const AsyncSnapshotLike({
    required this.isLoading,
    this.data,
    this.errorMessage,
  });

  final bool isLoading;
  final T? data;
  final String? errorMessage;
}

String dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

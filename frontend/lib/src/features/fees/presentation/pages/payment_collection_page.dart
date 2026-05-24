import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_button.dart';
import '../../../../core/widgets/app_error_state.dart';
import '../../../../core/widgets/app_loading_state.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../data/models/fee_models.dart';
import '../../data/repositories/fees_repository_impl.dart';
import '../controllers/fees_providers.dart';
import '../widgets/fee_widgets.dart';

class PaymentCollectionPage extends ConsumerStatefulWidget {
  const PaymentCollectionPage({this.assignmentId, super.key});

  final String? assignmentId;

  @override
  ConsumerState<PaymentCollectionPage> createState() {
    return _PaymentCollectionPageState();
  }
}

class _PaymentCollectionPageState extends ConsumerState<PaymentCollectionPage> {
  static const _paymentModes = [
    'CASH',
    'UPI',
    'BANK_TRANSFER',
    'CHEQUE',
    'ONLINE',
  ];

  final _formKey = GlobalKey<FormState>();
  final _amountController = TextEditingController();
  final _paymentDateController = TextEditingController();
  final _referenceController = TextEditingController();
  final _payerController = TextEditingController();
  final _collectedByController = TextEditingController();
  final _remarksController = TextEditingController();

  String? _assignmentId;
  String _paymentMode = 'CASH';
  bool _assessLateFee = true;
  bool _saving = false;
  FeeReceiptModel? _receipt;

  @override
  void initState() {
    super.initState();
    _assignmentId = widget.assignmentId;
    _paymentDateController.text = _dateLabel(DateTime.now());
  }

  @override
  void dispose() {
    _amountController.dispose();
    _paymentDateController.dispose();
    _referenceController.dispose();
    _payerController.dispose();
    _collectedByController.dispose();
    _remarksController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final assignments = ref.watch(feeAssignmentsProvider);

    return AdminShell(
      title: 'Payment collection',
      activeModuleId: 'fees',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: assignments.when(
        data: _buildForm,
        error: (error, _) => AppErrorState(
          message: _message(error),
          onRetry: () => ref.invalidate(feeAssignmentsProvider),
        ),
        loading: () => const AppLoadingState(label: 'Loading assignments'),
      ),
    );
  }

  Widget _buildForm(List<StudentFeeAssignmentModel> assignments) {
    final assignmentIds = assignments.map((assignment) => assignment.id).toSet();
    final selectedAssignment = assignmentIds.contains(_assignmentId)
        ? _assignmentId
        : null;

    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 980),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Header(
                  saving: _saving,
                  onBack: () => context.go(AppRoutes.fees),
                  onSave: _saving ? null : _submit,
                ),
                const SizedBox(height: 12),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                      children: [
                        DropdownButtonFormField<String>(
                          initialValue: selectedAssignment,
                          isExpanded: true,
                          items: [
                            for (final assignment in assignments)
                              DropdownMenuItem(
                                value: assignment.id,
                                child: Text(
                                  '${assignment.studentName} - ${assignment.feeStructureName}',
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                          ],
                          onChanged: (value) {
                            setState(() {
                              _assignmentId = value;
                              _receipt = null;
                            });
                          },
                          validator: _required,
                          decoration: const InputDecoration(
                            labelText: 'Student assignment',
                            prefixIcon: Icon(Icons.assignment_ind_outlined),
                          ),
                        ),
                        const SizedBox(height: 14),
                        _SelectedAssignmentPanel(
                          assignment: _findAssignment(
                            assignments,
                            selectedAssignment,
                          ),
                        ),
                        const SizedBox(height: 14),
                        _ResponsiveFields(
                          children: [
                            TextFormField(
                              controller: _amountController,
                              keyboardType:
                                  const TextInputType.numberWithOptions(
                                decimal: true,
                              ),
                              inputFormatters: [_MoneyFormatter()],
                              validator: _positiveAmount,
                              decoration: const InputDecoration(
                                labelText: 'Amount',
                                prefixIcon: Icon(Icons.payments_outlined),
                              ),
                            ),
                            TextFormField(
                              controller: _paymentDateController,
                              validator: _date,
                              decoration: const InputDecoration(
                                labelText: 'Payment date',
                                hintText: 'YYYY-MM-DD',
                                prefixIcon: Icon(Icons.event_outlined),
                              ),
                            ),
                            DropdownButtonFormField<String>(
                              initialValue: _paymentMode,
                              isExpanded: true,
                              items: [
                                for (final mode in _paymentModes)
                                  DropdownMenuItem(
                                    value: mode,
                                    child: Text(mode.replaceAll('_', ' ')),
                                  ),
                              ],
                              onChanged: (value) {
                                if (value == null) {
                                  return;
                                }
                                setState(() => _paymentMode = value);
                              },
                              decoration: const InputDecoration(
                                labelText: 'Payment mode',
                                prefixIcon: Icon(Icons.account_balance_wallet),
                              ),
                            ),
                            TextFormField(
                              controller: _referenceController,
                              validator: _reference,
                              decoration: const InputDecoration(
                                labelText: 'Reference number',
                                prefixIcon: Icon(Icons.tag_outlined),
                              ),
                            ),
                            TextFormField(
                              controller: _payerController,
                              validator: _required,
                              decoration: const InputDecoration(
                                labelText: 'Payer name',
                                prefixIcon: Icon(Icons.person_outline),
                              ),
                            ),
                            TextFormField(
                              controller: _collectedByController,
                              decoration: const InputDecoration(
                                labelText: 'Collected by',
                                prefixIcon: Icon(Icons.verified_user_outlined),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: _remarksController,
                          maxLines: 3,
                          decoration: const InputDecoration(
                            labelText: 'Remarks',
                            alignLabelWithHint: true,
                          ),
                        ),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Assess late fee'),
                          value: _assessLateFee,
                          onChanged: (value) {
                            setState(() => _assessLateFee = value);
                          },
                        ),
                      ],
                    ),
                  ),
                ),
                if (_receipt != null) ...[
                  const SizedBox(height: 12),
                  _ReceiptPanel(receipt: _receipt!),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }
    final assignmentId = _assignmentId;
    if (assignmentId == null) {
      return;
    }
    setState(() => _saving = true);
    final result = await ref.read(feesRepositoryProvider).collectPayment(
      assignmentId,
      {
        'amount': double.tryParse(_amountController.text.trim()),
        'paymentDate': _paymentDateController.text.trim(),
        'paymentMode': _paymentMode,
        'referenceNumber': _blankToNull(_referenceController.text),
        'payerName': _payerController.text.trim(),
        'collectedBy': _blankToNull(_collectedByController.text),
        'remarks': _blankToNull(_remarksController.text),
        'assessLateFee': _assessLateFee,
      },
    );
    if (!mounted) {
      return;
    }
    setState(() => _saving = false);
    result.when<void>(
      success: (receipt) {
        ref.invalidate(feeAssignmentsProvider);
        ref.invalidate(feeDefaultersProvider);
        setState(() => _receipt = receipt);
      },
      failure: (failure) => _showSnack(failure.message),
    );
  }

  String? _reference(String? value) {
    if (_paymentMode == 'CASH') {
      return null;
    }
    return _required(value);
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.saving,
    required this.onBack,
    required this.onSave,
  });

  final bool saving;
  final VoidCallback onBack;
  final VoidCallback? onSave;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          runSpacing: 12,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: 'Back',
                  onPressed: onBack,
                  icon: const Icon(Icons.arrow_back),
                ),
                const SizedBox(width: 8),
                Text(
                  'Collect payment',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ],
            ),
            AppButton(
              label: 'Collect',
              icon: Icons.point_of_sale_outlined,
              isLoading: saving,
              onPressed: onSave,
            ),
          ],
        ),
      ),
    );
  }
}

class _SelectedAssignmentPanel extends StatelessWidget {
  const _SelectedAssignmentPanel({required this.assignment});

  final StudentFeeAssignmentModel? assignment;

  @override
  Widget build(BuildContext context) {
    if (assignment == null) {
      return const SizedBox.shrink();
    }
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Wrap(
        spacing: 18,
        runSpacing: 10,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: [
          _Detail(label: 'Admission', value: assignment!.admissionNumber),
          _Detail(label: 'Class', value: _classLabel(assignment!)),
          _Detail(label: 'Paid', value: _money(assignment!.paidAmount)),
          _Detail(label: 'Balance', value: _money(assignment!.balanceAmount)),
          FeeStatusChip(status: assignment!.status),
        ],
      ),
    );
  }
}

class _ReceiptPanel extends StatelessWidget {
  const _ReceiptPanel({required this.receipt});

  final FeeReceiptModel receipt;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Wrap(
          spacing: 18,
          runSpacing: 12,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox.square(
                  dimension: 44,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Icon(
                      Icons.receipt_long_outlined,
                      color: Theme.of(context).colorScheme.onPrimaryContainer,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      receipt.receiptNumber,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    Text(
                      '${receipt.studentName} - ${receipt.admissionNumber}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.outline,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            _Detail(label: 'Amount', value: _money(receipt.totalAmount)),
            _Detail(label: 'Mode', value: receipt.paymentMode),
            FeeStatusChip(status: receipt.status),
          ],
        ),
      ),
    );
  }
}

class _Detail extends StatelessWidget {
  const _Detail({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(minWidth: 110),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: Theme.of(context).colorScheme.outline,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 3),
          Text(
            value,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _ResponsiveFields extends StatelessWidget {
  const _ResponsiveFields({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 840 ? 2 : 1;
        return GridView.count(
          crossAxisCount: columns,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: columns == 1 ? 5.2 : 4.8,
          physics: const NeverScrollableScrollPhysics(),
          shrinkWrap: true,
          children: children,
        );
      },
    );
  }
}

class _MoneyFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final text = newValue.text;
    if (text.isEmpty || RegExp(r'^\d{0,10}(\.\d{0,2})?$').hasMatch(text)) {
      return newValue;
    }
    return oldValue;
  }
}

String _classLabel(StudentFeeAssignmentModel assignment) {
  final section = assignment.sectionName;
  return section == null || section.isEmpty
      ? assignment.className
      : '${assignment.className} $section';
}

StudentFeeAssignmentModel? _findAssignment(
  List<StudentFeeAssignmentModel> assignments,
  String? assignmentId,
) {
  if (assignmentId == null) {
    return null;
  }
  for (final assignment in assignments) {
    if (assignment.id == assignmentId) {
      return assignment;
    }
  }
  return null;
}

String _money(double value) {
  return 'INR ${value.toStringAsFixed(2)}';
}

String? _required(String? value) {
  if (value == null || value.trim().isEmpty) {
    return 'Required';
  }
  return null;
}

String? _positiveAmount(String? value) {
  final amount = double.tryParse(value?.trim() ?? '');
  if (amount == null || amount <= 0) {
    return 'Enter a valid amount';
  }
  return null;
}

String? _date(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'Required';
  }
  final parsed = DateTime.tryParse(text);
  if (parsed == null || text.length != 10) {
    return 'Use YYYY-MM-DD';
  }
  return null;
}

String? _blankToNull(String value) {
  final trimmed = value.trim();
  return trimmed.isEmpty ? null : trimmed;
}

String _dateLabel(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

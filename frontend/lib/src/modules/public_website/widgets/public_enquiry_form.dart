import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../data/public_website_data.dart';
import '../models/public_website_models.dart';
import '../services/public_enquiry_service.dart';
import 'public_site_theme.dart';
import 'public_site_widgets.dart';

class PublicEnquiryForm extends StatefulWidget {
  const PublicEnquiryForm({super.key, required this.enquiryService});

  final PublicEnquiryService enquiryService;

  @override
  State<PublicEnquiryForm> createState() => _PublicEnquiryFormState();
}

class _PublicEnquiryFormState extends State<PublicEnquiryForm> {
  final _formKey = GlobalKey<FormState>();
  final _studentNameController = TextEditingController();
  final _parentNameController = TextEditingController();
  final _mobileController = TextEditingController();
  final _emailController = TextEditingController();
  final _messageController = TextEditingController();
  String? _classInterested;
  bool _submitting = false;
  bool _submitted = false;

  @override
  void dispose() {
    _studentNameController.dispose();
    _parentNameController.dispose();
    _mobileController.dispose();
    _emailController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      color: Colors.white,
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Admission Enquiry',
                style: theme.textTheme.titleLarge?.copyWith(
                  color: PublicSiteColors.ink,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'Share your details and the admission office will follow up.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: PublicSiteColors.muted,
                  height: 1.45,
                ),
              ),
              if (_submitted) ...[
                const SizedBox(height: 16),
                _SuccessBanner(
                  onClose: () => setState(() => _submitted = false),
                ),
              ],
              const SizedBox(height: 18),
              _FormGrid(
                children: [
                  _PublicTextField(
                    controller: _studentNameController,
                    label: 'Student Name',
                    icon: Icons.school_outlined,
                    textInputAction: TextInputAction.next,
                    validator: _required('Student name is required'),
                  ),
                  _PublicTextField(
                    controller: _parentNameController,
                    label: 'Parent Name',
                    icon: Icons.person_outline,
                    textInputAction: TextInputAction.next,
                    validator: _required('Parent name is required'),
                  ),
                  _PublicTextField(
                    controller: _mobileController,
                    label: 'Mobile Number',
                    icon: Icons.call_outlined,
                    keyboardType: TextInputType.phone,
                    textInputAction: TextInputAction.next,
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[0-9+\-\s]')),
                    ],
                    validator: _validateMobile,
                  ),
                  _PublicTextField(
                    controller: _emailController,
                    label: 'Email',
                    icon: Icons.mail_outline,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    validator: _validateEmail,
                  ),
                  DropdownButtonFormField<String>(
                    initialValue: _classInterested,
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Class Interested',
                      prefixIcon: Icon(Icons.school_outlined),
                    ),
                    items: [
                      for (final admissionClass in admissionClasses)
                        DropdownMenuItem(
                          value: admissionClass.name,
                          child: Text(admissionClass.name),
                        ),
                    ],
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return 'Class interested is required';
                      }
                      return null;
                    },
                    onChanged: (value) {
                      setState(() => _classInterested = value);
                    },
                  ),
                ],
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _messageController,
                minLines: 4,
                maxLines: 6,
                textInputAction: TextInputAction.newline,
                decoration: const InputDecoration(
                  labelText: 'Message',
                  alignLabelWithHint: true,
                  prefixIcon: Icon(Icons.notes_outlined),
                ),
              ),
              const SizedBox(height: 18),
              PublicActionButton(
                label: _submitting ? 'Sending...' : 'Submit Enquiry',
                icon: Icons.send_outlined,
                leading: _submitting
                    ? const SizedBox.square(
                        dimension: 20,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : null,
                expand: true,
                onPressed: _submitting ? null : _submitEnquiry,
              ),
            ],
          ),
        ),
      ),
    );
  }

  FormFieldValidator<String> _required(String message) {
    return (value) {
      if (value == null || value.trim().isEmpty) {
        return message;
      }
      return null;
    };
  }

  String? _validateMobile(String? value) {
    final normalized = (value ?? '').replaceAll(RegExp(r'\D'), '');
    if (normalized.isEmpty) {
      return 'Mobile number is required';
    }
    if (normalized.length < 10 || normalized.length > 13) {
      return 'Enter a valid mobile number';
    }
    return null;
  }

  String? _validateEmail(String? value) {
    final text = value?.trim() ?? '';
    if (text.isEmpty) {
      return 'Email is required';
    }
    final valid = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(text);
    if (!valid) {
      return 'Enter a valid email';
    }
    return null;
  }

  Future<void> _submitEnquiry() async {
    if (_submitting) {
      return;
    }

    final valid = _formKey.currentState?.validate() ?? false;
    if (!valid) {
      return;
    }

    setState(() {
      _submitting = true;
      _submitted = false;
    });

    final request = EnquiryRequest(
      studentName: _studentNameController.text.trim(),
      parentName: _parentNameController.text.trim(),
      mobileNumber: _mobileController.text.trim(),
      email: _emailController.text.trim(),
      classInterested: _classInterested ?? '',
      message: _messageController.text.trim(),
    );

    try {
      await widget.enquiryService.submit(request);
    } on PublicEnquiryException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() => _submitting = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
      return;
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() => _submitting = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Unable to submit the enquiry. Please try again.'),
        ),
      );
      return;
    }

    if (!mounted) {
      return;
    }

    _formKey.currentState?.reset();
    _studentNameController.clear();
    _parentNameController.clear();
    _mobileController.clear();
    _emailController.clear();
    _messageController.clear();
    setState(() {
      _classInterested = null;
      _submitting = false;
      _submitted = true;
    });
  }
}

class _FormGrid extends StatelessWidget {
  const _FormGrid({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return PublicResponsiveGrid(
      minItemWidth: 260,
      maxColumns: 2,
      spacing: 14,
      runSpacing: 14,
      children: children,
    );
  }
}

class _PublicTextField extends StatelessWidget {
  const _PublicTextField({
    required this.controller,
    required this.label,
    required this.icon,
    this.keyboardType,
    this.textInputAction,
    this.inputFormatters,
    this.validator,
  });

  final TextEditingController controller;
  final String label;
  final IconData icon;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final List<TextInputFormatter>? inputFormatters;
  final FormFieldValidator<String>? validator;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: keyboardType,
      textInputAction: textInputAction,
      inputFormatters: inputFormatters,
      validator: validator,
      decoration: InputDecoration(labelText: label, prefixIcon: Icon(icon)),
    );
  }
}

class _SuccessBanner extends StatelessWidget {
  const _SuccessBanner({required this.onClose});

  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: PublicSiteColors.green.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: PublicSiteColors.green.withValues(alpha: 0.3),
        ),
      ),
      child: Row(
        children: [
          const Icon(Icons.check_circle_outline, color: PublicSiteColors.green),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'Thank you. Your enquiry has been received successfully.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: PublicSiteColors.green,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          IconButton(
            tooltip: 'Dismiss success message',
            onPressed: onClose,
            icon: const Icon(Icons.close, color: PublicSiteColors.green),
          ),
        ],
      ),
    );
  }
}

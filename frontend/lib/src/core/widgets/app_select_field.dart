import 'package:flutter/material.dart';

class AppSelectField<T> extends StatelessWidget {
  const AppSelectField({
    required this.label,
    required this.value,
    required this.items,
    required this.onChanged,
    this.required = false,
    this.helperText,
    this.validator,
    this.enabled = true,
    this.icon,
    super.key,
  });

  final String label;
  final T? value;
  final List<DropdownMenuItem<T>> items;
  final ValueChanged<T?> onChanged;
  final bool required;
  final String? helperText;
  final FormFieldValidator<T>? validator;
  final bool enabled;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<T>(
      initialValue: value,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: required ? '$label *' : label,
        helperText: helperText,
        prefixIcon: icon == null ? null : Icon(icon),
      ),
      validator: validator,
      items: items,
      onChanged: enabled && items.isNotEmpty ? onChanged : null,
    );
  }
}

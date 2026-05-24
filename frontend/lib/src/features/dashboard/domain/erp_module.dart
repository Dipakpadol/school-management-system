import 'package:flutter/material.dart';

class ErpModule {
  const ErpModule({
    required this.id,
    required this.title,
    required this.icon,
    required this.status,
    required this.description,
  });

  final String id;
  final String title;
  final IconData icon;
  final String status;
  final String description;
}

import 'package:flutter/material.dart';

import 'erp_module.dart';

const erpModules = <ErpModule>[
  ErpModule(
    id: 'students',
    title: 'Student Management',
    icon: Icons.school_outlined,
    status: 'API ready',
    description: 'Admissions, profiles, guardians, documents, classes.',
  ),
  ErpModule(
    id: 'fees',
    title: 'Fees Management',
    icon: Icons.payments_outlined,
    status: 'API ready',
    description: 'Structures, assignments, installments, receipts, reports.',
  ),
  ErpModule(
    id: 'users',
    title: 'User Management',
    icon: Icons.manage_accounts_outlined,
    status: 'API ready',
    description: 'Users, roles, account status, password resets.',
  ),
  ErpModule(
    id: 'audit-logs',
    title: 'Audit Logs',
    icon: Icons.manage_search_outlined,
    status: 'API ready',
    description: 'Track creates, updates, status changes, and payments.',
  ),
  ErpModule(
    id: 'hostel',
    title: 'Hostel Management',
    icon: Icons.apartment_outlined,
    status: 'Next',
    description: 'Hostel rooms, allocations, wardens, occupancy.',
  ),
  ErpModule(
    id: 'attendance',
    title: 'Attendance',
    icon: Icons.fact_check_outlined,
    status: 'Next',
    description: 'Daily attendance, class registers, absence tracking.',
  ),
  ErpModule(
    id: 'reports',
    title: 'Reports',
    icon: Icons.analytics_outlined,
    status: 'Next',
    description: 'Operational, academic, attendance, and finance reports.',
  ),
  ErpModule(
    id: 'settings',
    title: 'Settings',
    icon: Icons.tune_outlined,
    status: 'Next',
    description: 'Institution profile, permissions, academic defaults.',
  ),
];

ErpModule? findModuleById(String id) {
  for (final module in erpModules) {
    if (module.id == id) {
      return module;
    }
  }
  return null;
}

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
    id: 'academic',
    title: 'Academic Management',
    icon: Icons.account_tree_outlined,
    status: 'API ready',
    description: 'Years, classes, sections, subjects, mappings, timetable.',
  ),
  ErpModule(
    id: 'hostel',
    title: 'Hostel Management',
    icon: Icons.apartment_outlined,
    status: 'API ready',
    description: 'Hostel rooms, allocations, wardens, occupancy.',
  ),
  ErpModule(
    id: 'transport',
    title: 'Transport Management',
    icon: Icons.directions_bus_outlined,
    status: 'API ready',
    description: 'Buses, routes, pickup points, drivers, assignments.',
  ),
  ErpModule(
    id: 'teachers',
    title: 'Teacher Management',
    icon: Icons.badge_outlined,
    status: 'API ready',
    description: 'Teacher profiles, documents, classes, subjects.',
  ),
  ErpModule(
    id: 'attendance',
    title: 'Attendance',
    icon: Icons.fact_check_outlined,
    status: 'API ready',
    description: 'Daily attendance, class registers, absence tracking.',
  ),
  ErpModule(
    id: 'exams',
    title: 'Exams & Results',
    icon: Icons.assignment_outlined,
    status: 'API ready',
    description: 'Exam setup, schedules, marks, grades, report cards.',
  ),
  ErpModule(
    id: 'reports',
    title: 'Reports',
    icon: Icons.analytics_outlined,
    status: 'API ready',
    description: 'Operational, academic, attendance, and finance reports.',
  ),
  ErpModule(
    id: 'notifications',
    title: 'Notifications',
    icon: Icons.campaign_outlined,
    status: 'API ready',
    description: 'Templates, send-ready records, and delivery history.',
  ),
  ErpModule(
    id: 'settings',
    title: 'Settings',
    icon: Icons.tune_outlined,
    status: 'API ready',
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

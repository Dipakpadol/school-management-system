import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/network/page_payload.dart';
import '../../../../core/widgets/admin_shell.dart';
import '../../../../core/widgets/app_confirm_dialog.dart';
import '../../../../core/widgets/app_page_layout.dart';
import '../../../../core/widgets/app_select_field.dart';
import '../../../../core/widgets/app_text_field.dart';
import '../../../auth/domain/entities/auth_user.dart';
import '../../../auth/presentation/controllers/auth_controller.dart';
import '../../../dashboard/presentation/controllers/dashboard_controller.dart';
import '../../data/models/library_models.dart';
import '../../data/repositories/library_repository_impl.dart';
import '../controllers/library_providers.dart';

class LibraryManagementPage extends ConsumerWidget {
  const LibraryManagementPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).user;
    final tabs = _tabsFor(user);

    return AdminShell(
      title: 'Library Management',
      activeModuleId: 'library',
      onLogout: () async {
        await ref.read(authControllerProvider.notifier).logout();
        if (context.mounted) {
          context.go(AppRoutes.login);
        }
      },
      child: tabs.isEmpty
          ? const _AccessDenied()
          : DefaultTabController(
              length: tabs.length,
              initialIndex: _initialIndex(context, tabs),
              child: Column(
                children: [
                  const Padding(
                    padding: EdgeInsets.fromLTRB(24, 24, 24, 12),
                    child: AppPageHeader(
                      title: 'Library Management',
                      subtitle:
                          'Manage catalog, copies, memberships, circulation, fines, and library reports.',
                      icon: Icons.local_library_outlined,
                    ),
                  ),
                  Material(
                    color: Colors.white,
                    child: TabBar(
                      isScrollable: true,
                      tabs: [
                        for (final tab in tabs)
                          Tab(icon: Icon(tab.icon), text: tab.label),
                      ],
                    ),
                  ),
                  const Divider(height: 1),
                  Expanded(
                    child: TabBarView(
                      children: [for (final tab in tabs) tab.child],
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}

class _SummaryTab extends ConsumerWidget {
  const _SummaryTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(librarySummaryProvider);
    return RefreshIndicator(
      onRefresh: () async => ref.invalidate(librarySummaryProvider),
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          summary.when(
            data: (item) => _MetricGrid(
              metrics: [
                _MetricData(
                  label: 'Books',
                  value: _intLabel(item.totalBooks),
                  icon: Icons.menu_book_outlined,
                  color: const Color(0xFF2563EB),
                ),
                _MetricData(
                  label: 'Copies',
                  value: _intLabel(item.totalCopies),
                  icon: Icons.inventory_2_outlined,
                  color: const Color(0xFF0F766E),
                ),
                _MetricData(
                  label: 'Available',
                  value: _intLabel(item.availableCopies),
                  icon: Icons.task_alt_outlined,
                  color: const Color(0xFF16A34A),
                ),
                _MetricData(
                  label: 'Issued',
                  value: _intLabel(item.issuedCopies),
                  icon: Icons.assignment_return_outlined,
                  color: const Color(0xFF7C3AED),
                ),
                _MetricData(
                  label: 'Overdue',
                  value: _intLabel(item.overdueLoans),
                  icon: Icons.assignment_late_outlined,
                  color: const Color(0xFFDC2626),
                ),
                _MetricData(
                  label: 'Members',
                  value: _intLabel(item.activeMembers),
                  icon: Icons.card_membership_outlined,
                  color: const Color(0xFF0891B2),
                ),
                _MetricData(
                  label: 'Lost/Damaged',
                  value: _intLabel(item.lostCopies + item.damagedCopies),
                  icon: Icons.warning_amber_outlined,
                  color: const Color(0xFFB45309),
                ),
                _MetricData(
                  label: 'Pending fines',
                  value: _money(item.pendingFineAmount),
                  icon: Icons.receipt_long_outlined,
                  color: const Color(0xFFE11D48),
                ),
              ],
            ),
            error: (error, _) => _ErrorPanel(
              message: _message(error),
              onRetry: () => ref.invalidate(librarySummaryProvider),
            ),
            loading: () =>
                const _LoadingPanel(label: 'Loading library summary'),
          ),
          const SizedBox(height: 18),
          _Surface(
            title: 'Circulation watchlist',
            action: OutlinedButton.icon(
              onPressed: () {
                DefaultTabController.of(context).animateTo(4);
              },
              icon: const Icon(Icons.assignment_late_outlined),
              label: const Text('Open loans'),
            ),
            child: const Text(
              'Use the circulation tab to review active loans, overdue books, returns, and lost-book handling.',
            ),
          ),
        ],
      ),
    );
  }
}

class _CatalogTab extends ConsumerStatefulWidget {
  const _CatalogTab({
    required this.canCreate,
    required this.canUpdate,
    required this.canDelete,
  });

  final bool canCreate;
  final bool canUpdate;
  final bool canDelete;

  @override
  ConsumerState<_CatalogTab> createState() => _CatalogTabState();
}

class _CatalogTabState extends ConsumerState<_CatalogTab> {
  static const _pageSize = 20;

  final _keywordController = TextEditingController();
  LibraryBookFilter _filter = const LibraryBookFilter(size: _pageSize);

  @override
  void dispose() {
    _keywordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(libraryCategoriesProvider);
    final authors = ref.watch(libraryAuthorsProvider);
    final publishers = ref.watch(libraryPublishersProvider);
    final books = ref.watch(libraryBooksProvider(_filter));
    final categoryItems = categories.maybeWhen(
      data: (items) => items,
      orElse: () => const <LibraryCategoryModel>[],
    );
    final authorItems = authors.maybeWhen(
      data: (items) => items,
      orElse: () => const <LibraryAuthorModel>[],
    );
    final publisherItems = publishers.maybeWhen(
      data: (items) => items,
      orElse: () => const <LibraryPublisherModel>[],
    );

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Catalog master data',
          action: widget.canCreate
              ? Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () => _showMasterDialog(
                        context,
                        ref,
                        title: 'Add category',
                        nameLabel: 'Category name',
                        detailLabel: 'Description',
                        onSave: (payload) async {
                          final result = await ref
                              .read(libraryRepositoryProvider)
                              .createCategory(payload);
                          return result.when(
                            success: (_) => null,
                            failure: (failure) => failure.message,
                          );
                        },
                      ),
                      icon: const Icon(Icons.category_outlined),
                      label: const Text('Category'),
                    ),
                    OutlinedButton.icon(
                      onPressed: () => _showMasterDialog(
                        context,
                        ref,
                        title: 'Add author',
                        nameLabel: 'Author name',
                        detailLabel: 'Biography',
                        detailKey: 'biography',
                        onSave: (payload) async {
                          final result = await ref
                              .read(libraryRepositoryProvider)
                              .createAuthor(payload);
                          return result.when(
                            success: (_) => null,
                            failure: (failure) => failure.message,
                          );
                        },
                      ),
                      icon: const Icon(Icons.person_add_alt_outlined),
                      label: const Text('Author'),
                    ),
                    OutlinedButton.icon(
                      onPressed: () => _showMasterDialog(
                        context,
                        ref,
                        title: 'Add publisher',
                        nameLabel: 'Publisher name',
                        detailLabel: 'Contact info',
                        detailKey: 'contactInfo',
                        onSave: (payload) async {
                          final result = await ref
                              .read(libraryRepositoryProvider)
                              .createPublisher(payload);
                          return result.when(
                            success: (_) => null,
                            failure: (failure) => failure.message,
                          );
                        },
                      ),
                      icon: const Icon(Icons.business_outlined),
                      label: const Text('Publisher'),
                    ),
                  ],
                )
              : null,
          child: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              _CountPill(
                icon: Icons.category_outlined,
                label: 'Categories',
                count: categoryItems.length,
              ),
              _CountPill(
                icon: Icons.person_outline,
                label: 'Authors',
                count: authorItems.length,
              ),
              _CountPill(
                icon: Icons.business_outlined,
                label: 'Publishers',
                count: publisherItems.length,
              ),
              IconButton.outlined(
                tooltip: 'Refresh master data',
                onPressed: _refreshMasters,
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _Surface(
          title: 'Books',
          action: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              if (widget.canCreate)
                FilledButton.icon(
                  onPressed: () => _showBookDialog(
                    context,
                    ref,
                    categories: categoryItems,
                    authors: authorItems,
                    publishers: publisherItems,
                  ),
                  icon: const Icon(Icons.add),
                  label: const Text('Add book'),
                ),
              IconButton.outlined(
                tooltip: 'Refresh',
                onPressed: () => ref.invalidate(libraryBooksProvider(_filter)),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _SearchField(
                    controller: _keywordController,
                    label: 'Search books',
                    onSubmitted: (value) => _changeFilter(
                      _filter.copyWith(
                        keyword: value,
                        page: 0,
                        clearKeyword: value.trim().isEmpty,
                      ),
                    ),
                  ),
                  _select(
                    width: 220,
                    label: 'Category',
                    value: _filter.categoryId,
                    items: {
                      for (final item in categoryItems) item.id: item.name,
                    },
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        categoryId: value,
                        page: 0,
                        clearCategory: value == null,
                      ),
                    ),
                  ),
                  _select(
                    width: 220,
                    label: 'Publisher',
                    value: _filter.publisherId,
                    items: {
                      for (final item in publisherItems) item.id: item.name,
                    },
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        publisherId: value,
                        page: 0,
                        clearPublisher: value == null,
                      ),
                    ),
                  ),
                  _select(
                    width: 160,
                    label: 'Status',
                    value: _filter.active?.toString(),
                    items: const {'true': 'Active', 'false': 'Inactive'},
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        active: value == null ? null : value == 'true',
                        page: 0,
                        clearActive: value == null,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              books.when(
                data: (page) => _BookPageView(
                  page: page,
                  canUpdate: widget.canUpdate,
                  canDelete: widget.canDelete,
                  onPageChanged: (page) =>
                      _changeFilter(_filter.copyWith(page: page)),
                ),
                error: (error, _) => _ErrorPanel(
                  message: _message(error),
                  onRetry: () => ref.invalidate(libraryBooksProvider(_filter)),
                ),
                loading: () => const _LoadingPanel(label: 'Loading books'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _changeFilter(LibraryBookFilter filter) {
    setState(() => _filter = filter);
  }

  void _refreshMasters() {
    ref.invalidate(libraryCategoriesProvider);
    ref.invalidate(libraryAuthorsProvider);
    ref.invalidate(libraryPublishersProvider);
  }
}

class _BookPageView extends ConsumerWidget {
  const _BookPageView({
    required this.page,
    required this.canUpdate,
    required this.canDelete,
    required this.onPageChanged,
  });

  final PagePayload<LibraryBookModel> page;
  final bool canUpdate;
  final bool canDelete;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No books found.');
    }
    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final book = page.content[index];
            return _InfoCard(
              icon: Icons.menu_book_outlined,
              title: book.title,
              subtitle:
                  '${_dash(book.categoryName)} | ${_dash(book.publisherName)} | ${book.authors.map((item) => item.name).join(', ')}',
              footer: Wrap(
                spacing: 8,
                runSpacing: 8,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  _StatusChip(status: book.active ? 'ACTIVE' : 'INACTIVE'),
                  if (_hasText(book.isbn)) _MiniText('ISBN ${book.isbn}'),
                  if (_hasText(book.shelfLocation))
                    _MiniText('Shelf ${book.shelfLocation}'),
                ],
              ),
              trailing: canDelete && book.active
                  ? IconButton(
                      tooltip: 'Deactivate book',
                      onPressed: () => _deactivateBook(context, ref, book),
                      icon: const Icon(Icons.block_outlined),
                    )
                  : null,
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _CopiesTab extends ConsumerStatefulWidget {
  const _CopiesTab({required this.canCreate, required this.canUpdate});

  final bool canCreate;
  final bool canUpdate;

  @override
  ConsumerState<_CopiesTab> createState() => _CopiesTabState();
}

class _CopiesTabState extends ConsumerState<_CopiesTab> {
  final _keywordController = TextEditingController();
  LibraryCopyFilter _filter = const LibraryCopyFilter();

  @override
  void dispose() {
    _keywordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final copies = ref.watch(libraryCopiesProvider(_filter));
    final booksState = ref.watch(
      libraryBooksProvider(const LibraryBookFilter(size: 100, active: true)),
    );
    final books = booksState.maybeWhen(
      data: (page) => page.content,
      orElse: () => const <LibraryBookModel>[],
    );

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Book copies',
          action: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              if (widget.canCreate)
                FilledButton.icon(
                  onPressed: () => _showCopyDialog(context, ref, books),
                  icon: const Icon(Icons.add),
                  label: const Text('Add copy'),
                ),
              IconButton.outlined(
                tooltip: 'Refresh',
                onPressed: () => ref.invalidate(libraryCopiesProvider(_filter)),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _SearchField(
                    controller: _keywordController,
                    label: 'Search copies',
                    onSubmitted: (value) => _changeFilter(
                      _filter.copyWith(
                        keyword: value,
                        page: 0,
                        clearKeyword: value.trim().isEmpty,
                      ),
                    ),
                  ),
                  _select(
                    width: 190,
                    label: 'Status',
                    value: _filter.status,
                    items: _statusItems(_copyStatuses),
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        status: value,
                        page: 0,
                        clearStatus: value == null,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              copies.when(
                data: (page) => _CopyPageView(
                  page: page,
                  canUpdate: widget.canUpdate,
                  onPageChanged: (page) =>
                      _changeFilter(_filter.copyWith(page: page)),
                ),
                error: (error, _) => _ErrorPanel(
                  message: _message(error),
                  onRetry: () => ref.invalidate(libraryCopiesProvider(_filter)),
                ),
                loading: () => const _LoadingPanel(label: 'Loading copies'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _changeFilter(LibraryCopyFilter filter) {
    setState(() => _filter = filter);
  }
}

class _CopyPageView extends ConsumerWidget {
  const _CopyPageView({
    required this.page,
    required this.canUpdate,
    required this.onPageChanged,
  });

  final PagePayload<LibraryBookCopyModel> page;
  final bool canUpdate;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No copies found.');
    }
    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final copy = page.content[index];
            return _InfoCard(
              icon: Icons.inventory_2_outlined,
              title: copy.accessionNumber,
              subtitle:
                  '${copy.bookTitle} | ${_dash(copy.shelfLocation)} | ${_money(copy.price ?? 0)}',
              footer: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _StatusChip(status: copy.status),
                  if (copy.acquiredOn != null)
                    _MiniText('Acquired ${libraryDateParam(copy.acquiredOn!)}'),
                ],
              ),
              trailing: canUpdate
                  ? PopupMenuButton<String>(
                      tooltip: 'Copy actions',
                      icon: const Icon(Icons.more_vert),
                      onSelected: (value) =>
                          _markCopy(context, ref, copy, value),
                      itemBuilder: (context) => const [
                        PopupMenuItem(
                          value: 'available',
                          child: Text('Available'),
                        ),
                        PopupMenuItem(value: 'damaged', child: Text('Damaged')),
                        PopupMenuItem(value: 'lost', child: Text('Lost')),
                      ],
                    )
                  : null,
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _MembershipsTab extends ConsumerStatefulWidget {
  const _MembershipsTab({
    required this.canCreate,
    required this.canUpdate,
    required this.canDelete,
  });

  final bool canCreate;
  final bool canUpdate;
  final bool canDelete;

  @override
  ConsumerState<_MembershipsTab> createState() => _MembershipsTabState();
}

class _MembershipsTabState extends ConsumerState<_MembershipsTab> {
  final _keywordController = TextEditingController();
  LibraryMembershipFilter _filter = const LibraryMembershipFilter(active: true);

  @override
  void dispose() {
    _keywordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final memberships = ref.watch(libraryMembershipsProvider(_filter));
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Library memberships',
          action: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              if (widget.canCreate)
                FilledButton.icon(
                  onPressed: () => _showMembershipDialog(context, ref),
                  icon: const Icon(Icons.person_add_alt_outlined),
                  label: const Text('Add member'),
                ),
              IconButton.outlined(
                tooltip: 'Refresh',
                onPressed: () =>
                    ref.invalidate(libraryMembershipsProvider(_filter)),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _SearchField(
                    controller: _keywordController,
                    label: 'Search members',
                    onSubmitted: (value) => _changeFilter(
                      _filter.copyWith(
                        keyword: value,
                        page: 0,
                        clearKeyword: value.trim().isEmpty,
                      ),
                    ),
                  ),
                  _select(
                    width: 180,
                    label: 'Member type',
                    value: _filter.memberType,
                    items: _statusItems(_memberTypes),
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        memberType: value,
                        page: 0,
                        clearMemberType: value == null,
                      ),
                    ),
                  ),
                  _select(
                    width: 160,
                    label: 'Status',
                    value: _filter.active?.toString(),
                    items: const {'true': 'Active', 'false': 'Inactive'},
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        active: value == null ? null : value == 'true',
                        page: 0,
                        clearActive: value == null,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              memberships.when(
                data: (page) => _MembershipPageView(
                  page: page,
                  canDelete: widget.canDelete,
                  onPageChanged: (page) =>
                      _changeFilter(_filter.copyWith(page: page)),
                ),
                error: (error, _) => _ErrorPanel(
                  message: _message(error),
                  onRetry: () =>
                      ref.invalidate(libraryMembershipsProvider(_filter)),
                ),
                loading: () =>
                    const _LoadingPanel(label: 'Loading memberships'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _changeFilter(LibraryMembershipFilter filter) {
    setState(() => _filter = filter);
  }
}

class _MembershipPageView extends ConsumerWidget {
  const _MembershipPageView({
    required this.page,
    required this.canDelete,
    required this.onPageChanged,
  });

  final PagePayload<LibraryMembershipModel> page;
  final bool canDelete;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No memberships found.');
    }
    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final member = page.content[index];
            return _InfoCard(
              icon: Icons.card_membership_outlined,
              title: member.memberName,
              subtitle:
                  '${member.membershipNumber} | ${_display(member.memberType)} | ${_dash(member.memberCode)}',
              footer: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _StatusChip(status: member.active ? 'ACTIVE' : 'INACTIVE'),
                  _MiniText('From ${libraryDateParam(member.startDate)}'),
                  if (member.expiryDate != null)
                    _MiniText(
                      'Expires ${libraryDateParam(member.expiryDate!)}',
                    ),
                ],
              ),
              trailing: canDelete && member.active
                  ? IconButton(
                      tooltip: 'Deactivate membership',
                      onPressed: () =>
                          _deactivateMembership(context, ref, member),
                      icon: const Icon(Icons.block_outlined),
                    )
                  : null,
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _CirculationTab extends ConsumerStatefulWidget {
  const _CirculationTab({required this.canIssue, required this.canReturn});

  final bool canIssue;
  final bool canReturn;

  @override
  ConsumerState<_CirculationTab> createState() => _CirculationTabState();
}

class _CirculationTabState extends ConsumerState<_CirculationTab> {
  LibraryLoanFilter _filter = const LibraryLoanFilter(status: 'ACTIVE');

  @override
  Widget build(BuildContext context) {
    final loans = ref.watch(libraryLoansProvider(_filter));
    final copies = ref.watch(
      libraryCopiesProvider(
        const LibraryCopyFilter(status: 'AVAILABLE', size: 100),
      ),
    );
    final memberships = ref.watch(
      libraryMembershipsProvider(
        const LibraryMembershipFilter(active: true, size: 100),
      ),
    );
    final availableCopies = copies.maybeWhen(
      data: (page) => page.content,
      orElse: () => const <LibraryBookCopyModel>[],
    );
    final activeMemberships = memberships.maybeWhen(
      data: (page) => page.content,
      orElse: () => const <LibraryMembershipModel>[],
    );

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Circulation',
          action: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              if (widget.canIssue)
                FilledButton.icon(
                  onPressed: () => _showIssueDialog(
                    context,
                    ref,
                    copies: availableCopies,
                    memberships: activeMemberships,
                  ),
                  icon: const Icon(Icons.outbox_outlined),
                  label: const Text('Issue book'),
                ),
              IconButton.outlined(
                tooltip: 'Refresh',
                onPressed: () => ref.invalidate(libraryLoansProvider(_filter)),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 12,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  _select(
                    width: 170,
                    label: 'Loan status',
                    value: _filter.status,
                    items: _statusItems(_loanStatuses),
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        status: value,
                        page: 0,
                        clearStatus: value == null,
                      ),
                    ),
                  ),
                  _select(
                    width: 180,
                    label: 'Member type',
                    value: _filter.memberType,
                    items: _statusItems(_memberTypes),
                    onChanged: (value) => _changeFilter(
                      _filter.copyWith(
                        memberType: value,
                        page: 0,
                        clearMemberType: value == null,
                      ),
                    ),
                  ),
                  FilterChip(
                    selected: _filter.overdueOnly,
                    onSelected: (value) => _changeFilter(
                      _filter.copyWith(overdueOnly: value, page: 0),
                    ),
                    label: const Text('Overdue only'),
                    avatar: const Icon(Icons.assignment_late_outlined),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              loans.when(
                data: (page) => _LoanPageView(
                  page: page,
                  canReturn: widget.canReturn,
                  onPageChanged: (page) =>
                      _changeFilter(_filter.copyWith(page: page)),
                ),
                error: (error, _) => _ErrorPanel(
                  message: _message(error),
                  onRetry: () => ref.invalidate(libraryLoansProvider(_filter)),
                ),
                loading: () => const _LoadingPanel(label: 'Loading loans'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _changeFilter(LibraryLoanFilter filter) {
    setState(() => _filter = filter);
  }
}

class _LoanPageView extends ConsumerWidget {
  const _LoanPageView({
    required this.page,
    required this.canReturn,
    required this.onPageChanged,
  });

  final PagePayload<LibraryLoanModel> page;
  final bool canReturn;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No loans found.');
    }
    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final loan = page.content[index];
            return _InfoCard(
              icon: loan.overdue
                  ? Icons.assignment_late_outlined
                  : Icons.assignment_return_outlined,
              title: loan.bookTitle,
              subtitle:
                  '${loan.accessionNumber} | ${loan.memberName} | due ${libraryDateParam(loan.dueDate)}',
              footer: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _StatusChip(status: loan.status),
                  if (loan.overdue) const _MiniText('Overdue'),
                  if (loan.pendingFine > 0)
                    _MiniText('Fine ${_money(loan.pendingFine)}'),
                ],
              ),
              trailing: canReturn && loan.status == 'ACTIVE'
                  ? PopupMenuButton<String>(
                      tooltip: 'Loan actions',
                      icon: const Icon(Icons.more_vert),
                      onSelected: (value) async {
                        if (value == 'return') {
                          await _showReturnDialog(context, ref, loan);
                        } else {
                          await _markLoanLost(context, ref, loan);
                        }
                      },
                      itemBuilder: (context) => const [
                        PopupMenuItem(value: 'return', child: Text('Return')),
                        PopupMenuItem(value: 'lost', child: Text('Mark lost')),
                      ],
                    )
                  : null,
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _FinesTab extends ConsumerStatefulWidget {
  const _FinesTab({required this.canManage});

  final bool canManage;

  @override
  ConsumerState<_FinesTab> createState() => _FinesTabState();
}

class _FinesTabState extends ConsumerState<_FinesTab> {
  LibraryFineFilter _filter = const LibraryFineFilter(status: 'PENDING');

  @override
  Widget build(BuildContext context) {
    final fines = ref.watch(libraryFinesProvider(_filter));
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Library fines',
          action: IconButton.outlined(
            tooltip: 'Refresh',
            onPressed: () => ref.invalidate(libraryFinesProvider(_filter)),
            icon: const Icon(Icons.refresh),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _select(
                width: 170,
                label: 'Fine status',
                value: _filter.status,
                items: _statusItems(_fineStatuses),
                onChanged: (value) => _changeFilter(
                  _filter.copyWith(
                    status: value,
                    page: 0,
                    clearStatus: value == null,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              fines.when(
                data: (page) => _FinePageView(
                  page: page,
                  canManage: widget.canManage,
                  onPageChanged: (page) =>
                      _changeFilter(_filter.copyWith(page: page)),
                ),
                error: (error, _) => _ErrorPanel(
                  message: _message(error),
                  onRetry: () => ref.invalidate(libraryFinesProvider(_filter)),
                ),
                loading: () => const _LoadingPanel(label: 'Loading fines'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _changeFilter(LibraryFineFilter filter) {
    setState(() => _filter = filter);
  }
}

class _FinePageView extends ConsumerWidget {
  const _FinePageView({
    required this.page,
    required this.canManage,
    required this.onPageChanged,
  });

  final PagePayload<LibraryFineModel> page;
  final bool canManage;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (page.content.isEmpty) {
      return const _EmptyBox(message: 'No fines found.');
    }
    return Column(
      children: [
        _ResponsiveGrid(
          count: page.content.length,
          itemBuilder: (context, index) {
            final fine = page.content[index];
            return _InfoCard(
              icon: Icons.receipt_long_outlined,
              title: fine.bookTitle,
              subtitle:
                  '${fine.memberName} | ${fine.accessionNumber} | ${fine.reason}',
              footer: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _StatusChip(status: fine.status),
                  _MiniText('Amount ${_money(fine.amount)}'),
                  _MiniText('Paid ${_money(fine.paidAmount)}'),
                ],
              ),
              trailing: canManage && fine.status == 'PENDING'
                  ? PopupMenuButton<String>(
                      tooltip: 'Fine actions',
                      icon: const Icon(Icons.more_vert),
                      onSelected: (value) async {
                        if (value == 'pay') {
                          await _showPayFineDialog(context, ref, fine);
                        } else {
                          await _waiveFine(context, ref, fine);
                        }
                      },
                      itemBuilder: (context) => const [
                        PopupMenuItem(value: 'pay', child: Text('Pay')),
                        PopupMenuItem(value: 'waive', child: Text('Waive')),
                      ],
                    )
                  : null,
            );
          },
        ),
        const SizedBox(height: 12),
        _PaginationBar(
          page: page.page,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          onPageChanged: onPageChanged,
        ),
      ],
    );
  }
}

class _LibraryReportsTab extends StatelessWidget {
  const _LibraryReportsTab();

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        _Surface(
          title: 'Library reports',
          action: FilledButton.icon(
            onPressed: () => context.go('${AppRoutes.reports}?section=library'),
            icon: const Icon(Icons.analytics_outlined),
            label: const Text('Open reports'),
          ),
          child: Wrap(
            spacing: 10,
            runSpacing: 10,
            children: const [
              _ReportChip(label: 'Inventory'),
              _ReportChip(label: 'Available books'),
              _ReportChip(label: 'Issued books'),
              _ReportChip(label: 'Overdue books'),
              _ReportChip(label: 'Member loan history'),
              _ReportChip(label: 'Fines'),
              _ReportChip(label: 'Lost/Damaged'),
            ],
          ),
        ),
      ],
    );
  }
}

Future<void> _showMasterDialog(
  BuildContext context,
  WidgetRef ref, {
  required String title,
  required String nameLabel,
  required String detailLabel,
  required Future<String?> Function(Map<String, dynamic>) onSave,
  String detailKey = 'description',
}) async {
  final name = TextEditingController();
  final detail = TextEditingController();
  final formKey = GlobalKey<FormState>();
  try {
    final saved = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(title),
        content: Form(
          key: formKey,
          child: Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              _DialogField(controller: name, label: nameLabel),
              _DialogField(
                controller: detail,
                label: detailLabel,
                required: false,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () async {
              if (!(formKey.currentState?.validate() ?? false)) {
                return;
              }
              final error = await onSave({
                'name': name.text.trim(),
                detailKey: _blankToNull(detail.text),
                'active': true,
              });
              if (error != null) {
                if (context.mounted) {
                  _snack(context, error);
                }
                return;
              }
              if (dialogContext.mounted) {
                Navigator.of(dialogContext).pop(true);
              }
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
    if (saved == true && context.mounted) {
      _snack(context, 'Saved.');
      _refreshLibrary(ref);
    }
  } finally {
    name.dispose();
    detail.dispose();
  }
}

Future<void> _showBookDialog(
  BuildContext context,
  WidgetRef ref, {
  required List<LibraryCategoryModel> categories,
  required List<LibraryAuthorModel> authors,
  required List<LibraryPublisherModel> publishers,
}) async {
  final title = TextEditingController();
  final isbn = TextEditingController();
  final edition = TextEditingController();
  final year = TextEditingController();
  final language = TextEditingController(text: 'English');
  final shelf = TextEditingController();
  final description = TextEditingController();
  final formKey = GlobalKey<FormState>();
  String? categoryId;
  String? publisherId;
  final selectedAuthors = <String>{};

  try {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: const Text('Add book'),
            content: SizedBox(
              width: 680,
              child: Form(
                key: formKey,
                child: Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    _DialogField(controller: title, label: 'Title'),
                    _DialogField(
                      controller: isbn,
                      label: 'ISBN',
                      required: false,
                    ),
                    _DialogSelect(
                      label: 'Category',
                      value: categoryId,
                      items: {
                        for (final item in categories) item.id: item.name,
                      },
                      onChanged: (value) =>
                          setDialogState(() => categoryId = value),
                    ),
                    _DialogSelect(
                      label: 'Publisher',
                      value: publisherId,
                      items: {
                        for (final item in publishers) item.id: item.name,
                      },
                      onChanged: (value) =>
                          setDialogState(() => publisherId = value),
                    ),
                    _DialogField(
                      controller: edition,
                      label: 'Edition',
                      required: false,
                    ),
                    _DialogField(
                      controller: year,
                      label: 'Publication year',
                      required: false,
                      validator: _optionalInt,
                    ),
                    _DialogField(
                      controller: language,
                      label: 'Language',
                      required: false,
                    ),
                    _DialogField(
                      controller: shelf,
                      label: 'Shelf location',
                      required: false,
                    ),
                    _DialogField(
                      controller: description,
                      label: 'Description',
                      required: false,
                    ),
                    SizedBox(
                      width: 624,
                      child: Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          for (final author in authors)
                            FilterChip(
                              selected: selectedAuthors.contains(author.id),
                              onSelected: (selected) {
                                setDialogState(() {
                                  if (selected) {
                                    selectedAuthors.add(author.id);
                                  } else {
                                    selectedAuthors.remove(author.id);
                                  }
                                });
                              },
                              label: Text(author.name),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () async {
                  if (!(formKey.currentState?.validate() ?? false)) {
                    return;
                  }
                  final result = await ref
                      .read(libraryRepositoryProvider)
                      .createBook({
                        'title': title.text.trim(),
                        'isbn': _blankToNull(isbn.text),
                        'categoryId': categoryId,
                        'publisherId': publisherId,
                        'authorIds': selectedAuthors.toList(growable: false),
                        'edition': _blankToNull(edition.text),
                        'publicationYear': _intOrNull(year.text),
                        'language': _blankToNull(language.text),
                        'description': _blankToNull(description.text),
                        'shelfLocation': _blankToNull(shelf.text),
                        'active': true,
                      });
                  result.when(
                    success: (_) {
                      if (dialogContext.mounted) {
                        Navigator.of(dialogContext).pop();
                      }
                      _refreshLibrary(ref);
                      _snack(context, 'Book created.');
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                child: const Text('Save'),
              ),
            ],
          );
        },
      ),
    );
  } finally {
    title.dispose();
    isbn.dispose();
    edition.dispose();
    year.dispose();
    language.dispose();
    shelf.dispose();
    description.dispose();
  }
}

Future<void> _showCopyDialog(
  BuildContext context,
  WidgetRef ref,
  List<LibraryBookModel> books,
) async {
  final accession = TextEditingController();
  final shelf = TextEditingController();
  final acquiredOn = TextEditingController(
    text: libraryDateParam(DateTime.now()),
  );
  final price = TextEditingController();
  final condition = TextEditingController();
  final formKey = GlobalKey<FormState>();
  String? bookId = books.isEmpty ? null : books.first.id;
  String status = 'AVAILABLE';

  try {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: const Text('Add copy'),
            content: Form(
              key: formKey,
              child: Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _DialogSelect(
                    label: 'Book',
                    value: bookId,
                    items: {for (final item in books) item.id: item.title},
                    validator: _required,
                    onChanged: (value) => setDialogState(() => bookId = value),
                  ),
                  _DialogField(
                    controller: accession,
                    label: 'Accession number',
                  ),
                  _DialogField(
                    controller: shelf,
                    label: 'Shelf location',
                    required: false,
                  ),
                  _DialogField(
                    controller: acquiredOn,
                    label: 'Acquired on',
                    validator: _optionalDate,
                  ),
                  _DialogField(
                    controller: price,
                    label: 'Price',
                    required: false,
                    validator: _optionalMoney,
                  ),
                  _DialogSelect(
                    label: 'Status',
                    value: status,
                    items: _statusItems(_copyStatuses),
                    onChanged: (value) =>
                        setDialogState(() => status = value ?? 'AVAILABLE'),
                  ),
                  _DialogField(
                    controller: condition,
                    label: 'Condition note',
                    required: false,
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () async {
                  if (!(formKey.currentState?.validate() ?? false) ||
                      bookId == null) {
                    return;
                  }
                  final result = await ref
                      .read(libraryRepositoryProvider)
                      .createCopy({
                        'bookId': bookId,
                        'accessionNumber': accession.text.trim(),
                        'shelfLocation': _blankToNull(shelf.text),
                        'acquiredOn': _blankToNull(acquiredOn.text),
                        'price': _doubleOrNull(price.text),
                        'conditionNote': _blankToNull(condition.text),
                        'status': status,
                      });
                  result.when(
                    success: (_) {
                      if (dialogContext.mounted) {
                        Navigator.of(dialogContext).pop();
                      }
                      _refreshLibrary(ref);
                      _snack(context, 'Copy created.');
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                child: const Text('Save'),
              ),
            ],
          );
        },
      ),
    );
  } finally {
    accession.dispose();
    shelf.dispose();
    acquiredOn.dispose();
    price.dispose();
    condition.dispose();
  }
}

Future<void> _showMembershipDialog(BuildContext context, WidgetRef ref) async {
  final memberId = TextEditingController();
  final membershipNumber = TextEditingController();
  final startDate = TextEditingController(
    text: libraryDateParam(DateTime.now()),
  );
  final expiryDate = TextEditingController();
  final notes = TextEditingController();
  final formKey = GlobalKey<FormState>();
  String memberType = 'STUDENT';

  try {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: const Text('Add membership'),
            content: Form(
              key: formKey,
              child: Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _DialogSelect(
                    label: 'Member type',
                    value: memberType,
                    items: _statusItems(_memberTypes),
                    onChanged: (value) =>
                        setDialogState(() => memberType = value ?? 'STUDENT'),
                  ),
                  _DialogField(controller: memberId, label: '$memberType ID'),
                  _DialogField(
                    controller: membershipNumber,
                    label: 'Membership number',
                  ),
                  _DialogField(
                    controller: startDate,
                    label: 'Start date',
                    validator: _requiredDate,
                  ),
                  _DialogField(
                    controller: expiryDate,
                    label: 'Expiry date',
                    required: false,
                    validator: _optionalDate,
                  ),
                  _DialogField(
                    controller: notes,
                    label: 'Notes',
                    required: false,
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () async {
                  if (!(formKey.currentState?.validate() ?? false)) {
                    return;
                  }
                  final payload = {
                    'memberType': memberType,
                    'studentId': memberType == 'STUDENT'
                        ? memberId.text.trim()
                        : null,
                    'teacherId': memberType == 'TEACHER'
                        ? memberId.text.trim()
                        : null,
                    'staffId': memberType == 'STAFF'
                        ? memberId.text.trim()
                        : null,
                    'membershipNumber': membershipNumber.text.trim(),
                    'startDate': startDate.text.trim(),
                    'expiryDate': _blankToNull(expiryDate.text),
                    'active': true,
                    'notes': _blankToNull(notes.text),
                  };
                  final result = await ref
                      .read(libraryRepositoryProvider)
                      .createMembership(payload);
                  result.when(
                    success: (_) {
                      if (dialogContext.mounted) {
                        Navigator.of(dialogContext).pop();
                      }
                      _refreshLibrary(ref);
                      _snack(context, 'Membership created.');
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                child: const Text('Save'),
              ),
            ],
          );
        },
      ),
    );
  } finally {
    memberId.dispose();
    membershipNumber.dispose();
    startDate.dispose();
    expiryDate.dispose();
    notes.dispose();
  }
}

Future<void> _showIssueDialog(
  BuildContext context,
  WidgetRef ref, {
  required List<LibraryBookCopyModel> copies,
  required List<LibraryMembershipModel> memberships,
}) async {
  final issueDate = TextEditingController(
    text: libraryDateParam(DateTime.now()),
  );
  final dueDate = TextEditingController();
  final remarks = TextEditingController();
  final formKey = GlobalKey<FormState>();
  String? copyId = copies.isEmpty ? null : copies.first.id;
  String? membershipId = memberships.isEmpty ? null : memberships.first.id;

  try {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: const Text('Issue book'),
            content: SizedBox(
              width: 640,
              child: Form(
                key: formKey,
                child: Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    _DialogSelect(
                      label: 'Copy',
                      value: copyId,
                      items: {
                        for (final item in copies)
                          item.id:
                              '${item.accessionNumber} - ${item.bookTitle}',
                      },
                      validator: _required,
                      onChanged: (value) =>
                          setDialogState(() => copyId = value),
                    ),
                    _DialogSelect(
                      label: 'Membership',
                      value: membershipId,
                      items: {
                        for (final item in memberships)
                          item.id:
                              '${item.membershipNumber} - ${item.memberName}',
                      },
                      validator: _required,
                      onChanged: (value) =>
                          setDialogState(() => membershipId = value),
                    ),
                    _DialogField(
                      controller: issueDate,
                      label: 'Issue date',
                      validator: _requiredDate,
                    ),
                    _DialogField(
                      controller: dueDate,
                      label: 'Due date',
                      required: false,
                      validator: _optionalDate,
                    ),
                    _DialogField(
                      controller: remarks,
                      label: 'Remarks',
                      required: false,
                    ),
                  ],
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () async {
                  if (!(formKey.currentState?.validate() ?? false) ||
                      copyId == null ||
                      membershipId == null) {
                    return;
                  }
                  final result = await ref
                      .read(libraryRepositoryProvider)
                      .issue({
                        'copyId': copyId,
                        'membershipId': membershipId,
                        'issueDate': issueDate.text.trim(),
                        'dueDate': _blankToNull(dueDate.text),
                        'remarks': _blankToNull(remarks.text),
                      });
                  result.when(
                    success: (_) {
                      if (dialogContext.mounted) {
                        Navigator.of(dialogContext).pop();
                      }
                      _refreshLibrary(ref);
                      _snack(context, 'Book issued.');
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                child: const Text('Issue'),
              ),
            ],
          );
        },
      ),
    );
  } finally {
    issueDate.dispose();
    dueDate.dispose();
    remarks.dispose();
  }
}

Future<void> _showReturnDialog(
  BuildContext context,
  WidgetRef ref,
  LibraryLoanModel loan,
) async {
  final returnDate = TextEditingController(
    text: libraryDateParam(DateTime.now()),
  );
  final remarks = TextEditingController();
  final formKey = GlobalKey<FormState>();
  var markDamaged = false;

  try {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          return AlertDialog(
            title: const Text('Return book'),
            content: Form(
              key: formKey,
              child: Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _DialogField(
                    controller: returnDate,
                    label: 'Return date',
                    validator: _requiredDate,
                  ),
                  _DialogField(
                    controller: remarks,
                    label: 'Remarks',
                    required: false,
                  ),
                  SizedBox(
                    width: 300,
                    child: CheckboxListTile(
                      value: markDamaged,
                      onChanged: (value) =>
                          setDialogState(() => markDamaged = value ?? false),
                      title: const Text('Mark copy damaged'),
                      controlAffinity: ListTileControlAffinity.leading,
                    ),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () async {
                  if (!(formKey.currentState?.validate() ?? false)) {
                    return;
                  }
                  final result = await ref
                      .read(libraryRepositoryProvider)
                      .returnLoan(loan.id, {
                        'returnDate': returnDate.text.trim(),
                        'markDamaged': markDamaged,
                        'remarks': _blankToNull(remarks.text),
                      });
                  result.when(
                    success: (_) {
                      if (dialogContext.mounted) {
                        Navigator.of(dialogContext).pop();
                      }
                      _refreshLibrary(ref);
                      _snack(context, 'Book returned.');
                    },
                    failure: (failure) => _snack(context, failure.message),
                  );
                },
                child: const Text('Return'),
              ),
            ],
          );
        },
      ),
    );
  } finally {
    returnDate.dispose();
    remarks.dispose();
  }
}

Future<void> _showPayFineDialog(
  BuildContext context,
  WidgetRef ref,
  LibraryFineModel fine,
) async {
  final amount = TextEditingController(
    text: (fine.amount - fine.paidAmount)
        .clamp(0, fine.amount)
        .toStringAsFixed(2),
  );
  final note = TextEditingController();
  final formKey = GlobalKey<FormState>();
  try {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Pay fine'),
        content: Form(
          key: formKey,
          child: Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              _DialogField(
                controller: amount,
                label: 'Amount',
                validator: _requiredMoney,
              ),
              _DialogField(controller: note, label: 'Note', required: false),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () async {
              if (!(formKey.currentState?.validate() ?? false)) {
                return;
              }
              final result = await ref
                  .read(libraryRepositoryProvider)
                  .payFine(fine.id, {
                    'amount': _doubleOrNull(amount.text),
                    'note': _blankToNull(note.text),
                  });
              result.when(
                success: (_) {
                  if (dialogContext.mounted) {
                    Navigator.of(dialogContext).pop();
                  }
                  _refreshLibrary(ref);
                  _snack(context, 'Fine updated.');
                },
                failure: (failure) => _snack(context, failure.message),
              );
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  } finally {
    amount.dispose();
    note.dispose();
  }
}

Future<void> _deactivateBook(
  BuildContext context,
  WidgetRef ref,
  LibraryBookModel book,
) async {
  if (!await _confirm(context, 'Deactivate ${book.title}?')) {
    return;
  }
  final result = await ref
      .read(libraryRepositoryProvider)
      .deactivateBook(book.id);
  result.when(
    success: (_) {
      _refreshLibrary(ref);
      _snack(context, 'Book deactivated.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _markCopy(
  BuildContext context,
  WidgetRef ref,
  LibraryBookCopyModel copy,
  String action,
) async {
  final confirmed = await _confirm(
    context,
    'Mark ${copy.accessionNumber} as ${_display(action)}?',
  );
  if (!confirmed) {
    return;
  }
  final result = await ref
      .read(libraryRepositoryProvider)
      .markCopy(copy.id, action, note: 'Updated from admin panel');
  result.when(
    success: (_) {
      _refreshLibrary(ref);
      _snack(context, 'Copy updated.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _deactivateMembership(
  BuildContext context,
  WidgetRef ref,
  LibraryMembershipModel member,
) async {
  if (!await _confirm(context, 'Deactivate ${member.membershipNumber}?')) {
    return;
  }
  final result = await ref
      .read(libraryRepositoryProvider)
      .deactivateMembership(member.id);
  result.when(
    success: (_) {
      _refreshLibrary(ref);
      _snack(context, 'Membership deactivated.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _markLoanLost(
  BuildContext context,
  WidgetRef ref,
  LibraryLoanModel loan,
) async {
  if (!await _confirm(context, 'Mark ${loan.accessionNumber} as lost?')) {
    return;
  }
  final result = await ref
      .read(libraryRepositoryProvider)
      .markLoanLost(loan.id, note: 'Marked lost from admin panel');
  result.when(
    success: (_) {
      _refreshLibrary(ref);
      _snack(context, 'Loan marked lost.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

Future<void> _waiveFine(
  BuildContext context,
  WidgetRef ref,
  LibraryFineModel fine,
) async {
  if (!await _confirm(context, 'Waive this fine?')) {
    return;
  }
  final result = await ref.read(libraryRepositoryProvider).waiveFine(fine.id);
  result.when(
    success: (_) {
      _refreshLibrary(ref);
      _snack(context, 'Fine waived.');
    },
    failure: (failure) => _snack(context, failure.message),
  );
}

class _Surface extends StatelessWidget {
  const _Surface({required this.title, required this.child, this.action});

  final String title;
  final Widget child;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                ?action,
              ],
            ),
            const SizedBox(height: 16),
            child,
          ],
        ),
      ),
    );
  }
}

class _MetricGrid extends StatelessWidget {
  const _MetricGrid({required this.metrics});

  final List<_MetricData> metrics;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1180
            ? 4
            : constraints.maxWidth >= 760
            ? 2
            : 1;
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 118,
          ),
          itemCount: metrics.length,
          itemBuilder: (context, index) {
            final metric = metrics[index];
            return Material(
              color: Colors.white,
              borderRadius: BorderRadius.circular(8),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    SizedBox.square(
                      dimension: 46,
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: metric.color.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Icon(metric.icon, color: metric.color),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            metric.label,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context).textTheme.bodySmall
                                ?.copyWith(color: const Color(0xFF64748B)),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            metric.value,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context).textTheme.titleLarge
                                ?.copyWith(fontWeight: FontWeight.w800),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }
}

class _MetricData {
  const _MetricData({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.footer,
    this.trailing,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget footer;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox.square(
              dimension: 44,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: theme.colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(icon, color: theme.colorScheme.onPrimaryContainer),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    subtitle,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.outline,
                    ),
                  ),
                  const SizedBox(height: 10),
                  footer,
                ],
              ),
            ),
            if (trailing != null) ...[const SizedBox(width: 8), trailing!],
          ],
        ),
      ),
    );
  }
}

class _ResponsiveGrid extends StatelessWidget {
  const _ResponsiveGrid({required this.count, required this.itemBuilder});

  final int count;
  final IndexedWidgetBuilder itemBuilder;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 1120 ? 2 : 1;
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            mainAxisExtent: 150,
          ),
          itemCount: count,
          itemBuilder: itemBuilder,
        );
      },
    );
  }
}

class _PaginationBar extends StatelessWidget {
  const _PaginationBar({
    required this.page,
    required this.totalPages,
    required this.totalElements,
    required this.onPageChanged,
  });

  final int page;
  final int totalPages;
  final int totalElements;
  final ValueChanged<int> onPageChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Text('$totalElements records'),
        const Spacer(),
        IconButton(
          tooltip: 'Previous page',
          onPressed: page == 0 ? null : () => onPageChanged(page - 1),
          icon: const Icon(Icons.chevron_left),
        ),
        Text('Page ${totalPages == 0 ? 0 : page + 1} of $totalPages'),
        IconButton(
          tooltip: 'Next page',
          onPressed: totalPages == 0 || page >= totalPages - 1
              ? null
              : () => onPageChanged(page + 1),
          icon: const Icon(Icons.chevron_right),
        ),
      ],
    );
  }
}

class _SearchField extends StatelessWidget {
  const _SearchField({
    required this.controller,
    required this.label,
    required this.onSubmitted,
  });

  final TextEditingController controller;
  final String label;
  final ValueChanged<String> onSubmitted;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 260,
      child: TextField(
        controller: controller,
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: const Icon(Icons.search_outlined),
          suffixIcon: IconButton(
            tooltip: 'Apply search',
            onPressed: () => onSubmitted(controller.text),
            icon: const Icon(Icons.arrow_forward),
          ),
        ),
        onSubmitted: onSubmitted,
      ),
    );
  }
}

class _DialogField extends StatelessWidget {
  const _DialogField({
    required this.controller,
    required this.label,
    this.required = true,
    this.validator,
  });

  final TextEditingController controller;
  final String label;
  final bool required;
  final String? Function(String?)? validator;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 300,
      child: AppTextField(
        controller: controller,
        label: label,
        required: required,
        validator: validator ?? (required ? _required : null),
      ),
    );
  }
}

class _DialogSelect extends StatelessWidget {
  const _DialogSelect({
    required this.label,
    required this.value,
    required this.items,
    required this.onChanged,
    this.validator,
  });

  final String label;
  final String? value;
  final Map<String, String> items;
  final ValueChanged<String?>? onChanged;
  final String? Function(String?)? validator;

  @override
  Widget build(BuildContext context) {
    final validValue = items.containsKey(value) ? value : null;
    return SizedBox(
      width: 300,
      child: AppSelectField<String>(
        label: label,
        value: validValue,
        items: [
          for (final entry in items.entries)
            DropdownMenuItem(value: entry.key, child: Text(entry.value)),
        ],
        enabled: onChanged != null && items.isNotEmpty,
        validator: validator,
        onChanged: (value) => onChanged?.call(value),
      ),
    );
  }
}

class _CountPill extends StatelessWidget {
  const _CountPill({
    required this.icon,
    required this.label,
    required this.count,
  });

  final IconData icon;
  final String label;
  final int count;

  @override
  Widget build(BuildContext context) {
    return Chip(avatar: Icon(icon, size: 18), label: Text('$label: $count'));
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final color = switch (status.toUpperCase()) {
      'ACTIVE' ||
      'AVAILABLE' ||
      'RETURNED' ||
      'PAID' => const Color(0xFF16A34A),
      'ISSUED' || 'PENDING' => const Color(0xFFF59E0B),
      'LOST' || 'DAMAGED' || 'OVERDUE' => const Color(0xFFDC2626),
      'INACTIVE' || 'WITHDRAWN' || 'WAIVED' => const Color(0xFF64748B),
      _ => const Color(0xFF0891B2),
    };
    return AppStatusBadge(label: _display(status), color: color);
  }
}

class _MiniText extends StatelessWidget {
  const _MiniText(this.value);

  final String value;

  @override
  Widget build(BuildContext context) {
    return Text(
      value,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: Theme.of(context).textTheme.labelSmall?.copyWith(
        color: const Color(0xFF64748B),
        fontWeight: FontWeight.w700,
      ),
    );
  }
}

class _ReportChip extends StatelessWidget {
  const _ReportChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: const Icon(Icons.description_outlined, size: 18),
      label: Text(label),
    );
  }
}

class _LoadingPanel extends StatelessWidget {
  const _LoadingPanel({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 120,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 12),
            Text(label),
          ],
        ),
      ),
    );
  }
}

class _ErrorPanel extends StatelessWidget {
  const _ErrorPanel({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFE2E8F0)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Expanded(child: Text(message)),
          OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}

class _EmptyBox extends StatelessWidget {
  const _EmptyBox({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return AppEmptyState(message: message, icon: Icons.local_library_outlined);
  }
}

class _AccessDenied extends StatelessWidget {
  const _AccessDenied();

  @override
  Widget build(BuildContext context) {
    return const Center(child: Text('You do not have permission.'));
  }
}

class _LibraryTabSpec {
  const _LibraryTabSpec({
    required this.id,
    required this.label,
    required this.icon,
    required this.child,
  });

  final String id;
  final String label;
  final IconData icon;
  final Widget child;
}

List<_LibraryTabSpec> _tabsFor(AuthUser? user) {
  return [
    if (_hasAny(user, const ['LIBRARY_READ']))
      const _LibraryTabSpec(
        id: 'summary',
        label: 'Summary',
        icon: Icons.dashboard_outlined,
        child: _SummaryTab(),
      ),
    if (_hasAny(user, const ['LIBRARY_READ']))
      _LibraryTabSpec(
        id: 'catalog',
        label: 'Catalog',
        icon: Icons.menu_book_outlined,
        child: _CatalogTab(
          canCreate: _hasAny(user, const ['LIBRARY_CREATE']),
          canUpdate: _hasAny(user, const ['LIBRARY_UPDATE']),
          canDelete: _hasAny(user, const ['LIBRARY_DELETE']),
        ),
      ),
    if (_hasAny(user, const ['LIBRARY_READ']))
      _LibraryTabSpec(
        id: 'copies',
        label: 'Copies',
        icon: Icons.inventory_2_outlined,
        child: _CopiesTab(
          canCreate: _hasAny(user, const ['LIBRARY_CREATE']),
          canUpdate: _hasAny(user, const ['LIBRARY_UPDATE']),
        ),
      ),
    if (_hasAny(user, const ['LIBRARY_READ']))
      _LibraryTabSpec(
        id: 'members',
        label: 'Members',
        icon: Icons.card_membership_outlined,
        child: _MembershipsTab(
          canCreate: _hasAny(user, const ['LIBRARY_CREATE']),
          canUpdate: _hasAny(user, const ['LIBRARY_UPDATE']),
          canDelete: _hasAny(user, const ['LIBRARY_DELETE']),
        ),
      ),
    if (_hasAny(user, const ['LIBRARY_READ']))
      _LibraryTabSpec(
        id: 'circulation',
        label: 'Circulation',
        icon: Icons.assignment_return_outlined,
        child: _CirculationTab(
          canIssue: _hasAny(user, const ['LIBRARY_ISSUE']),
          canReturn: _hasAny(user, const ['LIBRARY_RETURN']),
        ),
      ),
    if (_hasAny(user, const ['LIBRARY_READ']))
      _LibraryTabSpec(
        id: 'fines',
        label: 'Fines',
        icon: Icons.receipt_long_outlined,
        child: _FinesTab(canManage: _hasAny(user, const ['LIBRARY_FINE'])),
      ),
    if (_hasAny(user, const ['LIBRARY_READ']))
      const _LibraryTabSpec(
        id: 'reports',
        label: 'Reports',
        icon: Icons.analytics_outlined,
        child: _LibraryReportsTab(),
      ),
  ];
}

int _initialIndex(BuildContext context, List<_LibraryTabSpec> tabs) {
  final section = GoRouterState.of(context).uri.queryParameters['section'];
  final index = tabs.indexWhere((tab) => tab.id == section);
  return index < 0 ? 0 : index;
}

Widget _select({
  required double width,
  required String label,
  required String? value,
  required Map<String, String> items,
  required ValueChanged<String?> onChanged,
}) {
  final validValue = items.containsKey(value) ? value : null;
  return SizedBox(
    width: width,
    child: DropdownButtonFormField<String>(
      initialValue: validValue,
      isExpanded: true,
      decoration: InputDecoration(labelText: label),
      items: [
        const DropdownMenuItem(value: '', child: Text('All')),
        for (final entry in items.entries)
          DropdownMenuItem(value: entry.key, child: Text(entry.value)),
      ],
      onChanged: (value) =>
          onChanged(value == null || value.isEmpty ? null : value),
    ),
  );
}

Map<String, String> _statusItems(List<String> values) {
  return {for (final value in values) value: _display(value)};
}

bool _hasAny(AuthUser? user, Iterable<String> permissions) {
  if (user == null) {
    return false;
  }
  if (user.hasRole('SUPER_ADMIN') || user.hasRole('ADMIN')) {
    return true;
  }
  if (user.permissions.isNotEmpty) {
    return user.hasAnyPermission(permissions);
  }
  final roles = user.roles.map((role) => role.toUpperCase()).toSet();
  if (permissions.contains('LIBRARY_READ')) {
    return roles.intersection({
      'PRINCIPAL',
      'TEACHER',
      'STUDENT',
      'PARENT',
      'WARDEN',
      'RECEPTIONIST',
    }).isNotEmpty;
  }
  if (permissions.contains('LIBRARY_CREATE') ||
      permissions.contains('LIBRARY_UPDATE') ||
      permissions.contains('LIBRARY_DELETE') ||
      permissions.contains('LIBRARY_ISSUE') ||
      permissions.contains('LIBRARY_RETURN') ||
      permissions.contains('LIBRARY_FINE')) {
    return roles.contains('PRINCIPAL') || roles.contains('RECEPTIONIST');
  }
  return false;
}

void _refreshLibrary(WidgetRef ref) {
  ref.invalidate(librarySummaryProvider);
  ref.invalidate(libraryCategoriesProvider);
  ref.invalidate(libraryAuthorsProvider);
  ref.invalidate(libraryPublishersProvider);
  ref.invalidate(libraryBooksProvider);
  ref.invalidate(libraryCopiesProvider);
  ref.invalidate(libraryMembershipsProvider);
  ref.invalidate(libraryLoansProvider);
  ref.invalidate(libraryFinesProvider);
  ref.invalidate(dashboardOverviewProvider);
}

Future<bool> _confirm(BuildContext context, String message) async {
  return showAppConfirmDialog(
    context: context,
    title: 'Delete library record?',
    message:
        '$message This updates the library catalog or circulation setup. Existing circulation history remains governed by backend rules.',
    confirmLabel: 'Delete',
    confirmIcon: Icons.delete_outline,
    destructive: true,
  );
}

String? _required(String? value) {
  return value == null || value.trim().isEmpty ? 'Required' : null;
}

String? _requiredDate(String? value) {
  return _required(value) ?? _optionalDate(value);
}

String? _optionalDate(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return DateTime.tryParse(text) == null || text.length != 10
      ? 'Use YYYY-MM-DD'
      : null;
}

String? _optionalMoney(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  final parsed = double.tryParse(text);
  return parsed == null || parsed < 0 ? 'Enter a valid amount' : null;
}

String? _requiredMoney(String? value) {
  return _required(value) ?? _optionalMoney(value);
}

String? _optionalInt(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return null;
  }
  return int.tryParse(text) == null ? 'Enter a whole number' : null;
}

String? _blankToNull(String? value) {
  if (value == null || value.trim().isEmpty) {
    return null;
  }
  return value.trim();
}

int? _intOrNull(String value) {
  final text = value.trim();
  return text.isEmpty ? null : int.tryParse(text);
}

double? _doubleOrNull(String value) {
  final text = value.trim();
  return text.isEmpty ? null : double.tryParse(text);
}

bool _hasText(String? value) {
  return value != null && value.trim().isNotEmpty;
}

String _dash(String? value) {
  return value == null || value.trim().isEmpty ? '-' : value.trim();
}

String _display(String value) {
  return value
      .replaceAll('_', ' ')
      .toLowerCase()
      .split(' ')
      .where((part) => part.isNotEmpty)
      .map((part) => '${part[0].toUpperCase()}${part.substring(1)}')
      .join(' ');
}

String _intLabel(int value) {
  final raw = value.toString();
  final buffer = StringBuffer();
  for (var index = 0; index < raw.length; index++) {
    final remaining = raw.length - index;
    buffer.write(raw[index]);
    if (remaining > 1 && remaining % 3 == 1) {
      buffer.write(',');
    }
  }
  return buffer.toString();
}

String _money(double value) {
  return 'Rs ${value.toStringAsFixed(2)}';
}

String _message(Object error) {
  return error.toString().replaceFirst('Exception: ', '');
}

void _snack(BuildContext context, String message) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
}

const _copyStatuses = ['AVAILABLE', 'ISSUED', 'LOST', 'DAMAGED', 'WITHDRAWN'];
const _loanStatuses = ['ACTIVE', 'RETURNED', 'LOST'];
const _fineStatuses = ['PENDING', 'PAID', 'WAIVED'];
const _memberTypes = ['STUDENT', 'TEACHER', 'STAFF'];

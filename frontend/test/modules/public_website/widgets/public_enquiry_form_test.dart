import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:school_erp/src/modules/public_website/models/public_website_models.dart';
import 'package:school_erp/src/modules/public_website/services/public_enquiry_service.dart';
import 'package:school_erp/src/modules/public_website/widgets/public_enquiry_form.dart';

void main() {
  testWidgets(
    'submit button is disabled while submitting and duplicate clicks are ignored',
    (tester) async {
      final completer = Completer<void>();
      final service = _FakePublicEnquiryService(
        onSubmit: (_) => completer.future,
      );

      await tester.pumpWidget(_buildForm(service));
      await _fillValidForm(tester);
      await _tapSubmit(tester);

      expect(service.requests, hasLength(1));
      expect(find.text('Sending...'), findsOneWidget);
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNull,
      );

      await tester.tap(find.text('Sending...'));
      await tester.pump();

      expect(service.requests, hasLength(1));

      completer.complete();
      await tester.pumpAndSettle();

      expect(find.text(_successMessage), findsOneWidget);
    },
  );

  testWidgets('form is cleared after success', (tester) async {
    final service = _FakePublicEnquiryService();

    await tester.pumpWidget(_buildForm(service));
    await _fillValidForm(tester);
    await _tapSubmit(tester);
    await tester.pumpAndSettle();

    expect(service.requests, hasLength(1));
    expect(service.requests.single.classInterested, 'Pre-primary');
    expect(find.text(_successMessage), findsOneWidget);
    expect(_fieldTexts(tester), everyElement(isEmpty));
  });

  testWidgets('form values are retained after failure', (tester) async {
    final service = _FakePublicEnquiryService(
      onSubmit: (_) async {
        throw const PublicEnquiryException(
          'Unable to submit the enquiry. Please try again.',
        );
      },
    );

    await tester.pumpWidget(_buildForm(service));
    await _fillValidForm(tester);
    await _tapSubmit(tester);
    await tester.pumpAndSettle();

    expect(service.requests, hasLength(1));
    expect(find.text(_successMessage), findsNothing);
    expect(
      find.text('Unable to submit the enquiry. Please try again.'),
      findsOneWidget,
    );
    expect(_fieldTexts(tester), [
      'Asha Student',
      'Riya Parent',
      '+91 98765 43210',
      'parent@example.com',
      'Please call after 4 PM.',
    ]);
    expect(find.text('Pre-primary'), findsOneWidget);
    expect(find.text('Submit Enquiry'), findsOneWidget);
  });
}

const _successMessage =
    'Thank you. Your enquiry has been received successfully.';

Widget _buildForm(PublicEnquiryService service) {
  return MaterialApp(
    home: Scaffold(
      body: SingleChildScrollView(
        child: PublicEnquiryForm(enquiryService: service),
      ),
    ),
  );
}

Future<void> _fillValidForm(WidgetTester tester) async {
  await tester.enterText(find.byType(TextFormField).at(0), 'Asha Student');
  await tester.enterText(find.byType(TextFormField).at(1), 'Riya Parent');
  await tester.enterText(find.byType(TextFormField).at(2), '+91 98765 43210');
  await tester.enterText(
    find.byType(TextFormField).at(3),
    'parent@example.com',
  );
  await tester.enterText(
    find.byType(TextFormField).at(4),
    'Please call after 4 PM.',
  );

  await tester.tap(find.byType(DropdownButtonFormField<String>));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Pre-primary').last);
  await tester.pumpAndSettle();
}

Future<void> _tapSubmit(WidgetTester tester) async {
  await tester.ensureVisible(find.text('Submit Enquiry'));
  await tester.tap(find.text('Submit Enquiry'));
  await tester.pump();
}

List<String> _fieldTexts(WidgetTester tester) {
  return tester
      .widgetList<TextFormField>(find.byType(TextFormField))
      .map((field) => field.controller?.text ?? '')
      .toList();
}

class _FakePublicEnquiryService implements PublicEnquiryService {
  _FakePublicEnquiryService({this.onSubmit});

  final Future<void> Function(EnquiryRequest request)? onSubmit;
  final List<EnquiryRequest> requests = [];

  @override
  Future<void> submit(EnquiryRequest request) {
    requests.add(request);
    return onSubmit?.call(request) ?? Future<void>.value();
  }
}

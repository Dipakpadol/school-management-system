import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/public_website_models.dart';

final publicEnquiryServiceProvider = Provider<PublicEnquiryService>((ref) {
  return MockPublicEnquiryService();
});

abstract class PublicEnquiryService {
  Future<void> submit(EnquiryRequest request);
}

class MockPublicEnquiryService implements PublicEnquiryService {
  Map<String, String>? latestPayload;

  @override
  Future<void> submit(EnquiryRequest request) async {
    // Replace this delay with POST /api/v1/public/enquiries when the backend
    // endpoint is introduced.
    await Future<void>.delayed(const Duration(milliseconds: 600));
    latestPayload = request.toJson();
  }
}

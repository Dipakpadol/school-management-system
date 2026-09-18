import '../../../../core/network/page_payload.dart';
import '../../../../core/result/result.dart';
import '../../data/models/communication_models.dart';

abstract interface class CommunicationsRepository {
  Future<Result<PagePayload<CommunicationModel>>> communications(
    CommunicationFilter filter,
  );

  Future<Result<CommunicationModel>> create(Map<String, dynamic> payload);

  Future<Result<CommunicationModel>> update(
    String communicationId,
    Map<String, dynamic> payload,
  );

  Future<Result<CommunicationModel>> publish(String communicationId);

  Future<Result<CommunicationModel>> unpublish(String communicationId);

  Future<Result<CommunicationModel>> archive(String communicationId);
}

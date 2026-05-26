import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;

Future<void> downloadBytes(
  List<int> bytes,
  String filename,
  String contentType,
) async {
  final data = Uint8List.fromList(bytes).toJS;
  final blob = web.Blob([data].toJS, web.BlobPropertyBag(type: contentType));
  final url = web.URL.createObjectURL(blob);
  final anchor = web.HTMLAnchorElement()
    ..href = url
    ..download = filename
    ..style.display = 'none';
  web.document.body?.appendChild(anchor);
  anchor.click();
  anchor.remove();
  web.URL.revokeObjectURL(url);
}

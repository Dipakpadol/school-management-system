import 'dart:async';
import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;

import 'picked_upload_file.dart';

Future<PickedUploadFile?> pickUploadFile({String accept = ''}) {
  final completer = Completer<PickedUploadFile?>();
  final input = web.HTMLInputElement()
    ..type = 'file'
    ..accept = accept
    ..style.display = 'none';

  void complete(PickedUploadFile? file) {
    if (!completer.isCompleted) {
      completer.complete(file);
    }
  }

  input.addEventListener(
    'change',
    ((web.Event _) {
      final file = input.files?.item(0);
      if (file == null) {
        complete(null);
        return;
      }
      file.arrayBuffer().toDart.then((buffer) {
        complete(PickedUploadFile(
          name: file.name,
          bytes: Uint8List.view(buffer.toDart),
        ));
      }).catchError((Object _) {
        complete(null);
      });
    }).toJS,
  );

  web.document.body?.appendChild(input);
  input.click();
  return completer.future.whenComplete(() => input.remove());
}

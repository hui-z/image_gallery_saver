import 'dart:async';
// In order to *not* need this ignore, consider extracting the "web" version
// of your plugin as a separate package, instead of inlining it in the same
// package as the core of your plugin.
// ignore: avoid_web_libraries_in_flutter
import 'dart:html';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';
import 'package:image/image.dart';
import 'package:http/http.dart';


class ImageGallerySaverWeb {
  static void registerWith(Registrar registrar) {
    final MethodChannel channel = MethodChannel('image_gallery_saver', const StandardMethodCodec(), registrar);
    final pluginInstance = ImageGallerySaverWeb();
    channel.setMethodCallHandler(pluginInstance.handleMethodCall);
  }

  /// Handles method calls over the MethodChannel of this plugin.
  Future<dynamic> handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'saveImageToGallery':
        return saveImageToGallery(call.arguments['imageBytes'], call.arguments['quality'], call.arguments['name']);
      case 'saveFileToGallery':
        return saveFileToGallery(call.arguments['file'], call.arguments['name']);
      default:
        throw PlatformException(
          code: 'Unimplemented',
          details: 'image_gallery_saver for web doesn\'t implement \'${call.method}\''
        );
    }
  }

  FutureOr<dynamic> saveImageToGallery(Uint8List imageBytes, int quality, String? name) async {
    final image = decodeImage(imageBytes);
    String? errorMessage;
    if (image == null) {
      errorMessage = "Provided 'imageBytes' can not be decoded!";
    } else {
      var imageName = name ?? "image-q$quality.jpg";
      if (!imageName.toLowerCase().endsWith(".jpg") && !imageName.toLowerCase().endsWith(".jpeg")) {
        imageName += ".jpg";
      }

      AnchorElement(href: Uri.dataFromBytes(encodeJpg(image, quality: quality)).toString())
        ..setAttribute("download", imageName)
        ..click();
    }
    return Future.value({"isSuccess": true, "filePath": null, "errorMessage": errorMessage});
  }

  FutureOr<dynamic> saveFileToGallery(String file, String? name) async {
    final bytes = await readBytes(Uri.parse(file));
    var imageName = name ?? file;
    AnchorElement(href: Uri.dataFromBytes(bytes).toString())
      ..setAttribute("download", imageName)
      ..click();
    return Future.value({"isSuccess": true, "filePath": null, "errorMessage": null});
  }
}

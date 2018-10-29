import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class ImageGallerySaver {
  static const MethodChannel _channel =
      const MethodChannel('image_gallery_saver');

  static Future save(Uint8List imageBytes) async {
    assert(imageBytes != null);
    final result =
    await _channel.invokeMethod('saveImageToGallery', imageBytes);
    return result;
  }

}

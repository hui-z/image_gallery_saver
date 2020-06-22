import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class ImageGallerySaver {
  static const MethodChannel _channel =
      const MethodChannel('image_gallery_saver');

  /// save image to Gallery
  /// imageBytes can't null
  static Future saveImage(Uint8List imageBytes, {int quality = 80, String name}) async {
    assert(imageBytes != null);
    final result =
    await _channel.invokeMethod('saveImageToGallery', <String, dynamic> {
      'imageBytes': imageBytes,
      'quality': quality,
      'name': name
    });
    return result;
  }

  /// Save the PNG，JPG，JPEG image or video located at [file] to the local device media gallery.
  static Future saveFile(String file) async {
    assert(file != null);
    final result =
    await _channel.invokeMethod('saveFileToGallery', file);
    return result;
  }

}

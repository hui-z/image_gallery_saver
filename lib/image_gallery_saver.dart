import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class ImageGallerySaver {
  static const MethodChannel _channel =
      const MethodChannel('image_gallery_saver');

  /// save image to Gallery
  /// imageBytes can't null
  /// return Map type
  /// for example:{"isSuccess":true, "filePath":String?}
  static FutureOr<dynamic> saveImage(Uint8List imageBytes,
      {int quality = 80,
      String? name,
      bool isReturnImagePathOfIOS = false}) async {
    assert(imageBytes != null);
    final result =
        await _channel.invokeMethod('saveImageToGallery', <String, dynamic>{
      'imageBytes': imageBytes,
      'quality': quality,
      'name': name,
      'isReturnImagePathOfIOS': isReturnImagePathOfIOS
    });
    return result;
  }

  /// Save the PNG，JPG，JPEG image or video located at [file] to the local device media gallery.
  static Future saveFile(String file,
      {String? name, bool isReturnPathOfIOS = false}) async {
    assert(file != null);
    final result = await _channel.invokeMethod(
        'saveFileToGallery', <String, dynamic>{
      'file': file,
      'name': name,
      'isReturnPathOfIOS': isReturnPathOfIOS
    });
    return result;
  }

  /// Delete the PNG，JPG，JPEG image or video located at [file] from the local device media gallery.
  static Future deleteImage(String identifier) async {
    assert(identifier != null);
    final result =
        await _channel.invokeMethod('deleteImageFromGallery', <String, dynamic>{
      'identifier': identifier,
    });
    return result;
  }
}

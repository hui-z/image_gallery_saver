import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:image_gallery_saver/image_gallery_saver.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const MethodChannel channel = MethodChannel('image_gallery_saver');
  final List<MethodCall> log = <MethodCall>[];
  bool? response;

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      log.add(methodCall);
      return response;
    });
  });

  tearDown(() {
    log.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('saveImageToGallery test', () async {
    response = true;
    Uint8List imageBytes = Uint8List(16);
    final result = await ImageGallerySaver.saveImage(imageBytes);

    expect(
      log,
      <Matcher>[
        isMethodCall('saveImageToGallery', arguments: <String, dynamic>{
          'imageBytes': imageBytes,
          'quality': 80,
          'name': null,
          'isReturnImagePathOfIOS': false,
        }),
      ],
    );
    expect(result, response);
  });
}
# image_gallery_saver

[![Build Status](https://travis-ci.org/hui-z/image_gallery_saver.svg?branch=master)](https://travis-ci.org/hui-z/image_gallery_saver#)
[![pub package](https://img.shields.io/pub/v/image_gallery_saver.svg)](https://pub.dartlang.org/packages/image_gallery_saver)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://choosealicense.com/licenses/mit/)

We use image_picker to select images from the Android and iOS image library, but it can't save image to gallery，this plugin can provide this feature.

## Usage

To use this plugin, add image_gallery_saver as a dependency in your pubspec.yaml file.

## iOS
Your project need create with swift
Add the following keys to your Info.plist file, located in <project root>/ios/Runner/Info.plist:
 * NSPhotoLibraryAddUsageDescription - describe why your app needs permission for the photo library. This is called Privacy - Photo Library Additions Usage Description in the visual editor
 
 ##  Android
 need to ask for storage permission to save the image. You can using https://github.com/BaseflowIT/flutter-permission-handler to handle permission.

## Example
save net image
``` dart
_save() async {
    var response = await Dio().get("https://ss0.baidu.com/94o3dSag_xI4khGko9WTAnF6hhy/image/h%3D300/sign=a62e824376d98d1069d40a31113eb807/838ba61ea8d3fd1fc9c7b6853a4e251f94ca5f46.jpg", options: Options(responseType: ResponseType.bytes));
    final result = await ImageGallerySaver.saveImage(Uint8List.fromList(response.data));
    print(result);
  }
```

save net video
``` dart
_saveVideo() async {
    var appDocDir = await getTemporaryDirectory();
    String savePath = appDocDir.path + "/temp.mp4";
    await Dio().download("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4", savePath);
    final result = await ImageGallerySaver.saveFile(savePath);
    print(result);
 }
```

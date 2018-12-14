# image_gallery_saver

[![Build Status](https://travis-ci.org/hui-z/image_gallery_saver.svg?branch=master)](https://travis-ci.org/hui-z/image_gallery_saver#)
[![pub package](https://img.shields.io/pub/v/image_gallery_saver.svg)](https://pub.dartlang.org/packages/image_gallery_saver)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://choosealicense.com/licenses/mit/)

We use image_picker to selecte images from the Android and iOS image library, but it can't save image to gallery，this plugin can provide this feature.

## Usage

To use this plugin, add image_gallery_saver as a dependency in your pubspec.yaml file.

## iOS
Add the following keys to your Info.plist file, located in <project root>/ios/Runner/Info.plist:
 * NSPhotoLibraryAddUsageDescription - describe why your app needs permission for the photo library. This is called Privacy - Photo Library Additions Usage Description in the visual editor
 
 ##  Android
 No configuration required - the plugin should work out of the box.

## Example
``` dart
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:image_gallery_saver/image_gallery_saver.dart';
import 'dart:ui' as ui;

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
    @override
    Widget build(BuildContext context) {
        return MaterialApp(
            title: 'Flutter Demo',
            theme: ThemeData(
                primarySwatch: Colors.blue,
            ),
            home: MyHomePage(),
        );
    }
}

class MyHomePage extends StatefulWidget {
    @override
    _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
    GlobalKey _globalKey = GlobalKey();

    @override
    Widget build(BuildContext context) {
        return Scaffold(
            appBar: AppBar(
                title: Text(""),
            ),
            body: RepaintBoundary(
                key: _globalKey,
                child: Center(
                    child: Container(
                        child: RaisedButton(
                            onPressed: _saved,
                            child: Text("保存屏幕截图"),
                        ),
                        width: 100,
                        height: 50,
                    ),
                ),
            )
        );
    }
    
    _saved() async {
        RenderRepaintBoundary boundary =
        _globalKey.currentContext.findRenderObject();
        ui.Image image = await boundary.toImage();
        ByteData byteData =
        await image.toByteData(format: ui.ImageByteFormat.png);
        final result = await ImageGallerySaver.save(byteData.buffer.asUint8List());
    }
}
``` 

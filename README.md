# image_gallery_saver
pub package
We use image_picker to selecting images from the Android and iOS image library, but it can't save image to gallery, As well, this package can  provide this function, it only provide this function.

## Usage

To use this plugin, add image_gallery_saver as a dependency in your pubspec.yaml file.

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
child: RaisedButton(
onPressed: _saved,
child: Container(color: Colors.red,),
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
}```

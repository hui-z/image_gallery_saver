example/lib/main.dart

```dart
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
            title: 'Save image to gallery',
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

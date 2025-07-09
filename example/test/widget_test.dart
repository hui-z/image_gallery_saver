import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:image_gallery_saver_example/main.dart';

void main() {
  testWidgets('Verify Widgets', (WidgetTester tester) async {
    await tester.pumpWidget(MyApp());
    await tester.pumpAndSettle();
    final Finder saveButton = find.widgetWithText(ElevatedButton, 'Save Local Image');
    expect(find.widgetWithText(ElevatedButton, 'Save Network Image'), findsOneWidget);
    expect(find.widgetWithText(ElevatedButton, 'Save Network Gif Image'), findsOneWidget);
    expect(find.widgetWithText(ElevatedButton, 'Save Network Video'), findsOneWidget);
    expect(saveButton, findsOneWidget);
  });
}
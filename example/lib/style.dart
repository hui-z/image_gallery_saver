import 'dart:io';

import 'package:flutter/material.dart';

class HFontSizes {
  static const normal = 16.0;
  static const normal2 = 15.0;
  static const small = 14.0;
  static const smaller = 13.0;
}

extension FontWeightExt on FontWeight {
  static const FontWeight light = FontWeight.w300;
  static const FontWeight regular = FontWeight.w400;
  static FontWeight medium = Platform.isAndroid ? FontWeight.w600 : FontWeight.w600;
  static const FontWeight semibold = FontWeight.w800;
  static const FontWeight bold = FontWeight.w900;
}

class HColors {
  static const Color contentBackgroundColor = Colors.white;
  static const Color contentBackgroundGrayColor2 = Color(0xFFF8F8F8);
  static const Color secondaryLabelColor2 = Color(0xFF666666);
  static const Color secondaryLabelColor3 = Color(0xFF999999);
  static const Color secondaryLabelColor4 = Color(0xFFB4B4B4);
  static const Color blue2 = Color(0xFF0091FF);
  static const Color primary = Color(0xFF448AFF);
  static const Color secondary = Color(0xFFE95E3D);
  static const Color mediumGrey = Color(0xFF86919C);
}

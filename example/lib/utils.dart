import 'dart:io';

import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:permission_handler/permission_handler.dart';

import 'dialog.dart';

class Utils {
  static void toast(String msg) {
    Fluttertoast.showToast(
      msg: msg,
      toastLength: Toast.LENGTH_SHORT,
      gravity: ToastGravity.CENTER,
      timeInSecForIosWeb: 1,
      textColor: Colors.white,
      fontSize: 16.0,
      backgroundColor: Colors.black,
    );
  }
}

class PermissionUtil {
  /// 安卓权限
  static List<Permission> androidPermissions = <Permission>[
    // 在这里添加需要的权限
    Permission.storage
  ];

  /// ios权限
  static List<Permission> iosPermissions = <Permission>[
    // 在这里添加需要的权限
    Permission.storage
  ];

  static Future<Map<Permission, PermissionStatus>> requestAll() async {
    if (Platform.isIOS) {
      return await iosPermissions.request();
    }
    return await androidPermissions.request();
  }

  static Future<Map<Permission, PermissionStatus>> request(
      Permission permission) async {
    final List<Permission> permissions = <Permission>[permission];
    return await permissions.request();
  }

  static bool isDenied(Map<Permission, PermissionStatus> result) {
    var isDenied = false;
    result.forEach((key, value) {
      if (value == PermissionStatus.denied) {
        isDenied = true;
        return;
      }
    });
    return isDenied;
  }

  static void showDeniedDialog(BuildContext context) {
    HDialog.show(
        context: context,
        title: '权限申请异常',
        content: '请在【应用信息】-【权限管理】中，开启全部所需权限，以正常使用惠爆单功能',
        options: <DialogAction>[
          DialogAction(text: '去设置', onPressed: () => openAppSettings())
        ]);
  }

  /// 检查权限
  static Future<bool> checkGranted(Permission permission) async {
    PermissionStatus storageStatus = await permission.status;
    if (storageStatus == PermissionStatus.granted) {
      //已授权
      return true;
    } else {
      //拒绝授权
      return false;
    }
  }
}

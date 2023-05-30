import 'dart:async';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'style.dart';

class HDialog {
  static Future<T?> show<T>(
      {required BuildContext context,
      String title = '温馨提示',
      String? content,
      bool barrierDismissible = true,
      Color? barrierColor = Colors.black54,
      bool? useSafeArea,
      bool? useRootNavigator,
      Widget? contentWidget,
      List<DialogAction> options = const [DialogAction(text: '知道了')]}) {
    return showDialog(
        context: context,
        barrierDismissible: barrierDismissible,
        barrierColor: barrierColor,
        useSafeArea: useSafeArea ?? true,
        useRootNavigator: useRootNavigator ?? true,
        builder: (BuildContext context) => HDialogWidget(
              title: title,
              content: content,
              contentWidget: contentWidget,
              options: options,
              isSheetStyle: false,
            ));
  }
}

class DialogAction<T> {
  final String? text;
  final ActionType type;
  final Widget? child;
  final T? actionValue;
  final VoidCallback? onPressed;

  const DialogAction(
      {this.text,
      this.type = ActionType.positive,
      this.child,
      this.actionValue,
      this.onPressed});
}

enum ActionType { delete, positive, negative }

Color _getActionColor(ActionType type) {
  switch (type) {
    case ActionType.delete:
      return HColors.secondary;
    case ActionType.negative:
      return HColors.mediumGrey;
    case ActionType.positive:
      return HColors.primary;
    default:
      return HColors.primary;
  }
}

class HDialogWidget<T> extends StatelessWidget {
  final String? title;
  final String? content;
  final Widget? contentWidget;
  final List<DialogAction<T>> options;
  final DialogAction<T>? bottomSheetCancel;
  final bool isSheetStyle;

  const HDialogWidget(
      {Key? key,
      this.title,
      this.content,
      this.contentWidget,
      this.options = const [],
      this.bottomSheetCancel,
      this.isSheetStyle = false})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (Platform.isIOS) {
      return createIOSDialog(
          content: content,
          contentWidget: contentWidget,
          title: title,
          context: context,
          options: options);
    } else {
      return createAndroidDialog(
          content: content,
          contentWidget: contentWidget,
          title: title,
          context: context,
          options: options);
    }
  }

  AlertDialog createAndroidDialog<T>(
      {required BuildContext context,
      String? title,
      String? content,
      required List<DialogAction<T>> options,
      Widget? contentWidget}) {
    final actions = options.map((option) {
      return CupertinoButton(
          onPressed: () {
            Navigator.pop(context, option.actionValue);
            if (option.onPressed != null) option.onPressed!();
          },
          child: option.child ??
              Text(option.text ?? '',
                  style: TextStyle(color: _getActionColor(option.type))));
    }).toList();
    final dialog = AlertDialog(
      title: title==null ? null : Text(title),
      content: contentWidget != null && content==null
          ? contentWidget
          : Text(content ?? ''),
      actions: actions,
    );
    return dialog;
  }

  Widget createIOSDialog<T>(
      {required BuildContext context,
      String? title,
      String? content,
      Widget? contentWidget,
      required List<DialogAction<T>> options}) {
    final actions = options.map((option) {
      return CupertinoButton(
        onPressed: () {
          Navigator.pop(context, option.actionValue);
          if (option.onPressed != null) option.onPressed!();
        },
        child: option.child ??
            Text(option.text ?? '',
                style: TextStyle(color: _getActionColor(option.type))),
      );
    }).toList();
    return CupertinoAlertDialog(
      title: title==null ? null : Text(title),
      content: contentWidget != null && content == null
          ? contentWidget
          : Text(content ?? ''),
      actions: actions,
    );
  }

  Widget createIOSSheetDialog<T>(
      {required BuildContext context,
      String? title,
      String? content,
      Widget? contentWidget,
      required List<DialogAction<T>> options,
      DialogAction<T>? bottomSheetCancel}) {
    final actions = options.map((option) {
      return CupertinoActionSheetAction(
          onPressed: () {
            Navigator.pop(context, option.actionValue);
            if (option.onPressed != null) option.onPressed!();
          },
          child: option.child ??
              Text(option.text ?? '',
                  style: TextStyle(color: _getActionColor(option.type))));
    }).toList();
    final cancelButton = bottomSheetCancel == null
        ? null
        : CupertinoActionSheetAction(
            onPressed: () {
              Navigator.pop(context, bottomSheetCancel.actionValue);
              if (bottomSheetCancel.onPressed != null)
                bottomSheetCancel.onPressed!();
            },
            isDefaultAction: true,
            child: bottomSheetCancel.child ??
                Text(bottomSheetCancel.text ?? '',
                    style: TextStyle(
                        color: _getActionColor(bottomSheetCancel.type))),
          );
    return CupertinoActionSheet(
        title: title==null ? null : Text(title),
        message: contentWidget != null && content==null
            ? contentWidget
            : Text(content ?? ''),
        actions: actions,
        cancelButton: cancelButton);
  }
}
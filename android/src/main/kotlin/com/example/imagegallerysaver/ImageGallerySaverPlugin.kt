package com.example.imagegallerysaver

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageGallerySaverPlugin(private val registrar: Registrar): MethodCallHandler {

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "image_gallery_saver")
      channel.setMethodCallHandler(ImageGallerySaverPlugin(registrar))
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result): Unit {
    when {
        call.method == "saveImageToGallery" -> {
          val image = call.arguments as ByteArray
          result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image,0,image.size)))
        }
        call.method == "saveFileToGallery" -> {
          val path = call.arguments as String
          result.success(saveFileToGallery(path))
        }
        else -> result.notImplemented()
    }

  }

  private fun generateFile(): File {
    val storePath =  Environment.getExternalStorageDirectory().absolutePath + File.separator + getApplicationName()
    val appDir = File(storePath)
    if (!appDir.exists()) {
      appDir.mkdir()
    }
    val fileName = System.currentTimeMillis().toString() + ".png"
    return File(appDir, fileName)
  }

  private fun saveImageToGallery(bmp: Bitmap): Boolean {
    val context = registrar.activeContext().applicationContext
    val file = generateFile()
    try {
      val fos = FileOutputStream(file)
      val isSuccess = bmp.compress(Bitmap.CompressFormat.PNG, 60, fos)
      fos.flush()
      fos.close()
      val uri = Uri.fromFile(file)
      context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      return isSuccess
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return false
  }

  private fun saveFileToGallery(filePath: String): Boolean {
    val context = registrar.activeContext().applicationContext
    return try {
      val originalFile = File(filePath)
      val file = generateFile()
      originalFile.copyTo(file)

      val uri = Uri.fromFile(file)
      context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      true
    } catch (e: IOException) {
      e.printStackTrace()
      false
    }
  }

  private fun getApplicationName(): String {
    val context = registrar.activeContext().applicationContext
    var ai: ApplicationInfo? = null
    try {
        ai = context.packageManager.getApplicationInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
    }
    var appName: String
    appName = if (ai != null) {
      val charSequence = context.packageManager.getApplicationLabel(ai)
      StringBuilder(charSequence.length).append(charSequence).toString()
    } else {
      "image_gallery_saver"
    }
    return  appName
  }


}

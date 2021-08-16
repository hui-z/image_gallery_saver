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
          val image = call.argument<ByteArray>("imageBytes") ?: return
          val quality = call.argument<Int>("quality") ?: return
          val name = call.argument<String>("name")

          result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image,0,image.size), quality, name))
        }
        call.method == "saveFileToGallery" -> {
          val path = call.argument<String>("file") ?: return
          val name = call.argument<String>("name")
          result.success(saveFileToGallery(path, name))
        }
        else -> result.notImplemented()
    }

  }

  private fun generateFile(extension: String = "", name: String? = null): File {
    val storePath =  Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_PICTURES
    val appDir = File(storePath)
    if (!appDir.exists()) {
      appDir.mkdir()
    }
    var fileName = name?:System.currentTimeMillis().toString()
    if (extension.isNotEmpty()) {
      fileName += (".$extension")
    }
    return File(appDir, fileName)
  }

  private fun saveImageToGallery(bmp: Bitmap, quality: Int, name: String?): HashMap<String, Any?> {
    val context = registrar.activeContext().applicationContext
    val file = generateFile("jpg", name = name)
    return try {
      val fos = FileOutputStream(file)
      println("ImageGallerySaverPlugin $quality")
      bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
      fos.flush()
      fos.close()
      val uri = Uri.fromFile(file)
      context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      bmp.recycle()
      SaveResultModel(uri.toString().isNotEmpty(), uri.toString(), null).toHashMap()
    } catch (e: IOException) {
      SaveResultModel(false, null, e.toString()).toHashMap()
    }
  }

  private fun saveFileToGallery(filePath: String, name: String?): HashMap<String, Any?> {
    val context = registrar.activeContext().applicationContext
    return try {
      val originalFile = File(filePath)
      val file = generateFile(originalFile.extension, name)
      originalFile.copyTo(file)

      val uri = Uri.fromFile(file)
      context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
      SaveResultModel(uri.toString().isNotEmpty(), uri.toString(), null).toHashMap()
    } catch (e: IOException) {
      SaveResultModel(false, null, e.toString()).toHashMap()
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

class SaveResultModel (var isSuccess: Boolean,
                   var filePath: String? = null,
                   var errorMessage: String? = null) {
  fun toHashMap(): HashMap<String, Any?> {
    val hashMap = HashMap<String, Any?>()
    hashMap["isSuccess"] = isSuccess
    hashMap["filePath"] = filePath
    hashMap["errorMessage"] = errorMessage
    return hashMap
  }
}

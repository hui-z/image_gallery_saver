package com.example.imagegallerysaver

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry.Registrar

class ImageGallerySaverPlugin(): MethodCallHandler {
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar): Unit {
      val channel = MethodChannel(registrar.messenger(), "image_gallery_saver")
      channel.setMethodCallHandler(ImageGallerySaverPlugin())
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result): Unit {
    if (call.method == "saveImageToGallery") {
      val image = call.arguments as ByteArray
      result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image,0,image.size)))
    } else {
      result.notImplemented()
    }
  }

  private fun saveImageToGallery(bmp: Bitmap): Boolean {
    val context = registrar.activeContext().applicationContext
    val storePath =  Environment.getExternalStorageDirectory().absolutePath + File.separator + "image_saver_gallery"
    val appDir = File(storePath)
    if (!appDir.exists()) {
      appDir.mkdir()
    }
    val fileName = System.currentTimeMillis().toString() + ".png"
    val file = File(appDir, fileName)
    try {
      val fos = FileOutputStream(file)
      val isSuccess = bmp.compress(Bitmap.CompressFormat.JPEG, 60, fos)
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
}

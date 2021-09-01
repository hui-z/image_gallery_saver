package com.example.imagegallerysaver

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImageGallerySaverPlugin : FlutterPlugin, MethodCallHandler {
    private var applicationContext: Context? = null
    private var methodChannel: MethodChannel? = null


    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val instance = ImageGallerySaverPlugin()
            instance.onAttachedToEngine(registrar.context(), registrar.messenger())
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when {
            call.method == "saveImageToGallery" -> {
                val image = call.argument<ByteArray>("imageBytes") ?: return
                val quality = call.argument<Int>("quality") ?: return
                val name = call.argument<String>("name")

                result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image, 0, image.size), quality, name))
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
        val storePath = Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_PICTURES
        val appDir = File(storePath)
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        var fileName = name ?: System.currentTimeMillis().toString()
        if (extension.isNotEmpty()) {
            fileName += (".$extension")
        }
        return File(appDir, fileName)
    }

    private fun saveImageToGallery(bmp: Bitmap, quality: Int, name: String?): HashMap<String, Any?> {
        val context = applicationContext
        val file = generateFile("jpg", name = name)
        return try {
            val fos = FileOutputStream(file)
            println("ImageGallerySaverPlugin $quality")
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            fos.close()
            val uri = Uri.fromFile(file)
            context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            bmp.recycle()
            SaveResultModel(uri.toString().isNotEmpty(), uri.toString(), null).toHashMap()
        } catch (e: IOException) {
            SaveResultModel(false, null, e.toString()).toHashMap()
        }
    }

    private fun saveFileToGallery(filePath: String, name: String?): HashMap<String, Any?> {
        val context = applicationContext
        return try {
            val originalFile = File(filePath)
            val file = generateFile(originalFile.extension, name)
            originalFile.copyTo(file)

            val uri = Uri.fromFile(file)
            context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            SaveResultModel(uri.toString().isNotEmpty(), uri.toString(), null).toHashMap()
        } catch (e: IOException) {
            SaveResultModel(false, null, e.toString()).toHashMap()
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = null
        methodChannel!!.setMethodCallHandler(null);
        methodChannel = null;
    }

    private fun onAttachedToEngine(applicationContext: Context, messenger: BinaryMessenger) {
        this.applicationContext = applicationContext
        methodChannel = MethodChannel(messenger, "image_gallery_saver")
        methodChannel!!.setMethodCallHandler(this)
    }

}

class SaveResultModel(var isSuccess: Boolean,
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

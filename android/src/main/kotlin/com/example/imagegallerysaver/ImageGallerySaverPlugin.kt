package com.example.imagegallerysaver

import androidx.annotation.NonNull
import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.io.OutputStream

class ImageGallerySaverPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var methodChannel: MethodChannel
    private var applicationContext: Context? = null

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        this.applicationContext = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, "image_gallery_saver")
        methodChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall,@NonNull result: Result): Unit {
        when (call.method) {
            "saveImageToGallery" -> {
                val image = call.argument<ByteArray?>("imageBytes")
                val quality = call.argument<Int?>("quality")
                val name = call.argument<String?>("name")
                val albumName = call.argument<String?>("albumName")

                result.success(
                    saveImageToGallery(
                        BitmapFactory.decodeByteArray(
                            image ?: ByteArray(0),
                            0,
                            image?.size ?: 0
                        ), quality, name, albumName
                    )
                )
            }

            "saveFileToGallery" -> {
                val path = call.argument<String?>("file")
                val name = call.argument<String?>("name")
                result.success(saveFileToGallery(path, name))
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = null
        methodChannel.setMethodCallHandler(null);
    }

    private fun generateUri(extension: String = "", name: String? = null,  albumName: String? = null): Uri? {
        var fileName = name ?: System.currentTimeMillis().toString()
        var albumName =  if (albumName == null) Environment.DIRECTORY_PICTURES else  "${Environment.DIRECTORY_PICTURES}/$albumName"
        val mimeType = getMIMEType(extension)
        val isVideo = mimeType?.startsWith("video")==true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // >= android 10
            val uri = when {
                isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH, when {
                        isVideo -> Environment.DIRECTORY_MOVIES
                        else -> albumName
                    }
                )
                if (!TextUtils.isEmpty(mimeType)) {
                    put(when {isVideo -> MediaStore.Video.Media.MIME_TYPE
                        else -> MediaStore.Images.Media.MIME_TYPE
                    }, mimeType)
                }
            }

            applicationContext?.contentResolver?.insert(uri, values)

        } else {
            // < android 10
            val storePath =
                Environment.getExternalStoragePublicDirectory(when {
                    isVideo -> Environment.DIRECTORY_MOVIES
                    else -> Environment.DIRECTORY_PICTURES
                }).absolutePath
            val appDir = File(storePath).apply {
                if (!exists()) {
                    mkdir()
                }
            }

            val file =
                File(appDir, if (extension.isNotEmpty()) "$fileName.$extension" else fileName)
            Uri.fromFile(file)
        }
    }

    /**
     * get file Mime Type
     *
     * @param extension extension
     * @return file Mime Type
     */
    private fun getMIMEType(extension: String): String? {
        return if (!TextUtils.isEmpty(extension)) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else {
            null
        }
    }

    /**
     * Send storage success notification
     *
     * @param context context
     * @param fileUri file path
     */
    private fun sendBroadcast(context: Context, fileUri: Uri?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = fileUri
            context.sendBroadcast(mediaScanIntent)
        }
    }

    private fun saveImageToGallery(
        bmp: Bitmap?,
        quality: Int?,
        name: String?,
        albumName: String?
    ): HashMap<String, Any?> {
        // check parameters
        if (bmp == null || quality == null) {
            return SaveResultModel(false, null, "parameters error").toHashMap()
        }
        // check applicationContext
        val context = applicationContext
            ?: return SaveResultModel(false, null, "applicationContext null").toHashMap()
        var fileUri: Uri? = null
        var fos: OutputStream? = null
        var success = false
        try {
            fileUri = generateUri("jpg", name = name , albumName = albumName)
            if (fileUri != null) {
                fos = context.contentResolver.openOutputStream(fileUri)
                if (fos != null) {
                    println("ImageGallerySaverPlugin $quality")
                    bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                    fos.flush()
                    success = true
                }
            }
        } catch (e: IOException) {
            SaveResultModel(false, null, e.toString()).toHashMap()
        } finally {
            fos?.close()
            bmp.recycle()
        }
        return if (success) {
            sendBroadcast(context, fileUri)
            SaveResultModel(fileUri.toString().isNotEmpty(), fileUri.toString(), null).toHashMap()
        } else {
            SaveResultModel(false, null, "saveImageToGallery fail").toHashMap()
        }
    }

    private fun saveFileToGallery(filePath: String?, name: String?): HashMap<String, Any?> {
        // check parameters
        if (filePath == null) {
            return SaveResultModel(false, null, "parameters error").toHashMap()
        }
        val context = applicationContext ?: return SaveResultModel(
            false,
            null,
            "applicationContext null"
        ).toHashMap()
        var fileUri: Uri? = null
        var outputStream: OutputStream? = null
        var fileInputStream: FileInputStream? = null
        var success = false

        try {
            val originalFile = File(filePath)
            if(!originalFile.exists()) return SaveResultModel(false, null, "$filePath does not exist").toHashMap()
            fileUri = generateUri(originalFile.extension, name)
            if (fileUri != null) {
                outputStream = context.contentResolver?.openOutputStream(fileUri)
                if (outputStream != null) {
                    fileInputStream = FileInputStream(originalFile)

                    val buffer = ByteArray(10240)
                    var count = 0
                    while (fileInputStream.read(buffer).also { count = it } > 0) {
                        outputStream.write(buffer, 0, count)
                    }

                    outputStream.flush()
                    success = true
                }
            }
        } catch (e: IOException) {
            SaveResultModel(false, null, e.toString()).toHashMap()
        } finally {
            outputStream?.close()
            fileInputStream?.close()
        }
        return if (success) {
            sendBroadcast(context, fileUri)
            SaveResultModel(fileUri.toString().isNotEmpty(), fileUri.toString(), null).toHashMap()
        } else {
            SaveResultModel(false, null, "saveFileToGallery fail").toHashMap()
        }
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

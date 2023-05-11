package com.example.imagegallerysaver

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.text.format.DateUtils
import android.webkit.MimeTypeMap
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImageGallerySaverPlugin : FlutterPlugin, MethodCallHandler {
    private var applicationContext: Context? = null
    private var methodChannel: MethodChannel? = null

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when {
            call.method == "saveImageToGallery" -> {
                val image = call.argument<ByteArray>("imageBytes") ?: return
                val quality = call.argument<Int>("quality") ?: return
                val name = call.argument<String>("name")
                val folder = call.argument<String>("folder")
                if (Build.VERSION.SDK_INT >= 30) {
                    result.success(
                        saveImageToGallery30(
                            applicationContext!!,
                            BitmapFactory.decodeByteArray(image, 0, image.size),
                            name ?: "",
                            folder = folder ?: "",
                        ),
                    )
                } else {
                    result.success(
                        saveImageToGallery(
                            BitmapFactory.decodeByteArray(
                                image,
                                0,
                                image.size,
                            ),
                            quality,
                            name ?: "",
                        ),
                    )
                }
            }

            call.method == "saveFileToGallery" -> {
                val path = call.argument<String>("file") ?: return
                val name = call.argument<String>("name")
                val folder = call.argument<String>("folder")
                val isImage = call.argument<Boolean>("isImage") ?: true

                if (Build.VERSION.SDK_INT >= 30) {
                    result.success(
                        saveFileToGallery30(
                            applicationContext!!,
                            path,
                            name ?: "",
                            folder ?: "",
                            isImage,
                        ),
                    )
                } else {
                    result.success(saveFileToGallery(path, name))
                }
            }

            else -> result.notImplemented()
        }
    }

    private fun generateUri(extension: String = "", name: String? = null): Uri {
        var fileName = name ?: System.currentTimeMillis().toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            val mimeType = getMIMEType(extension)
            if (!TextUtils.isEmpty(mimeType)) {
                values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (mimeType!!.startsWith("video")) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                }
            }
            return applicationContext?.contentResolver?.insert(uri, values)!!
        } else {
            val storePath =
                Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_PICTURES
            val appDir = File(storePath)
            if (!appDir.exists()) {
                appDir.mkdir()
            }
            if (extension.isNotEmpty()) {
                fileName += (".$extension")
            }
            return Uri.fromFile(File(appDir, fileName))
        }
    }

    private fun getMIMEType(extension: String): String? {
        var type: String? = null
        if (!TextUtils.isEmpty(extension)) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase())
        }
        return type
    }

    private fun saveImageToGallery(
        bmp: Bitmap,
        quality: Int,
        name: String,
    ): HashMap<String, Any?> {
        val context = applicationContext
        val currentTime: Long = System.currentTimeMillis()
        val imageDate: String =
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(currentTime))
        val screenshotFileNameTemplate = "%s.jpg"
        val imageFileName: String =
            name.ifEmpty { String.format(screenshotFileNameTemplate, imageDate) }
        val fileUri = generateUri("jpg", name = imageFileName)
        return try {
            val fos = context?.contentResolver?.openOutputStream(fileUri)!!
            println("ImageGallerySaverPlugin $quality")
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            fos.close()
            context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri))
            bmp.recycle()
            SaveResultModel(fileUri.toString().isNotEmpty(), fileUri.toString(), null).toHashMap()
        } catch (e: IOException) {
            SaveResultModel(false, null, e.toString()).toHashMap()
        }
    }

    /**
     * android 10 以上版本
     */
    @TargetApi(Build.VERSION_CODES.Q)
    fun saveImageToGallery30(
        context: Context,
        image: Bitmap,
        name: String?,
        folder: String = "",
    ): HashMap<String, Any?> {
        val currentTime: Long = System.currentTimeMillis()
        val imageDate: String =
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(currentTime))
        val screenshotFileNameTemplate = "%s.png"
        val mImageFileName: String = name ?: String.format(screenshotFileNameTemplate, imageDate)
        val values = ContentValues()

        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            if (folder.isEmpty()) {
                Environment.DIRECTORY_PICTURES
            } else {
                "${Environment.DIRECTORY_PICTURES}${File.separator}$folder"
            },
        )
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, mImageFileName)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        values.put(MediaStore.MediaColumns.DATE_ADDED, currentTime / 1000)
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, currentTime / 1000)
        values.put(
            MediaStore.MediaColumns.DATE_EXPIRES,
            (currentTime + DateUtils.DAY_IN_MILLIS) / 1000,
        )
        values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        val resolver: ContentResolver = context.getContentResolver()
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) return SaveResultModel(false, null, "").toHashMap()

        try {
            resolver.openOutputStream(uri).use { out ->
                if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    return SaveResultModel(false, null, "").toHashMap()
                }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            values.putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            resolver.update(uri, values, null, null)
        } catch (e: IOException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resolver.delete(uri, null)
            }
            return SaveResultModel(false, null, "").toHashMap()
        }
        return SaveResultModel(true, null, "").toHashMap()
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun saveFileToGallery30(
        context: Context,
        filePath: String,
        name: String,
        folder: String,
        isImage: Boolean = true,
    ): HashMap<String, Any?> {
        var fileName = filePath
        if (filePath.contains('/')) {
            fileName = filePath.substringAfterLast("/")
        }
        var suffix = "png"
        if (fileName.contains(".")) {
            suffix = fileName.substringAfterLast(".", "")
        }
        val isImageSuffix = suffix.equals("png", ignoreCase = true)
                || suffix.equals("webp", ignoreCase = true)
                || suffix.equals("jpg", ignoreCase = true)
                || suffix.equals("jpeg", ignoreCase = true)
                || suffix.equals("heic", ignoreCase = true)
                || suffix.equals("gif", ignoreCase = true)
                || suffix.equals("apng", ignoreCase = true)
                || suffix.equals("raw", ignoreCase = true)
                || suffix.equals("svg", ignoreCase = true)
                || suffix.equals("bmp", ignoreCase = true)
                || suffix.equals("tif", ignoreCase = true)

        if (isImage && !isImageSuffix) {
            suffix = "png"
        }
        val currentTime: Long = System.currentTimeMillis()
        val imageDate: String =
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(currentTime))
        val screenshotFileNameTemplate = "%s.$suffix"
        val mImageFileName: String =
            name.ifEmpty { String.format(screenshotFileNameTemplate, imageDate) }
        val values = ContentValues()

        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            if (folder.isEmpty()) {
                if (isImage) {
                    Environment.DIRECTORY_PICTURES
                } else {
                    Environment.DIRECTORY_MOVIES
                }
            } else {
                if (isImage) {
                    "${Environment.DIRECTORY_PICTURES}${File.separator}$folder"
                } else {
                    "${Environment.DIRECTORY_MOVIES}${File.separator}$folder"
                }
            },
        )
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, mImageFileName)
        try {
            values.put(
                MediaStore.MediaColumns.MIME_TYPE,
                if (isImage) "image/$suffix" else "video/$suffix",
            )
        } catch (e: java.lang.Exception) {
        }
        values.put(MediaStore.MediaColumns.DATE_ADDED, currentTime / 1000)
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, currentTime / 1000)
        values.put(
            MediaStore.MediaColumns.DATE_EXPIRES,
            (currentTime + DateUtils.DAY_IN_MILLIS) / 1000,
        )
        values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        val resolver: ContentResolver = context.getContentResolver()
        val uri: Uri? = resolver.insert(
            if (isImage) {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            },
            values,
        )
        if (uri == null) return SaveResultModel(false, null, "").toHashMap()
        try {
            val fileInputStream = FileInputStream(filePath)
            val data = ByteArray(1024)
            var read = 0
            resolver.openOutputStream(uri).use { out ->
                while ((fileInputStream.read(data, 0, data.size).also { read = it }) != -1) {
                    out?.write(data, 0, read)
                }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            values.putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            fileInputStream.close()
            resolver.update(uri, values, null, null)
        } catch (e: IOException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resolver.delete(uri, null)
            }
            return SaveResultModel(false, null, "").toHashMap()
        }
        return SaveResultModel(true, null, "").toHashMap()
    }

    private fun saveFileToGallery(filePath: String, name: String?): HashMap<String, Any?> {
        val context = applicationContext
        return try {
            val originalFile = File(filePath)
            val fileUri = generateUri(originalFile.extension, name)

            val outputStream = context?.contentResolver?.openOutputStream(fileUri)!!
            val fileInputStream = FileInputStream(originalFile)

            val buffer = ByteArray(10240)
            var count = 0
            while (fileInputStream.read(buffer).also { count = it } > 0) {
                outputStream.write(buffer, 0, count)
            }

            outputStream.flush()
            outputStream.close()
            fileInputStream.close()

            context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri))
            SaveResultModel(fileUri.toString().isNotEmpty(), fileUri.toString(), null).toHashMap()
        } catch (e: IOException) {
            SaveResultModel(false, null, e.toString()).toHashMap()
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = null
        methodChannel!!.setMethodCallHandler(null)
        methodChannel = null
    }

    private fun onAttachedToEngine(applicationContext: Context, messenger: BinaryMessenger) {
        this.applicationContext = applicationContext
        methodChannel = MethodChannel(messenger, "image_gallery_saver")
        methodChannel!!.setMethodCallHandler(this)
    }
}

class SaveResultModel(
    var isSuccess: Boolean,
    var filePath: String? = null,
    var errorMessage: String? = null,
) {
    fun toHashMap(): HashMap<String, Any?> {
        val hashMap = HashMap<String, Any?>()
        hashMap["isSuccess"] = isSuccess
        hashMap["filePath"] = filePath
        hashMap["errorMessage"] = errorMessage
        return hashMap
    }
}

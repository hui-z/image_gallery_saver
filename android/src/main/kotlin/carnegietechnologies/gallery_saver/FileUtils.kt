package carnegietechnologies.gallery_saver

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import java.io.*
import android.media.MediaMetadataRetriever

/**
 * Core implementation of methods related to File manipulation
 */
internal object FileUtils {

    private const val TAG = "FileUtils"
    private const val SCALE_FACTOR = 50.0
    private const val BUFFER_SIZE = 1024 * 1024 * 8
    private const val DEGREES_90 = 90
    private const val DEGREES_180 = 180
    private const val DEGREES_270 = 270
    private const val EOF = -1

    /**
     * Inserts image into external storage
     *
     * @param contentResolver - content resolver
     * @param path            - path to temp file that needs to be stored
     * @param folderName      - folder name for storing image
     * @param toDcim          - whether the file should be saved to DCIM
     * @return true if image was saved successfully
     */
    fun insertImage(
        contentResolver: ContentResolver,
        path: String,
        folderName: String?,
        toDcim: Boolean
    ): Boolean {
        val file = File(path)
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.toString())
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        var source = getBytesFromFile(file)

        var directory = Environment.DIRECTORY_PICTURES
        if (toDcim) {
            directory = Environment.DIRECTORY_DCIM
        }

        val rotatedBytes = getRotatedBytesIfNecessary(source, path)

        if (rotatedBytes != null) {
            source = rotatedBytes
        }
        val albumDir = File(getAlbumFolderPath(folderName, MediaType.image, toDcim))
        val imageFilePath = File(albumDir, file.name).absolutePath

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, file.name)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        values.put(MediaStore.Images.Media.SIZE, file.length())

        if (android.os.Build.VERSION.SDK_INT < 29) {
            values.put(MediaStore.Images.ImageColumns.DATA, imageFilePath)
        }
        else {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, directory + File.separator + folderName)
        }

        var imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            imageUri = contentResolver.insert(imageUri, values)

            if (source != null) {
                var outputStream: OutputStream? = null
                if (imageUri != null) {
                    outputStream = contentResolver.openOutputStream(imageUri)
                }

                outputStream?.use {
                    outputStream.write(source)
                }

                if (imageUri != null && android.os.Build.VERSION.SDK_INT < 29) {
                    val pathId = ContentUris.parseId(imageUri)
                    val miniThumb = MediaStore.Images.Thumbnails.getThumbnail(
                            contentResolver, pathId, MediaStore.Images.Thumbnails.MINI_KIND, null)
                    storeThumbnail(contentResolver, miniThumb, pathId)
                }
            } else {
                if (imageUri != null) {
                    contentResolver.delete(imageUri, null, null)
                }
                imageUri = null
            }
        } catch (e: IOException) {
            contentResolver.delete(imageUri!!, null, null)
            return false
        } catch (t: Throwable) {
            return false
        }

        return true
    }

    /**
     * @param source -  array of bytes that will be rotated if it needs to be done
     * @param path   - path to image that needs to be checked for rotation
     * @return - array of bytes from rotated image, if rotation needs to be performed
     */
    private fun getRotatedBytesIfNecessary(source: ByteArray?, path: String): ByteArray? {
        var rotationInDegrees = 0

        try {
            rotationInDegrees = exifToDegrees(getRotation(path))
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
        }

        if (rotationInDegrees == 0) {
            return null
        }

        val bitmap = BitmapFactory.decodeByteArray(source, 0, source!!.size)
        val matrix = Matrix()
        matrix.preRotate(rotationInDegrees.toFloat())
        val adjustedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height, matrix, true
        )
        bitmap.recycle()

        val rotatedBytes = bitmapToArray(adjustedBitmap)

        adjustedBitmap.recycle()

        return rotatedBytes
    }

    /**
     * @param contentResolver - content resolver
     * @param source          - bitmap source image
     * @param id              - path id
     */
    private fun storeThumbnail(
        contentResolver: ContentResolver,
        source: Bitmap,
        id: Long
    ) {

        val matrix = Matrix()

        val scaleX = SCALE_FACTOR.toFloat() / source.width
        val scaleY = SCALE_FACTOR.toFloat() / source.height

        matrix.setScale(scaleX, scaleY)

        val thumb = Bitmap.createBitmap(
            source, 0, 0,
            source.width,
            source.height, matrix,
            true
        )

        val values = ContentValues()
        values.put(MediaStore.Images.Thumbnails.KIND, MediaStore.Images.Thumbnails.MICRO_KIND)
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID, id.toInt())
        values.put(MediaStore.Images.Thumbnails.HEIGHT, thumb.height)
        values.put(MediaStore.Images.Thumbnails.WIDTH, thumb.width)

        val thumbUri = contentResolver.insert(
            MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values
        )

        var outputStream: OutputStream? = null
        try{
        outputStream.use {
            if (thumbUri != null) {
                outputStream = contentResolver.openOutputStream(thumbUri)
            }
        }}catch (e: Exception){
        //avoid crashing on devices that do not support thumb
        }
    }

    /**
     * @param orientation - exif orientation
     * @return how many degrees is file rotated
     */
    private fun exifToDegrees(orientation: Int): Int {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> DEGREES_90
            ExifInterface.ORIENTATION_ROTATE_180 -> DEGREES_180
            ExifInterface.ORIENTATION_ROTATE_270 -> DEGREES_270
            else -> 0
        }
    }

    /**
     * @param path - path to bitmap that needs to be checked for orientation
     * @return exif orientation
     * @throws IOException - can happen while creating [ExifInterface] object for
     * provided path
     */
    @Throws(IOException::class)
    private fun getRotation(path: String): Int {
        val exif = ExifInterface(path)
        return exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }

    private fun bitmapToArray(bmp: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        bmp.recycle()
        return byteArray
    }

    private fun getBytesFromFile(file: File): ByteArray? {
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        val buf = BufferedInputStream(FileInputStream(file))
        buf.use {
            buf.read(bytes, 0, bytes.size)
        }

        return bytes
    }

    /**
     * @param contentResolver - content resolver
     * @param path            - path to temp file that needs to be stored
     * @param folderName      - folder name for storing video
     * @param toDcim          - whether the file should be saved to DCIM
     * @return true if video was saved successfully
     */
    fun insertVideo(
        contentResolver: ContentResolver,
        inputPath: String,
        folderName: String?,
        toDcim: Boolean,
        bufferSize: Int = BUFFER_SIZE
    ): Boolean {
        val inputFile = File(inputPath)
        val inputStream: InputStream?
        val outputStream: OutputStream?

        val extension = MimeTypeMap.getFileExtensionFromUrl(inputFile.toString())
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

        var directory = Environment.DIRECTORY_MOVIES
        if (toDcim) {
            directory = Environment.DIRECTORY_DCIM
        }

        val albumDir = File(getAlbumFolderPath(folderName, MediaType.video, toDcim))
        val videoFilePath = File(albumDir, inputFile.name).absolutePath

        val values = ContentValues()
        values.put(MediaStore.Video.Media.TITLE, inputFile.name)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, inputFile.name)
        values.put(MediaStore.Video.Media.MIME_TYPE, mimeType)
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())

        if (android.os.Build.VERSION.SDK_INT < 29) {
            try {
                val r = MediaMetadataRetriever()
                r.setDataSource(inputPath)
                val durString = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durString!!.toInt()
                values.put(MediaStore.Video.Media.DURATION, duration)
                values.put(MediaStore.Video.VideoColumns.DATA, videoFilePath)
            } catch(e: Exception) {}
        } else {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, directory + File.separator + folderName)
        }

        try {
            val url = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            inputStream = FileInputStream(inputFile)
            if (url != null) {
                outputStream = contentResolver.openOutputStream(url)
                val buffer = ByteArray(bufferSize)
                inputStream.use {
                    outputStream?.use {
                        var len = inputStream.read(buffer)
                        while (len != EOF) {
                            outputStream.write(buffer, 0, len)
                            len = inputStream.read(buffer)
                        }
                    }
                }
            }
        } catch (fnfE: FileNotFoundException) {
            Log.e("GallerySaver", fnfE.message ?: fnfE.toString())
            return false
        } catch (e: Exception) {
            Log.e("GallerySaver", e.message ?: e.toString())
            return false
        }
        return true
    }

    private fun getAlbumFolderPath(
        folderName: String?, 
        mediaType: MediaType,
        toDcim: Boolean
    ): String {
        var albumFolderPath: String = Environment.getExternalStorageDirectory().path
        if (toDcim && android.os.Build.VERSION.SDK_INT < 29) {
            albumFolderPath += File.separator + Environment.DIRECTORY_DCIM;
        }
        albumFolderPath = if (TextUtils.isEmpty(folderName)) {
            var baseFolderName = if (mediaType == MediaType.image)
                Environment.DIRECTORY_PICTURES else
                Environment.DIRECTORY_MOVIES
            if (toDcim) {
                baseFolderName = Environment.DIRECTORY_DCIM;
            }
            createDirIfNotExist(
                Environment.getExternalStoragePublicDirectory(baseFolderName).path
            ) ?: albumFolderPath
        } else {
            createDirIfNotExist(albumFolderPath + File.separator + folderName)
                ?: albumFolderPath
        }
        return albumFolderPath
    }

    private fun createDirIfNotExist(dirPath: String): String? {
        val dir = File(dirPath)
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                return dir.path
            } else {
                return null
            }
        } else {
            return dir.path
        }
    }
}

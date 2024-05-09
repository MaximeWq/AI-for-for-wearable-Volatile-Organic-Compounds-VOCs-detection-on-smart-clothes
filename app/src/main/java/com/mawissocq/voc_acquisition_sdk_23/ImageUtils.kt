package com.mawissocq.voc_acquisition_sdk_23
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object ImageUtils {
    fun decodeImageFile(file: File): Bitmap? {
        return BitmapFactory.decodeFile(file.absolutePath)
    }

}
package com.mawissocq.voc_acquisition3


import com.mawissocq.voc_acquisition3.SelectionView
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.mawissocq.voc_acquisition3.ImageUtils
import com.mawissocq.voc_acquisition3.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private val cameraPermissionRequestCode = 100
    private lateinit var imageCapture: ImageCapture
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var configButton: Button
    private lateinit var selectionView: SelectionView
    private var numRows = 1
    private var numColumns = 1
    private var captureCount = 0


    private var nbCaptureToTake = 1
    private var captureDelay = 100L




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        configButton = findViewById(R.id.configButton)
        selectionView = findViewById(R.id.selectionView)
        //val setNbCaptureButton = findViewById<Button>(R.id.setNbCaptureButton)
        //val setCaptureDelayButton = findViewById<Button>(R.id.setCaptureDelayButton)

        if (selectionView is SelectionView) {
            val selectionView = selectionView as SelectionView
        } else {
        }



        requestCameraPermission()
        openCamera()

        captureButton.setOnClickListener {
            captureImage()
        }

        configButton.setOnClickListener {
            showConfigDialog()
        }





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun showConfigDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_config, null)
        val numRowsInput = dialogView.findViewById<EditText>(R.id.numRowsInput)
        val numColumnsInput = dialogView.findViewById<EditText>(R.id.numColumnsInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Configure Grid")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val rowsStr = numRowsInput.text.toString()
                val columnsStr = numColumnsInput.text.toString()
                val nbCaptureStr = numRowsInput.text.toString()
                val captureDelayStr = numColumnsInput.text.toString()
                if (rowsStr.isNotEmpty() && columnsStr.isNotEmpty()) {
                    numRows = rowsStr.toInt()
                    numColumns = columnsStr.toInt()
                    nbCaptureToTake = nbCaptureStr.toInt()
                    captureDelay = captureDelayStr.toLong()
                    Toast.makeText(
                        this,
                        "Rows: $numRows, Columns: $numColumns, nbCaptureToTake: $nbCaptureToTake, captureDelay: $captureDelay",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }



    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
        }
    }


    private fun isCameraAvailable(): Boolean {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList
        return cameraIds.isNotEmpty()
    }

    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().setTargetRotation(previewView.display.rotation)
                .build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                this.imageCapture = imageCapture
            } catch (exception: Exception) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val currentTime = System.currentTimeMillis()
        val folderName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(currentTime)
        val folder = File(externalMediaDirs.first(), folderName)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, "image_$captureCount.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: file.toUri()
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    print("\nURI:  $savedUri\n")
                    val savedImageFile = File(savedUri.path ?: "")
                    val savedImage = ImageUtils.decodeImageFile(savedImageFile)
                    val croppedImage = cropImage(savedImage)
                    saveImage(croppedImage, folder)
                    val imagesDirectory = "file:///storage/emulated/0/Android/media/com.mawissocq.voc_acquisition3/"
                    processSubImages(imagesDirectory, folderName)

                    captureCount++

                    if (captureCount < nbCaptureToTake) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            captureImage()
                        }, captureDelay)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                }
            })
    }

    private fun cropImage(image: Bitmap?): Bitmap? {
        image?.let { bitmap ->
            val cropTop = bitmap.height / 4
            val cropBottom = bitmap.height * 3 / 4
            val cropLeft = bitmap.width / 4
            val cropRight = bitmap.width * 3 / 4

            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropLeft,
                0,
                bitmap.width / 2,
                bitmap.height / 2
            )
            return croppedBitmap

        }
        return null
    }



    private fun saveImage(bitmap: Bitmap?, folder: File) {
        bitmap?.let { image ->
            val gridWidth = image.width / numColumns
            val gridHeight = image.height / numRows

            var imageIndex = 0
            for (row in 0 until numRows) {
                for (col in 0 until numColumns) {
                    val startX = col * gridWidth
                    val startY = row * gridHeight

                    if (startX + gridWidth <= image.width && startY + gridHeight <= image.height) {
                        val gridBitmap =
                            Bitmap.createBitmap(image, startX, startY, gridWidth, gridHeight)

                        saveGridImage(gridBitmap, row, col, imageIndex++, folder)
                    }
                }
            }
        }
    }

    private fun saveGridImage(bitmap: Bitmap, row: Int, col: Int, imageIndex: Int, folder: File) {
        var output: FileOutputStream? = null
        try {
            val file = File(folder, "grid_image_${imageIndex}_${row}_${col}.jpg")
            output = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                output?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }




    private fun cropAndSaveImage(image: Bitmap?) {
        image?.let { bitmap ->
            val cropTop = 195
            val cropBottom = 208

            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                cropTop,
                bitmap.width,
                bitmap.height - cropTop - cropBottom
            )

            val croppedFile = File(externalMediaDirs.first(), "cropped_image.jpg")
            FileOutputStream(croppedFile).use { output ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }

            Toast.makeText(baseContext, "Image cropped and saved successfully", Toast.LENGTH_SHORT).show()
        }
    }



    private fun calculateAveragePixelValue(bitmap: Bitmap): Double {
        var totalPixelValue = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                totalPixelValue += (red + green + blue) / 3
            }
        }
        val totalPixels = bitmap.width * bitmap.height
        return totalPixelValue.toDouble() / totalPixels
    }



    private fun saveAverageValuesToCSV(averageValues: List<Double>, filePath: String) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            val writer = FileWriter(file, true)
            for ((index, value) in averageValues.withIndex()) {
                writer.append("$value;")
            }
            writer.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun processSubImages(imagesDirectory: String, folderName: String){
        val externalStorageDirectory = Environment.getExternalStorageDirectory()
        val imagesDir = File(externalStorageDirectory, "Android/media/com.mawissocq.voc_acquisition3/$folderName")
        val subImages = imagesDir.listFiles { file -> file.isFile && file.extension == "jpg" }
        println("subimages ${subImages}")
        if (subImages != null && subImages.isNotEmpty()) {
            val averageValues = mutableListOf<Double>()
            for (subImage in subImages.sortedBy { it.nameWithoutExtension }) {
                println("subImage ${subImage}")
                val bitmap = BitmapFactory.decodeFile(subImage.absolutePath)
                val averagePixelValue = calculateAveragePixelValue(bitmap)
                averageValues.add(averagePixelValue)
            }

            saveAverageValuesToCSV(averageValues, "${imagesDir.absolutePath}/../average_values.csv")
        }
    }








}

package com.mawissocq.voc_acquisition_sdk_23


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
import android.widget.TextView
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    private lateinit var coordinatesTextView: TextView




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        configButton = findViewById(R.id.configButton)
        selectionView = findViewById(R.id.selectionView)
        //coordinatesTextView = findViewById(R.id.coordinatesTextView)
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
        val nbCaptureToTakeInput = dialogView.findViewById<EditText>(R.id.nbCaptureToTakeInput)
        val captureDelayInput = dialogView.findViewById<EditText>(R.id.captureDelayInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Configure Grid")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val rowsStr = numRowsInput.text.toString()
                val columnsStr = numColumnsInput.text.toString()
                val nbCaptureStr = nbCaptureToTakeInput.text.toString()
                val captureDelayStr = captureDelayInput.text.toString()
                if (rowsStr.isNotEmpty() && columnsStr.isNotEmpty() && nbCaptureStr.isNotEmpty() && captureDelayStr.isNotEmpty()) {
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
        // Check if the request code matches the camera permission request code
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // If the permission is granted, open the camera
            openCamera()
        } else {
            // Permission denied - can handle this case here if needed

        }
    }


    private fun isCameraAvailable(): Boolean {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList
        return cameraIds.isNotEmpty()
    }

    /**
     * Initializes the camera and sets up the preview and image capture use cases.
     * This function first retrieves the camera provider, then configures the camera
     * to use the back camera for preview and image capture. It also sets up the
     * surface provider for the preview display.
     */

    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Get the camera provider once it's available
            val cameraProvider = cameraProviderFuture.get()
            // Select the default back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Build the preview use case
            val preview = Preview.Builder().build()
            // Build the image capture use case and set the target rotation
            val imageCapture = ImageCapture.Builder().setTargetRotation(previewView.display.rotation)
                .build()

            try {
                cameraProvider.unbindAll()
                // Bind the lifecycle of the camera to the lifecycle owner (this activity)
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

    @SuppressLint("SetTextI18n")
    private fun displaySelectionCoordinates(rect: Rect) {
        val coordinates = "Coordinates: (gauche: ${rect.left}, haut: ${rect.top}) - (droite: ${rect.right}, bas: ${rect.bottom})"
        coordinatesTextView.setText("test ${coordinates}")
    }

    /**
     * Captures an image using the camera, saves it to the external media directory,
     * and processes the captured image. The image is saved with a timestamp-based
     * folder name and a sequentially numbered file name. After saving, the image
     * is cropped to remove white pixels and the cropped image is saved again.
     * If the required number of captures is not yet reached, it schedules the next
     * capture after a specified delay. Finally, it processes sub-images and appends
     * commas to specific CSV files.
     */

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
                    val savedImageFile = File(savedUri.path ?: "")
                    val savedImage = BitmapFactory.decodeFile(savedImageFile.path)

                    CoroutineScope(Dispatchers.Main).launch {
                        val nonWhiteRect = withContext(Dispatchers.Default) {
                            detectNonWhitePixels(savedImage)
                        }

                        val croppedBitmap = Bitmap.createBitmap(
                            savedImage,
                            nonWhiteRect.left,
                            nonWhiteRect.top,
                            nonWhiteRect.width(),
                            nonWhiteRect.height()
                        )

                        saveCroppedBitmap(croppedBitmap, file)
                        saveImage(croppedBitmap, folder)

                        val imagesDirectory = "file:///storage/emulated/0/Android/media/com.mawissocq.voc_acquisition_sdk_23/"
                        //val imagesDirectory = "file:///Phone storage/Android/media/com.mawissocq.voc_acquisition_sdk_23/"
                        val externalStorageDirectory = Environment.getExternalStorageDirectory()

                        val imagesDir = File(
                            externalStorageDirectory,
                            "Android/media/com.mawissocq.voc_acquisition_sdk_23/"
                        )
                        processSubImages(imagesDirectory, folderName)


                        if (captureCount < nbCaptureToTake - 1) {
                            println(" capturecount $captureCount")
                            println("nbCaptureToTake $nbCaptureToTake")
                            Toast.makeText(baseContext, "Capture $captureCount nbCaptureToTake $nbCaptureToTake", Toast.LENGTH_SHORT).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                captureImage()
                            }, captureDelay)
                        } else {
                            appendCommaToEndOfCSV("${imagesDir.absolutePath}/rows_${numRows}_columns_${numColumns}_gray.csv")
                            appendCommaToEndOfCSV("${imagesDir.absolutePath}/rows_${numRows}_columns_${numColumns}_red.csv")
                            appendCommaToEndOfCSV("${imagesDir.absolutePath}/rows_${numRows}_columns_${numColumns}_green.csv")
                            appendCommaToEndOfCSV("${imagesDir.absolutePath}/rows_${numRows}_columns_${numColumns}_blue.csv")
                        }

                        captureCount++
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                }
            })
    }


    /**
     * Detects the non-white pixels in the image and returns the bounding rectangle
     */
    private fun detectNonWhitePixels(bitmap: Bitmap, tolerance: Int = 150): Rect {
        var left = bitmap.width
        var top = bitmap.height
        var right = 0
        var bottom = 0

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                if (!isApproxWhite(red, green, blue, tolerance)) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        //left = (left - 50).coerceAtMost(bitmap.width)
        //top = (top).coerceAtMost(bitmap.height)
        //right = (right + 60).coerceAtLeast(0)
        //bottom = (bottom + 80).coerceAtLeast(0)

        if (left >= right || top >= bottom) {
            left = 0
            top = 0
            right = bitmap.width
            bottom = bitmap.height
        }


        return Rect(left, top, right, bottom)
    }

    /**
     * Checks if the given RGB values are approximately white
     */
    private fun isApproxWhite(red: Int, green: Int, blue: Int, tolerance: Int): Boolean {
        return (255 - red <= tolerance) && (255 - green <= tolerance) && (255 - blue <= tolerance)
    }

    /**
     * Saves the cropped bitmap to a file
     */
    private fun saveCroppedBitmap(bitmap: Bitmap, file: File) {
        val croppedFile = File(file.parentFile, "cropped_${file.name}")
        FileOutputStream(croppedFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        Toast.makeText(baseContext, "Cropped image saved successfully", Toast.LENGTH_SHORT).show()
    }



    /**
     * Not used anymore.
     * Draw a selection rectangle on the bitmap and save it to a file
     */

    fun drawSelectionRectOnBitmap(bitmap: Bitmap, selectionRect: Rect): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(selectionRect, paint)
        return mutableBitmap
    }

    fun saveBitmapWithRect(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        Toast.makeText(baseContext, "Image with selection rect saved successfully", Toast.LENGTH_SHORT).show()
    }

    /**
     * Not used anymore.
     * Was used to return a bitmap with a selection rectangle drawn on it based on given coordinates
     */

    private fun drawSelectionRectangle(bitmap: Bitmap, selectionRect: Rect): Bitmap {
        val resultBitmap = bitmap.copy(bitmap.config, true)
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRect(selectionRect, paint)
        return resultBitmap
    }


/**
 * Splits the given bitmap into a grid based on the specified number of rows and columns,
 * and saves each grid segment as a separate image file in the specified folder.
 */

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





/**
 * Saves a given bitmap as a JPEG image file in the specified folder.
 * The filename includes the image index, row, and column to uniquely identify each grid segment.
 */
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




    /**
     * Calculates the average pixel value (RGB) of the central 20% region of the given bitmap.
     * The average pixel value is computed as the mean of the RGB components for each pixel in this region.
     *
     * @param bitmap The bitmap image to calculate the average pixel value from.
     * @return The average pixel value of the specified region, or 0.0 if no pixels are processed.
     */
    private fun calculateAveragePixelValue(bitmap: Bitmap): Double {
        val width20Percent = (bitmap.width * 0.2).toInt()
        val height20Percent = (bitmap.height * 0.2).toInt()

        // Calculate the starting points for the central 20% region
        val startX = (bitmap.width - width20Percent) / 2
        val startY = (bitmap.height - height20Percent) / 2

        var totalPixelValue = 0
        var totalPixels = 0

        for (x in startX until startX + width20Percent) {
            for (y in startY until startY + height20Percent) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                totalPixelValue += (red + green + blue) / 3
                totalPixels++
            }
        }

        return if (totalPixels > 0) {
            totalPixelValue.toDouble() / totalPixels
        } else {
            0.0
        }
    }

    /**
     * Calculates the average red value of the central 20% region of the given bitmap.
     * The average red value is computed as the mean of the red component of each pixel in this region.
     *
     * @param bitmap The bitmap image to calculate the average red value from.
     * @return The average red value of the specified region, or 0.0 if no pixels are processed.
     */
    private fun calculateAverageRedValue(bitmap: Bitmap): Double {
        val width20Percent = (bitmap.width * 0.2).toInt()
        val height20Percent = (bitmap.height * 0.2).toInt()

        // Calculate the starting points for the central 20% region
        val startX = (bitmap.width - width20Percent) / 2
        val startY = (bitmap.height - height20Percent) / 2

        var totalRedValue = 0
        var totalPixels = 0

        for (x in startX until startX + width20Percent) {
            for (y in startY until startY + height20Percent) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                totalRedValue += red
                totalPixels++
            }
        }

        return if (totalPixels > 0) {
            totalRedValue.toDouble() / totalPixels
        } else {
            0.0
        }
    }

    /**
     * Calculates the average green value of the central 20% region of the given bitmap.
     * The average green value is computed as the mean of the green component of each pixel in this region.
     *
     * @param bitmap The bitmap image to calculate the average green value from.
     * @return The average green value of the specified region, or 0.0 if no pixels are processed.
     */
    private fun calculateAverageGreenValue(bitmap: Bitmap): Double {
        val width20Percent = (bitmap.width * 0.2).toInt()
        val height20Percent = (bitmap.height * 0.2).toInt()

        // Calculate the starting points for the central 20% region
        val startX = (bitmap.width - width20Percent) / 2
        val startY = (bitmap.height - height20Percent) / 2

        var totalGreenValue = 0
        var totalPixels = 0

        for (x in startX until startX + width20Percent) {
            for (y in startY until startY + height20Percent) {
                val pixel = bitmap.getPixel(x, y)
                val green = Color.green(pixel)
                totalGreenValue += green
                totalPixels++
            }
        }

        return if (totalPixels > 0) {
            totalGreenValue.toDouble() / totalPixels
        } else {
            0.0
        }
    }

    /**
     * Calculates the average blue value of the central 20% region of the given bitmap.
     * The average blue value is computed as the mean of the blue component of each pixel in this region.
     *
     * @param bitmap The bitmap image to calculate the average blue value from.
     * @return The average blue value of the specified region, or 0.0 if no pixels are processed.
     */
    private fun calculateAverageBlueValue(bitmap: Bitmap): Double {
        val width20Percent = (bitmap.width * 0.2).toInt()
        val height20Percent = (bitmap.height * 0.2).toInt()

        // Calculate the starting points for the central 20% region
        val startX = (bitmap.width - width20Percent) / 2
        val startY = (bitmap.height - height20Percent) / 2

        var totalBlueValue = 0
        var totalPixels = 0

        for (x in startX until startX + width20Percent) {
            for (y in startY until startY + height20Percent) {
                val pixel = bitmap.getPixel(x, y)
                val blue = Color.blue(pixel)
                totalBlueValue += blue
                totalPixels++
            }
        }

        return if (totalPixels > 0) {
            totalBlueValue.toDouble() / totalPixels
        } else {
            0.0
        }
    }




    /**
     * Appends a line of average values to a CSV file.
     *
     * @param averageValues List of average values to be saved in the CSV file.
     * @param filePath Path to the CSV file where the data will be appended.
     */

    private fun saveAverageValuesToCSV(averageValues: List<Double>, filePath: String) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            val writer = FileWriter(file, true)
            writer.append(SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()))
            writer.append(";")
            for ((index, value) in averageValues.withIndex()) {
                writer.append("$value")
                if (index < averageValues.size - 1) {
                    writer.append(";")
                }
            }
            writer.append("\n")
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Processes sub-images within a specified directory to calculate average pixel values and save them to CSV files.
     *
     * @param imagesDirectory Path to the main directory containing sub-image folders.
     * @param folderName Name of the folder within the main directory where sub-images are located.
     */

    private fun processSubImages(imagesDirectory: String, folderName: String) {
        val externalStorageDirectory = Environment.getExternalStorageDirectory()
        val imagesDir = File(
            externalStorageDirectory,
            "Android/media/com.mawissocq.voc_acquisition_sdk_23/$folderName"
        )
        val subImages = imagesDir.listFiles { file ->
            file.isFile && file.extension == "jpg" && file.name.startsWith("grid_image")
        }
        if (subImages != null && subImages.isNotEmpty()) {
            val averageValuesGray = mutableListOf<Double>()
            val averageValuesRed = mutableListOf<Double>()
            val averageValuesGreen = mutableListOf<Double>()
            val averageValuesBlue = mutableListOf<Double>()
            for (subImage in subImages.sortedBy { it.nameWithoutExtension }) {
                val bitmap = BitmapFactory.decodeFile(subImage.absolutePath)
                val averagePixelValue = calculateAveragePixelValue(bitmap)
                val averageBlue = calculateAverageBlueValue(bitmap)
                val averageGreen = calculateAverageGreenValue(bitmap)
                val averageRed = calculateAverageRedValue(bitmap)

                averageValuesGray.add(averagePixelValue)
                averageValuesRed.add(averageRed)
                averageValuesGreen.add(averageGreen)
                averageValuesBlue.add(averageBlue)
            }

            saveAverageValuesToCSV(
                averageValuesGray,
                "${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_gray.csv"
            )
            saveAverageValuesToCSV(
                averageValuesRed,
                "${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_red.csv"
            )
            saveAverageValuesToCSV(
                averageValuesGreen,
                "${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_green.csv"
            )
            saveAverageValuesToCSV(
                averageValuesBlue,
                "${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_blue.csv"
            )

            try {
                //appendCommaToEndOfCSV("${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_gray.csv")
                //appendCommaToEndOfCSV("${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_red.csv")
                //appendCommaToEndOfCSV("${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_green.csv")
                //appendCommaToEndOfCSV("${imagesDir.absolutePath}/../rows_${numRows}_columns_${numColumns}_blue.csv")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    /**
     * Appends a hyphen to the end of a specified CSV file.
     *
     * This function checks if the given file exists. If it does, it opens the file in append mode,
     * appends a hyphen character to the end, and then closes the file. It logs success or error messages
     * based on the result of the operation.
     *
     * @param filePath The path to the CSV file where the hyphen should be appended.
     */

    private fun appendCommaToEndOfCSV(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                Log.d("appendCommaToEndOfCSV", "File exists: $filePath")
            } else {
                Log.e("appendCommaToEndOfCSV", "File does not exist: $filePath")
                return
            }

            val writer = FileWriter(file, true)
            writer.append("-")
            writer.flush()
            writer.close()

            Log.d("appendCommaToEndOfCSV", "Comma appended successfully")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("appendCommaToEndOfCSV", "Error appending comma: ${e.message}")
        }
    }



}
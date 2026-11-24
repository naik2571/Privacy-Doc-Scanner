package com.docscan.privacy

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.docscan.privacy.data.DocDbHelper
import com.docscan.privacy.data.ScannedDoc
import com.docscan.privacy.databinding.ActivityMainBinding
import com.docscan.privacy.databinding.DialogScanResultBinding
import com.docscan.privacy.ocr.ReceiptParser
import com.docscan.privacy.ui.DocAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "PrivacyDocScan"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DocDbHelper
    private lateinit var docAdapter: DocAdapter

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var isTorchOn = false
    private lateinit var cameraExecutor: ExecutorService

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required to scan documents", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DocDbHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupTabs()
        setupRecycler()
        setupListeners()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupTabs() {
        binding.btnTabScanner.setOnClickListener {
            binding.layoutScannerView.visibility = View.VISIBLE
            binding.layoutLedgerView.visibility = View.GONE

            binding.btnTabScanner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#14B8A6"))
            binding.btnTabScanner.setTextColor(Color.parseColor("#0A0E1A"))
            binding.btnTabLedger.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#12172A"))
            binding.btnTabLedger.setTextColor(Color.WHITE)
        }

        binding.btnTabLedger.setOnClickListener {
            binding.layoutScannerView.visibility = View.GONE
            binding.layoutLedgerView.visibility = View.VISIBLE

            binding.btnTabLedger.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#14B8A6"))
            binding.btnTabLedger.setTextColor(Color.parseColor("#0A0E1A"))
            binding.btnTabScanner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#12172A"))
            binding.btnTabScanner.setTextColor(Color.WHITE)

            loadLedgerData()
        }
    }

    private fun setupRecycler() {
        docAdapter = DocAdapter(emptyList()) { doc ->
            confirmDeleteDoc(doc)
        }
        binding.rvDocs.layoutManager = LinearLayoutManager(this)
        binding.rvDocs.adapter = docAdapter
    }

    private fun setupListeners() {
        binding.btnToggleFlash.setOnClickListener {
            isTorchOn = !isTorchOn
            camera?.cameraControl?.enableTorch(isTorchOn)
        }

        binding.btnSnapOcr.setOnClickListener {
            snapAndProcessReceipt()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun snapAndProcessReceipt() {
        val capture = imageCapture ?: return

        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val fullText = visionText.text
                            val boxes = mutableListOf<Rect>()
                            for (block in visionText.textBlocks) {
                                block.boundingBox?.let { boxes.add(it) }
                            }
                            binding.graphicOverlay.setBoundingBoxes(boxes)

                            val parsed = ReceiptParser.parse(fullText)
                            runOnUiThread {
                                showScanResultDialog(parsed)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "OCR recognition error: ${e.message}", e)
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Image capture error: ${exception.message}", exception)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showScanResultDialog(parsed: ReceiptParser.ParsedReceipt) {
        val dialogBinding = DialogScanResultBinding.inflate(LayoutInflater.from(this))
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.etTitle.setText(parsed.title)
        dialogBinding.etAmount.setText(String.format("%.2f", parsed.totalAmount))
        dialogBinding.etCurrency.setText(parsed.currency)
        dialogBinding.etCategory.setText(parsed.category)

        dialogBinding.btnCancel.setOnClickListener {
            binding.graphicOverlay.clear()
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString().trim().ifEmpty { "Receipt" }
            val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val currency = dialogBinding.etCurrency.text.toString().trim().ifEmpty { "$" }
            val category = dialogBinding.etCategory.text.toString().trim().ifEmpty { "GENERAL" }

            val doc = ScannedDoc(
                title = title,
                totalAmount = amount,
                currency = currency,
                date = parsed.date,
                category = category,
                rawText = parsed.fullText
            )

            dbHelper.insertDoc(doc)
            binding.graphicOverlay.clear()
            dialog.dismiss()
            Toast.makeText(this, "Receipt saved to private ledger!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun loadLedgerData() {
        val docs = dbHelper.getAllDocs()
        docAdapter.updateList(docs)

        val totalSum = dbHelper.getTotalExpenseSum()
        binding.tvTotalExpenses.text = String.format("$%.2f", totalSum)
        binding.tvReceiptCount.text = "${docs.size} Receipts"
        binding.layoutEmptyDocs.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDeleteDoc(doc: ScannedDoc) {
        AlertDialog.Builder(this)
            .setTitle("Delete Receipt")
            .setMessage("Delete receipt from \"${doc.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteDoc(doc.id)
                loadLedgerData()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textRecognizer.close()
    }
}

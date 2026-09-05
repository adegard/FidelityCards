package com.example.fidelitycards.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fidelitycards.R
import com.example.fidelitycards.data.CatimaCodec
import com.example.fidelitycards.databinding.ActivityExportBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExportBinding

    private val createDoc = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) startExport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_back))
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnExport.setOnClickListener {
            createDoc.launch("catima_backup.zip")
        }
    }

    private fun startExport(uri: Uri) {
        binding.progress.visibility = View.VISIBLE
        binding.status.visibility = View.VISIBLE
        binding.status.text = "Exporting..."

        CoroutineScope(Dispatchers.Main).launch {
            val error = withContext(Dispatchers.IO) {
                try {
                    val tempZip = File(cacheDir, "export_${System.currentTimeMillis()}.zip")
                    val err = CatimaCodec.exportToFile(
                        this@ExportActivity,
                        tempZip,
                        binding.chkImages.isChecked,
                        progress = { msg -> runOnUiThread { binding.status.text = msg } }
                    )
                    if (err == null) {
                        contentResolver.openOutputStream(uri, "wt")?.use { out ->
                            tempZip.inputStream().use { it.copyTo(out) }
                        } ?: return@withContext "Could not open destination"
                    }
                    tempZip.delete()
                    err
                } catch (e: Exception) {
                    e.message ?: "Export failed"
                }
            }

            binding.progress.visibility = View.GONE
            if (error != null) {
                binding.status.text = "Export failed: $error"
                Toast.makeText(this@ExportActivity, "Export failed", Toast.LENGTH_LONG).show()
            } else {
                binding.status.text = "Export complete."
                Toast.makeText(this@ExportActivity, "Export complete", Toast.LENGTH_LONG).show()
            }
        }
    }
}

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
import com.example.fidelitycards.databinding.ActivityImportBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportBinding

    private val pickZip = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) startImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_back))
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnPick.setOnClickListener {
            pickZip.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
        }
    }

    private fun startImport(uri: Uri) {
        binding.progress.visibility = View.VISIBLE
        binding.status.visibility = View.VISIBLE
        binding.status.text = "Reading archive..."

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // Copy the content uri to a temp file so ZipInputStream works reliably
                    val tempZip = File(cacheDir, "import_${System.currentTimeMillis()}.zip")
                    contentResolver.openInputStream(uri)?.use { input ->
                        tempZip.outputStream().use { out -> input.copyTo(out) }
                    }
                    CatimaCodec.importFromFile(
                        this@ImportActivity,
                        tempZip,
                        progress = { msg ->
                            runOnUiThread { binding.status.text = msg }
                        }
                    ).also { tempZip.delete() }
                } catch (e: Exception) {
                    CatimaCodec.ImportResult(0, 0, e.message)
                }
            }

            binding.progress.visibility = View.GONE
            if (result.error != null) {
                binding.status.text = "Import failed: ${result.error}"
                Toast.makeText(this@ImportActivity, "Import failed", Toast.LENGTH_LONG).show()
            } else {
                binding.status.text = "Imported ${result.imported} card(s), ${result.imagesImported} image(s)."
                Toast.makeText(this@ImportActivity, "Import complete", Toast.LENGTH_LONG).show()
            }
        }
    }
}

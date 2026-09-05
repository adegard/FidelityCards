package com.example.fidelitycards.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fidelitycards.R
import com.example.fidelitycards.data.BarcodeRenderer
import com.example.fidelitycards.data.CardStore
import com.example.fidelitycards.databinding.ActivityCardDetailBinding
import java.io.File

class CardDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardDetailBinding
    private lateinit var store: CardStore
    private var cardId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = CardStore.get(this)
        cardId = intent.getLongExtra("cardId", 0L)

        binding.toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_back))
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnEdit.setOnClickListener {
            val i = Intent(this, CardEditActivity::class.java)
            i.putExtra("cardId", cardId)
            startActivity(i)
        }

        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete this card?")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(cardId)
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        display()
    }

    private fun display() {
        val card = store.getById(cardId) ?: return
        binding.storeName.text = card.store.ifBlank { "Unknown" }
        binding.barcodeTypeLabel.text = if (card.hasBarcode)
            BarcodeRenderer.displayName(card.barcodeType)
        else
            "No barcode"

        if (card.hasBarcode) {
            val isQr = card.barcodeType.equals("QR_CODE", ignoreCase = true)
            binding.barcodeImage.adjustViewBounds = isQr
            binding.barcodeImage.scaleType =
                if (isQr) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_XY
            val widthPx = resources.displayMetrics.widthPixels
            val heightPx =
                if (isQr) widthPx
                else (widthPx * 0.40).toInt()
            val bmp = BarcodeRenderer.renderPixel(
                card.cardId, card.barcodeType,
                this, widthPx, heightPx
            )
            if (bmp != null) {
                binding.barcodeImage.setImageBitmap(bmp)
            } else {
                binding.barcodeImage.visibility = View.GONE
            }
            binding.barcodeText.text = card.cardId
            binding.barcodeText.visibility = View.VISIBLE
        } else {
            binding.barcodeImage.visibility = View.GONE
            binding.barcodeText.visibility = View.GONE
        }

        binding.noteLabel.text = if (card.note.isBlank()) "" else "Note: ${card.note}"

        loadImage(card.stripeImagePath, binding.frontImage)
        loadImage(card.extraImagePath, binding.backImage)
    }

    private fun loadImage(path: String?, imageView: ImageView) {
        if (path != null && File(path).exists()) {
            imageView.setImageURI(android.net.Uri.fromFile(File(path)))
            imageView.visibility = View.VISIBLE
        } else {
            imageView.visibility = View.GONE
        }
    }
}

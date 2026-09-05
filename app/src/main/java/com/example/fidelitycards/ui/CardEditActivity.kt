package com.example.fidelitycards.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fidelitycards.R
import com.example.fidelitycards.data.CardStore
import com.example.fidelitycards.data.FidelityCard
import com.example.fidelitycards.databinding.ActivityCardEditBinding
import java.io.File

class CardEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardEditBinding
    private lateinit var store: CardStore
    private var cardId: Long = 0L
    private var editingCard: FidelityCard? = null

    private val barcodeTypes = listOf(
        "CODE_128", "EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_39", "CODE_93",
        "ITF", "QR_CODE", "PDF_417", "DATA_MATRIX", "AZTEC", "CODABAR"
    )

    private var iconUri: Uri? = null
    private var frontUri: Uri? = null
    private var backUri: Uri? = null

    private val pickIcon = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) iconUri = uri
    }
    private val pickFront = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) frontUri = uri
    }
    private val pickBack = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) backUri = uri
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = CardStore.get(this)
        cardId = intent.getLongExtra("cardId", 0L)

        binding.toolbar.title = if (cardId > 0) "Edit Card" else "Add Card"
        binding.toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_back))
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.spinnerBarcode.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, barcodeTypes
        )

        // Pre-fill if editing
        editingCard = store.getById(cardId)
        editingCard?.let { c ->
            binding.editStore.setText(c.store)
            binding.editCardId.setText(c.cardId)
            binding.editNote.setText(c.note)
            val idx = barcodeTypes.indexOfFirst { it.equals(c.barcodeType, ignoreCase = true) }
            if (idx >= 0) binding.spinnerBarcode.setSelection(idx)
        }

        binding.btnPickIcon.setOnClickListener { pickIcon.launch("image/*") }
        binding.btnPickFront.setOnClickListener { pickFront.launch("image/*") }
        binding.btnPickBack.setOnClickListener { pickBack.launch("image/*") }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val storeName = binding.editStore.text.toString().trim()
        if (storeName.isEmpty()) {
            Toast.makeText(this, "Store name required", Toast.LENGTH_SHORT).show()
            return
        }

        val barcodeType = barcodeTypes[binding.spinnerBarcode.selectedItemPosition]
        val cardIdStr = binding.editCardId.text.toString().trim()

        val card = editingCard ?: FidelityCard()
        card.store = storeName
        card.cardId = cardIdStr
        card.barcodeType = barcodeType
        card.note = binding.editNote.text.toString().trim()

        iconUri?.let { card.iconImagePath = copyPickedImage(it, "icon") }
        frontUri?.let { card.stripeImagePath = copyPickedImage(it, "front") }
        backUri?.let { card.extraImagePath = copyPickedImage(it, "back") }

        if (card.id > 0) store.update(card) else store.add(card)
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun copyPickedImage(uri: Uri, role: String): String {
        val dir = File(filesDir, "images").apply { mkdirs() }
        val name = "${System.currentTimeMillis()}_$role.png"
        val dest = File(dir, name)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dest.absolutePath
    }
}

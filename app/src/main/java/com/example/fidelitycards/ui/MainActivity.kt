package com.example.fidelitycards.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fidelitycards.R
import com.example.fidelitycards.data.CardStore
import com.example.fidelitycards.data.FidelityCard
import com.example.fidelitycards.databinding.ActivityMainBinding
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.CaptureActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: CardStore
    private lateinit var adapter: CardAdapter

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val text = result.data?.getStringExtra(Intents.Scan.RESULT)
            val format = result.data?.getStringExtra(Intents.Scan.RESULT_FORMAT)
            if (!text.isNullOrBlank()) {
                val i = Intent(this, CardEditActivity::class.java)
                i.putExtra("prefillCardId", text)
                i.putExtra("prefillFormat", format)
                startActivity(i)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = CardStore.get(this)

        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_scan -> {
                    startScan()
                    true
                }
                R.id.action_import -> {
                    startActivity(Intent(this, ImportActivity::class.java))
                    true
                }
                R.id.action_export -> {
                    startActivity(Intent(this, ExportActivity::class.java))
                    true
                }
                else -> false
            }
        }

        adapter = CardAdapter(
            onClick = { openDetail(it) },
            onLongClick = { showCardActions(it) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, CardEditActivity::class.java))
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(store.getAll().sortedWith(compareBy { it.store.lowercase() }))
        binding.emptyView.visibility =
            if (store.count() == 0) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun startScan() {
        try {
            scanLauncher.launch(Intent(this, CaptureActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openDetail(card: FidelityCard) {
        val i = Intent(this, CardDetailActivity::class.java)
        i.putExtra("cardId", card.id)
        startActivity(i)
    }

    private fun showCardActions(card: FidelityCard) {
        val options = arrayOf("View", "Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(card.store)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openDetail(card)
                    1 -> {
                        val i = Intent(this, CardEditActivity::class.java)
                        i.putExtra("cardId", card.id)
                        startActivity(i)
                    }
                    2 -> confirmDelete(card)
                }
            }
            .show()
    }

    private fun confirmDelete(card: FidelityCard) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete \"${card.store}\"?")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(card.id)
                refresh()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

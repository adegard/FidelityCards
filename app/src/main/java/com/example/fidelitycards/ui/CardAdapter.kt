package com.example.fidelitycards.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fidelitycards.R
import com.example.fidelitycards.data.BarcodeRenderer
import com.example.fidelitycards.data.FidelityCard
import java.io.File

class CardAdapter(
    private val onClick: (FidelityCard) -> Unit,
    private val onLongClick: (FidelityCard) -> Unit
) : RecyclerView.Adapter<CardAdapter.VH>() {

    private val items = mutableListOf<FidelityCard>()

    fun submit(list: List<FidelityCard>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val card = items[position]
        holder.store.text = card.store.ifBlank { "Unknown" }
        holder.cardId.text = card.cardId
        holder.barcodeType.text = if (card.hasBarcode) BarcodeRenderer.displayName(card.barcodeType) else "No barcode"

        // Load icon if available
        val iconPath = card.iconImagePath
        if (iconPath != null && File(iconPath).exists()) {
            holder.icon.setImageURI(android.net.Uri.fromFile(File(iconPath)))
        } else {
            holder.icon.setImageDrawable(null)
            holder.icon.setBackgroundColor(card.displayColor)
        }

        holder.itemView.setOnClickListener { onClick(card) }
        holder.itemView.setOnLongClickListener {
            onLongClick(card)
            true
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iconImage)
        val store: TextView = itemView.findViewById(R.id.storeName)
        val cardId: TextView = itemView.findViewById(R.id.cardId)
        val barcodeType: TextView = itemView.findViewById(R.id.barcodeType)
    }
}

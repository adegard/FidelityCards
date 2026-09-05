package com.example.fidelitycards.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CardStore private constructor(context: Context) {

    private val appDir = context.filesDir
    private val storeFile = File(appDir, "cards.json")

    private var cards: MutableList<FidelityCard> = load()

    fun getAll(): List<FidelityCard> = cards.toList()

    fun getById(id: Long): FidelityCard? = cards.find { it.id == id }

    fun getNextId(): Long {
        val max = cards.maxOfOrNull { it.id } ?: 0L
        return maxOf(max + 1, 1L)
    }

    fun add(card: FidelityCard): FidelityCard {
        if (card.id <= 0L) card.id = getNextId()
        cards.add(card)
        save()
        return card
    }

    /** Add a batch of cards, keeping their original ids where possible. */
    fun addAll(newCards: List<FidelityCard>): Int {
        var count = 0
        val existing = cards.map { it.id }.toMutableSet()
        for (c in newCards) {
            if (c.id <= 0L) {
                c.id = getNextId()
            }
            if (existing.contains(c.id)) {
                // id already present; assign a fresh one
                c.id = getNextId()
            }
            existing.add(c.id)
            cards.add(c)
            count++
        }
        save()
        return count
    }

    fun update(card: FidelityCard) {
        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) cards[idx] = card
        save()
    }

    fun delete(id: Long) {
        cards.removeAll { it.id == id }
        save()
    }

    fun clear() {
        cards.clear()
        save()
    }

    fun count(): Int = cards.size

    private fun save() {
        val arr = JSONArray()
        for (c in cards) {
            val o = JSONObject()
            o.put("id", c.id)
            o.put("store", c.store)
            o.put("note", c.note)
            o.put("cardId", c.cardId)
            o.put("barcodeType", c.barcodeType)
            o.put("headerColor", c.headerColor)
            o.put("balance", c.balance)
            o.put("stripeImagePath", c.stripeImagePath ?: JSONObject.NULL)
            o.put("extraImagePath", c.extraImagePath ?: JSONObject.NULL)
            o.put("iconImagePath", c.iconImagePath ?: JSONObject.NULL)
            o.put("lastUsed", c.lastUsed)
            arr.put(o)
        }
        try {
            storeFile.writeText(arr.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun load(): MutableList<FidelityCard> {
        val list = mutableListOf<FidelityCard>()
        if (!storeFile.exists()) return list
        try {
            val arr = JSONArray(storeFile.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val c = FidelityCard(
                    id = o.optLong("id"),
                    store = o.optString("store"),
                    note = o.optString("note"),
                    cardId = o.optString("cardId"),
                    barcodeType = o.optString("barcodeType"),
                    headerColor = o.optInt("headerColor", -416706),
                    balance = o.optDouble("balance", 0.0),
                    stripeImagePath = if (o.isNull("stripeImagePath")) null else o.optString("stripeImagePath"),
                    extraImagePath = if (o.isNull("extraImagePath")) null else o.optString("extraImagePath"),
                    iconImagePath = if (o.isNull("iconImagePath")) null else o.optString("iconImagePath"),
                    lastUsed = o.optLong("lastUsed")
                )
                list.add(c)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        @Volatile
        private var instance: CardStore? = null

        fun get(context: Context): CardStore {
            return instance ?: synchronized(this) {
                instance ?: CardStore(context.applicationContext).also { instance = it }
            }
        }
    }
}

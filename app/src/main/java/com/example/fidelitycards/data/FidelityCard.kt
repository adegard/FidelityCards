package com.example.fidelitycards.data

data class FidelityCard(
    var id: Long = 0L,
    var store: String = "",
    var note: String = "",
    var cardId: String = "",
    var barcodeType: String = "", // CODE_128, EAN_13, UPC_A, QR_CODE, CODE_39, etc.
    var headerColor: Int = -416706,
    var balance: Double = 0.0,
    var stripeImagePath: String? = null,  // front image (local file)
    var extraImagePath: String? = null,   // back image (local file)
    var iconImagePath: String? = null,    // icon image (local file)
    var lastUsed: Long = 0L
) {
    val displayColor: Int
        get() = if (headerColor == 0) -416706 else headerColor

    val hasBarcode: Boolean
        get() = cardId.isNotBlank() && barcodeType.isNotBlank()

    companion object {
        const val CSV_HEADER = "_id,store,note,validfrom,expiry,balance,balancetype,cardid,barcodeid,barcodetype,headercolor,starstatus,lastused,archive"
    }
}

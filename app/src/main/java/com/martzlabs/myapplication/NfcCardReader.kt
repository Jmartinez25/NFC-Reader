package com.martzlabs.myapplication


import android.nfc.tech.IsoDep
import android.util.Log

data class CardData(
    val pan: String? = null,
    val expiry: String? = null,
    val cardholderName: String? = null,
    val track2: String? = null,
    val brand: CardBrand = CardBrand.UNKNOWN
)

enum class CardBrand {
    VISA, MASTERCARD, AMEX, DISCOVER, MAESTRO, DINERS, UNIONPAY, UNKNOWN
}

data class AflEntry(
    val sfi: Int,
    val firstRecord: Int,
    val lastRecord: Int
)
object NfcCardReader {

    private const val TAG = "NfcCardReader"

    private val KNOWN_AIDS = listOf(
        "A0000000031010",  // Visa Credit/Debit
        "A0000000032010",  // Visa Electron
        "A0000000033010",  // Visa Classic
        "A0000000041010",  // Mastercard
        "A0000000043060",  // Maestro
        "A0000000065010",  // Amex
        "A0000003241010",  // Discover
    )


    //Orquesta el proceso. Llama las demás funciones en orden y decide qué hacer si algo falla. Se llama desde mainActivity cuando detecta una tarjeta
    fun readCard(isoDep: IsoDep): CardData {

        // 1. Intentar con el directorio PPSE (método estándar contactless)
        val aidFromPpse = selectPPSE(isoDep)

        val aidToUse =
            aidFromPpse // Si PPSE funciona, perfecto, lo usamos si no, probamos con AIDs conocidos
                ?: tryKnownAids(isoDep)   // fallback: probar AIDs uno a uno
                ?: throw Exception("No se encontró ninguna aplicación de pago")

        // 2. Seleccionar la aplicación encontrada
        val aidResponse = selectAid(isoDep, aidToUse)
        checkSW(aidResponse, "SELECT AID")

        // 3. GET PROCESSING OPTIONS → obtenemos el AFL (mapa de archivos)
        val gpoResponse = getProcessingOptions(isoDep, aidResponse)
        checkSW(gpoResponse, "GPO")

        // 4. READ RECORD de cada entrada del AFL
        val aflEntries = parseAFL(gpoResponse)

        if (aflEntries.isEmpty()) {
            return readRecordsFallback(isoDep)
        }

        return readRecords(isoDep, aflEntries)
    }


    /**
     * PPSE = Proximity Payment System Environment
     * Es el "directorio" que lista las apps de pago en la tarjeta.
     * Nombre: "2PAY.SYS.DDF01" (contactless)
     */
    private fun selectPPSE(isoDep: IsoDep): ByteArray? {
        val ppseBytes = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

        val cmd = byteArrayOf(
            0x00,                    // CLA
            0xA4.toByte(),           // INS: SELECT
            0x04,                    // P1: by name
            0x00,                    // P2: first occurrence
            ppseBytes.size.toByte(), // Lc
            *ppseBytes,
            0x00                     // Le
        )

        val response = isoDep.transceive(cmd)

        // 90 00 = éxito
        if (!isSuccess(response)) {
            return null
        }

        // Buscar el AID dentro de la respuesta del PPSE
        // Estructura TLV: buscamos tag 0x4F (AID)
        return findTag(response, 0x4F)
    }

    private fun tryKnownAids(isoDep: IsoDep): ByteArray? {
        for (aidHex in KNOWN_AIDS) {
            val aid = aidHex.hexToByteArray()
            val response = selectAid(isoDep, aid)
            if (isSuccess(response)) {
                Log.d(TAG, "AID encontrado por fuerza bruta: $aidHex")
                return aid
            }
        }
        return null
    }

    private fun selectAid(isoDep: IsoDep, aid: ByteArray): ByteArray {
        val cmd = byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x00,
            aid.size.toByte(),
            *aid,
            0x00
        )
        return isoDep.transceive(cmd)
    }

    /**
     * Le decimos a la tarjeta que vamos a iniciar una transacción.
     * Responde con:
     *   - AIP: Application Interchange Profile (qué capacidades tiene)
     *   - AFL: Application File Locator (dónde están los datos)
     */
    private fun getProcessingOptions(isoDep: IsoDep, selectAidResponse: ByteArray): ByteArray {

        // Intentar extraer PDOL (Processing Data Object List) de la respuesta del SELECT AID
        // Si no hay PDOL, enviamos un template vacío
        val pdolData = buildPdolData(selectAidResponse)

        val cmd = byteArrayOf(
            0x80.toByte(),              // CLA
            0xA8.toByte(),              // INS: GET PROCESSING OPTIONS
            0x00,                       // P1
            0x00,                       // P2
            (pdolData.size).toByte(),   // Lc
            *pdolData,
            0x00                        // Le
        )

        return isoDep.transceive(cmd)
    }

    /**
     * Si la tarjeta tiene un PDOL, construimos los datos requeridos
     * (rellenados con ceros — suficiente para sólo leer datos).
     *
     * Tag 0x9F38 = PDOL en la respuesta del SELECT AID
     */
    private fun buildPdolData(selectResponse: ByteArray): ByteArray {
        val pdol = findTag(selectResponse, 0x9F38)

        return if (pdol == null) {
            byteArrayOf(0x83.toByte(), 0x00)
        } else {
            // Valores por defecto para los tags más comunes del PDOL
            // En vez de rellenar todo con ceros, damos valores válidos
            val defaults = mapOf(
                0x9F66 to byteArrayOf(0x26, 0x00, 0x00, 0x00), // TTQ: contactless, sin CVM
                0x9F02 to byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // Amount: 0
                0x9F03 to byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00), // Amount Other: 0
                0x9F1A to byteArrayOf(0x01, 0x70), // Terminal Country Code: Colombia (170)
                0x95 to byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00), // TVR: todo en cero
                0x5F2A to byteArrayOf(0x01, 0x70), // Currency Code: COP (170)
                0x9A to byteArrayOf(0x25, 0x05, 0x15), // Fecha: 2025-05-15
                0x9C to byteArrayOf(0x00), // Transaction Type: compra
                0x9F37 to byteArrayOf(0x01, 0x02, 0x03, 0x04), // Unpredictable Number
                0x9F35 to byteArrayOf(0x22), // Terminal Type
                0x9F45 to byteArrayOf(0x00, 0x00), // Data Authentication Code
                0x9F34 to byteArrayOf(0x1F, 0x03, 0x02), // CVM Results
                0x9F21 to byteArrayOf(0x10, 0x30, 0x00), // Transaction Time
                0x9F7C to byteArrayOf(
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                ), // Customer Exclusive Data
            )

            // Parsear el PDOL para saber qué pide y en qué orden
            val result = mutableListOf<Byte>()
            var i = 0
            while (i < pdol.size) {
                // Leer tag
                val t0 = pdol[i].toInt() and 0xFF
                val tag: Int
                if (t0 and 0x1F == 0x1F) {
                    tag = (t0 shl 8) or (pdol[i + 1].toInt() and 0xFF)
                    i += 2
                } else {
                    tag = t0
                    i++
                }
                // Leer longitud requerida
                val len = pdol[i].toInt() and 0xFF
                i++

                // Usar valor conocido o rellenar con ceros
                val value = defaults[tag]
                if (value != null && value.size == len) {
                    result.addAll(value.toList())
                } else {
                    repeat(len) { result.add(0x00) }
                }
            }

            val data = result.toByteArray()
            byteArrayOf(0x83.toByte(), data.size.toByte()) + data
        }
    }

    /**
     * El AFL (tag 0x94) viene como grupos de 4 bytes:
     *   Byte 1: SFI (5 bits altos) + 3 bits ignorados
     *   Byte 2: Primer registro
     *   Byte 3: Último registro
     *   Byte 4: Registros involucrados en autenticación offline (ignoramos)
     */
    private fun parseAFL(gpoResponse: ByteArray): List<AflEntry> {
        val entries = mutableListOf<AflEntry>()

        // El AFL puede venir en formato 80 (primitivo) o 77 (TLV)
        val aflBytes = when {
            gpoResponse[0] == 0x80.toByte() -> {
                // Formato primitivo: 80 Len [AIP 2 bytes] [AFL bytes...]
                val len = gpoResponse[1].toInt() and 0xFF
                if (len > 2) gpoResponse.slice(4 until 2 + len).toByteArray()
                else byteArrayOf()
            }

            gpoResponse[0] == 0x77.toByte() -> {
                findTag(gpoResponse, 0x94) ?: byteArrayOf()
            }

            else -> byteArrayOf()
        }

        if (aflBytes.size % 4 != 0) {
            Log.w(TAG, "AFL con longitud inválida: ${aflBytes.size}")
            return entries
        }

        var i = 0
        while (i + 3 < aflBytes.size) {
            val sfi = (aflBytes[i].toInt() and 0xFF) ushr 3
            val firstRecord = aflBytes[i + 1].toInt() and 0xFF
            val lastRecord = aflBytes[i + 2].toInt() and 0xFF

            if (sfi > 0 && firstRecord > 0) {
                entries.add(AflEntry(sfi, firstRecord, lastRecord))
                Log.d(TAG, "AFL: SFI=$sfi registros $firstRecord-$lastRecord")
            }
            i += 4
        }

        return entries
    }


    private fun readRecords(isoDep: IsoDep, aflEntries: List<AflEntry>): CardData {
        var pan: String? = null
        var expiry: String? = null
        var name: String? = null
        var track2: String? = null

        try {
            for (entry in aflEntries) {
                for (record in entry.firstRecord..entry.lastRecord) {

                    val cmd = byteArrayOf(
                        0x00,
                        0xB2.toByte(),
                        record.toByte(),
                        ((entry.sfi shl 3) or 0x04).toByte(),
                        0x00
                    )

                    val response = try {
                        isoDep.transceive(cmd)
                    } catch (e: android.nfc.TagLostException) {
                        Log.w(
                            TAG,
                            "Tag perdido en SFI=${entry.sfi} rec=$record, devolviendo datos parciales"
                        )
                        return CardData(
                            pan = pan,
                            expiry = expiry,
                            cardholderName = name?.trim(),
                            track2 = track2,
                            brand = pan?.let { detectBrand(it) } ?: CardBrand.UNKNOWN
                        )
                    }

                    if (!isSuccess(response)) continue

                    val recordData = response.dropLast(2).toByteArray()
                    val tlv = parseTLV(recordData)

                    pan = pan ?: tlv[0x5A]?.let { decodePan(it) }
                    expiry = expiry ?: tlv[0x5F24]?.let { decodeExpiry(it) }
                    name = name ?: tlv[0x5F20]?.let { decodeString(it) }
                    track2 = track2 ?: tlv[0x57]?.let { it.toHexString() }

                    if (name == null) {
                        val track1 = tlv[0x56]?.let { decodeString(it) }
                        if (track1 != null) {
                            // Formato Track 1: %B4539...^APELLIDO/NOMBRE^2512...
                            val parts = track1.split("^")
                            if (parts.size >= 2) name = parts[1].trim()
                        }
                    }

                    if (pan != null && expiry != null) return CardData(
                        pan = pan,
                        expiry = expiry,
                        cardholderName = name?.trim(),
                        track2 = track2,
                        brand = pan?.let { detectBrand(it) } ?: CardBrand.UNKNOWN
                    )

                    if (pan != null && expiry != null) return CardData(
                        pan,
                        expiry,
                        name?.trim(),
                        track2
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en readRecords: ${e.message}")
        }

        return CardData(
            pan = pan,
            expiry = expiry,
            cardholderName = name?.trim(),
            track2 = track2,
            brand = pan?.let { detectBrand(it) } ?: CardBrand.UNKNOWN
        )
    }

    /**
     * Fallback: si no hay AFL, intentar leer registros directamente
     * con SFIs del 1 al 10 y registros del 1 al 5
     */
    private fun readRecordsFallback(isoDep: IsoDep): CardData {
        Log.d(TAG, "Usando fallback de lectura directa")
        val fakeAfl = (1..3).map { sfi -> AflEntry(sfi, 1, 5) }
        return readRecords(isoDep, fakeAfl)
    }


    /**
     * PAN en BCD: cada nibble es un dígito
     * 0x45 0x39 0x14 → "453914..."
     * El padding es 'F' al final si el número es impar
     */
    private fun decodePan(bytes: ByteArray): String {
        return bytes.joinToString("") { String.format("%02X", it) }
            .trimEnd('F')
    }

    /**
     * Fecha en BCD: YYMMDD
     * 0x25 0x12 0x31 → año=25, mes=12 → "12/25"
     */
    private fun decodeExpiry(bytes: ByteArray): String {
        val yy = String.format("%02X", bytes[0])
        val mm = String.format("%02X", bytes[1])
        return "$mm/$yy"
    }

    private fun decodeString(bytes: ByteArray): String =
        String(bytes, Charsets.US_ASCII)

    /** Track 2: 4539148803436467D2512... → PAN es antes de la 'D' */
    private fun extractPanFromTrack2(track2: String): String? =
        track2.split("D").firstOrNull()?.trimEnd('F')

    /** Track 2: ...D2512... → expiry es YYMM después de la 'D' */
    private fun extractExpiryFromTrack2(track2: String): String? {
        val parts = track2.split("D")
        if (parts.size < 2 || parts[1].length < 4) return null
        val yy = parts[1].substring(0, 2)
        val mm = parts[1].substring(2, 4)
        return "$mm/$yy"
    }

    /**
     * Devuelve un Map<Int, ByteArray> de tag → valor
     * Soporta tags de 1 y 2 bytes, longitudes de 1 y 2 bytes,
     * y tipos constructed (que se desglosan recursivamente).
     */
    fun parseTLV(data: ByteArray): Map<Int, ByteArray> {
        val result = mutableMapOf<Int, ByteArray>()
        var i = 0

        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF

            val tag: Int
            val isConstructed = (b0 and 0x20) != 0

            if (b0 and 0x1F == 0x1F) {
                // Tag de 2 bytes
                if (i + 1 >= data.size) break
                tag = (b0 shl 8) or (data[i + 1].toInt() and 0xFF)
                i += 2
            } else {
                tag = b0
                i++
            }

            if (i >= data.size) break

            val lenByte = data[i].toInt() and 0xFF
            i++

            val length = when {
                lenByte < 0x80 -> lenByte
                lenByte == 0x81 -> {
                    if (i >= data.size) break
                    (data[i++].toInt() and 0xFF)
                }

                lenByte == 0x82 -> {
                    if (i + 1 >= data.size) break
                    val hi = data[i++].toInt() and 0xFF
                    val lo = data[i++].toInt() and 0xFF
                    (hi shl 8) or lo
                }

                else -> break
            }

            if (i + length > data.size) break

            val value = data.slice(i until i + length).toByteArray()
            i += length

            result[tag] = value

            if (isConstructed) {
                result.putAll(parseTLV(value))
            }
        }

        return result
    }

    fun findTag(data: ByteArray, targetTag: Int): ByteArray? =
        parseTLV(data)[targetTag]


    private fun isSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    private fun checkSW(response: ByteArray, step: String) {
        if (!isSuccess(response)) {
            val sw = response.takeLast(2).joinToString("") { String.format("%02X", it) }
            throw Exception("$step falló con SW: $sw")
        }
    }

    private fun ByteArray.toHexString() =
        joinToString(" ") { String.format("%02X", it) }

    private fun String.hexToByteArray(): ByteArray {
        val s = replace(" ", "")
        return ByteArray(s.length / 2) { i ->
            s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun detectBrand(pan: String): CardBrand {
        return when {
            pan.startsWith("4") -> CardBrand.VISA
            pan.substring(0, 2).toIntOrNull() in 51..55 -> CardBrand.MASTERCARD
            pan.substring(0, 4).toIntOrNull()
                ?.let { it in 2221..2720 } == true -> CardBrand.MASTERCARD

            pan.startsWith("34") || pan.startsWith("37") -> CardBrand.AMEX
            pan.startsWith("6011") -> CardBrand.DISCOVER
            pan.substring(0, 6).toIntOrNull()
                ?.let { it in 622126..622925 } == true -> CardBrand.DISCOVER

            pan.substring(0, 3).toIntOrNull()?.let { it in 644..649 } == true -> CardBrand.DISCOVER
            pan.startsWith("65") -> CardBrand.DISCOVER
            pan.startsWith("300") || pan.startsWith("301") ||
                    pan.startsWith("302") || pan.startsWith("303") ||
                    pan.startsWith("304") || pan.startsWith("305") ||
                    pan.startsWith("36") || pan.startsWith("38") -> CardBrand.DINERS

            pan.startsWith("62") -> CardBrand.UNIONPAY
            pan.startsWith("6304") || pan.startsWith("6759") ||
                    pan.startsWith("6761") || pan.startsWith("6762") ||
                    pan.startsWith("6763") -> CardBrand.MAESTRO

            else -> CardBrand.UNKNOWN
        }
    }
}

fun CardBrand.label() = when (this) {
    CardBrand.VISA -> "Visa"
    CardBrand.MASTERCARD -> "Mastercard"
    CardBrand.AMEX -> "American Express"
    CardBrand.DISCOVER -> "Discover"
    CardBrand.MAESTRO -> "Maestro"
    CardBrand.DINERS -> "Diners Club"
    CardBrand.UNIONPAY -> "UnionPay"
    CardBrand.UNKNOWN -> "Desconocida"
}
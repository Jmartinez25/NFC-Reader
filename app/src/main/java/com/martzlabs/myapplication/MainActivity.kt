package com.martzlabs.myapplication

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.martzlabs.myapplication.ui.ScanScreen
import com.martzlabs.myapplication.ui.WelcomeScreen
import com.martzlabs.myapplication.ui.theme.NFCTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val TAG = "MainActivity"

    private lateinit var nfcAdapter: NfcAdapter

    private val showScan = mutableStateOf(false)
    private val cardResult = mutableStateOf<CardData?>(null)
    private val scanError = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            NFCTheme {
                if (showScan.value) {
                    val onReset = {
                        cardResult.value = null
                        scanError.value = null
                    }

                    ScanScreen(
                        cardData = cardResult.value,
                        scanError = scanError.value,
                        onBack = {
                            showScan.value = false
                            cardResult.value = null
                            scanError.value = null
                        },
                        onReadAnother = onReset,
                        onRetry = onReset
                    )
                } else {
                    WelcomeScreen(
                        onStart = {
                            cardResult.value = null
                            scanError.value = null
                            showScan.value = true
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (!showScan.value) return

        val isoDep = IsoDep.get(tag) ?: return
        try {
            isoDep.connect()
            isoDep.timeout = 10000

            val card = NfcCardReader.readCard(isoDep)
            runOnUiThread {
                cardResult.value = card
                scanError.value = null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo tarjeta: ${e.message}")
            runOnUiThread {
                scanError.value = e.message ?: "Error desconocido"
                cardResult.value = null
            }
        } finally {
            isoDep.close()
        }
    }
}
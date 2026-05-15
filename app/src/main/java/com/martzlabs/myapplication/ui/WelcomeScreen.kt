package com.martzlabs.myapplication.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import com.martzlabs.myapplication.R

enum class NfcState { READY, DISABLED, NOT_SUPPORTED }

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val colors  = MaterialTheme.colorScheme
    val nfcState = rememberNfcState()
    val context  = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(24.dp),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                NfcPulseLogo()
                Spacer(Modifier.height(20.dp))

                Text(
                    text          = "LECTOR NFC",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    color         = colors.primary
                )
                Spacer(Modifier.height(6.dp))

                Text(
                    text       = "Tarjetas bancarias",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color      = colors.onSurface
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text       = "Acerca tu tarjeta al dispositivo para consultar sus datos al instante",
                    fontSize   = 14.sp,
                    color      = colors.onSurface.copy(alpha = 0.55f),
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))

                InfoCard()
                Spacer(Modifier.height(20.dp))
                
                when (nfcState) {

                    NfcState.READY -> {
                        Button(
                            onClick  = onStart,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor   = Color.White
                            )
                        ) {
                            Icon(
                                painter            = painterResource(R.drawable.nfc_logo),
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Empecemos", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text     = "Requiere NFC activado en el dispositivo",
                            fontSize = 11.sp,
                            color    = colors.onSurface.copy(alpha = 0.35f)
                        )
                    }

                    NfcState.DISABLED -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            color    = colors.tertiary.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier  = Modifier.padding(12.dp),
                                verticalAlignment        = Alignment.CenterVertically,
                                horizontalArrangement    = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter            = painterResource(R.drawable.nfc_logo),
                                    contentDescription = null,
                                    tint               = colors.tertiary,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Text(
                                    text       = "NFC está desactivado en tu dispositivo",
                                    fontSize   = 13.sp,
                                    color      = colors.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick  = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor   = Color.White
                            )
                        ) {
                            Text("Activar NFC", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    NfcState.NOT_SUPPORTED -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            color    = colors.error.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier  = Modifier.padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter            = painterResource(R.drawable.nfc_logo),
                                    contentDescription = null,
                                    tint               = colors.error,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text       = "Dispositivo no compatible",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = colors.error
                                    )
                                    Text(
                                        text       = "Esta app requiere un dispositivo con NFC.",
                                        fontSize   = 12.sp,
                                        color      = colors.onSurface.copy(alpha = 0.55f),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NfcPulseLogo() {
    val primary = MaterialTheme.colorScheme.primary

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.signalnfc)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations  = LottieConstants.IterateForever
    )
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value    = primary.toArgb(),
            keyPath  = arrayOf("**")
        )
    )

    LottieAnimation(
        composition       = composition,
        progress          = { progress },
        dynamicProperties = dynamicProperties,
        modifier          = Modifier.size(120.dp)
    )
}

@Composable
fun InfoCard() {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(12.dp),
        color          = colors.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(R.drawable.eye_off,         "Sin almacenamiento", "Los datos no se guardan en ningún lugar del dispositivo.")
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = colors.primary.copy(alpha = 0.1f))
            InfoRow(R.drawable.ic_security_safe, "Sin envío a terceros", "Nada sale de tu teléfono. Sin servidores ni conexión a red.")
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = colors.primary.copy(alpha = 0.1f))
            InfoRow(R.drawable.ic_lock,          "Solo lectura", "No es posible modificar ni clonar tu tarjeta.")
        }
    }
}

@Composable
fun InfoRow(icon: Int, title: String, desc: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier         = Modifier.size(32.dp).background(colors.tertiary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), null, tint = colors.tertiary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 12.sp, color = colors.onSurface.copy(alpha = 0.55f), lineHeight = 17.sp)
        }
    }
}

@Composable
fun rememberNfcState(): NfcState {
    val context = LocalContext.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    var nfcState by remember {
        mutableStateOf(
            when {
                nfcAdapter == null    -> NfcState.NOT_SUPPORTED
                !nfcAdapter.isEnabled -> NfcState.DISABLED
                else                  -> NfcState.READY
            }
        )
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF
                    )
                    nfcState = when (state) {
                        NfcAdapter.STATE_ON   -> NfcState.READY
                        NfcAdapter.STATE_OFF  -> NfcState.DISABLED
                        else                  -> nfcState
                    }
                }
            }
        }

        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return nfcState
}
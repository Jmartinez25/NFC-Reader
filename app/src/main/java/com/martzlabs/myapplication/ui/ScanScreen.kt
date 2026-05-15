package com.martzlabs.myapplication.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import com.martzlabs.myapplication.CardData
import com.martzlabs.myapplication.R
import com.martzlabs.myapplication.label


sealed class ScanState {
    object Waiting : ScanState()
    object Inactive : ScanState()
    object Success : ScanState()
    object Error : ScanState()
}

@Composable
fun ScanScreen(
    cardData: CardData? = null,
    scanError: String? = null,
    onBack: () -> Unit,
    onReadAnother: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val nfcState = rememberNfcState()

    val scanState: ScanState = when {
        cardData != null -> ScanState.Success
        scanError != null -> ScanState.Error
        nfcState == NfcState.DISABLED ||
                nfcState == NfcState.NOT_SUPPORTED -> ScanState.Inactive

        else -> ScanState.Waiting
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScanTopBar(onBack = onBack)

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = scanState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "scan_state"
                ) { state ->
                    when (state) {
                        ScanState.Waiting -> WaitingContent(
                            onCancel = onBack
                        )

                        ScanState.Inactive -> InactiveContent(
                            onActivate = {
                                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                            },
                            onCancel = onBack
                        )

                        ScanState.Success -> SuccessContent(
                            pan = cardData?.pan?.let { formatCardNumber(it) } ?: "",
                            expiry = cardData?.expiry ?: "",
                            name = cardData?.cardholderName,
                            brand = cardData?.brand?.label() ?: "Desconocida",
                            onReadAnother = onReadAnother,
                            onExit = onBack
                        )

                        ScanState.Error -> ErrorContent(
                            error = scanError ?: "Error desconocido",
                            onRetry = onRetry,
                            onCancel = onBack
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun ScanTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(36.dp)
                .background(colors.surfaceVariant, RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = "Lectura NFC",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onBackground,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun WaitingContent(onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            ScanLottie(dimmed = false)
            Spacer(Modifier.height(28.dp))
            NfcBadge(active = true)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Acerca tu tarjeta",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sostén la tarjeta en la parte trasera\ndel dispositivo",
                fontSize = 13.sp,
                color = colors.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.onSurface.copy(alpha = 0.7f)
            )
        ) {
            Text("Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun InactiveContent(onActivate: () -> Unit, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            ScanLottie(dimmed = true)
            Spacer(Modifier.height(28.dp))
            NfcBadge(active = false)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "NFC desactivado",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Activa el NFC en los ajustes del\ndispositivo para continuar",
                fontSize = 13.sp,
                color = colors.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        Button(
            onClick = onActivate,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = Color.White
            )
        ) {
            Text("Activar NFC", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.onSurface.copy(alpha = 0.7f)
            )
        ) {
            Text("Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SuccessContent(
    pan: String,
    expiry: String,
    name: String?,
    brand: String,
    onReadAnother: () -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            SuccessLottie()
            Spacer(Modifier.height(20.dp))
            NfcBadge(active = true, label = "Lectura exitosa")
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ResultRow("PAN", pan, isLast = false, mono = true)
                    ResultRow("Vencimiento", expiry, isLast = false)
                    ResultRow(
                        "Franquicia", brand, isLast = name == null,
                        valueColor = colors.primary
                    )
                    if (name != null) {
                        ResultRow("Titular", name, isLast = true)
                    }
                }
            }
        }

        Button(
            onClick = onReadAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = Color.White
            )
        ) {
            Text("Leer otra tarjeta", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor = colors.onSurface.copy(alpha = 0.7f)
            )
        ) {
            Text("Salir", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ErrorContent(error: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            ScanLottie(dimmed = true)
            Spacer(Modifier.height(28.dp))
            NfcBadge(active = false, label = "Error de lectura")
            Spacer(Modifier.height(14.dp))
            Text(
                text       = "No se pudo leer",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Medium,
                color      = colors.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = error,
                fontSize   = 13.sp,
                color      = colors.onBackground.copy(alpha = 0.5f),
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )
        }


        Button(
            onClick  = onRetry,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor   = Color.White
            )
        ) {
            Text("Intentar de nuevo", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick  = onCancel,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceVariant,
                contentColor   = colors.onSurface.copy(alpha = 0.7f)
            )
        ) {
            Text("Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ScanLottie(dimmed: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.wirelesnfc)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (dimmed) 1 else LottieConstants.IterateForever
    )
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = primary.copy(alpha = if (dimmed) 0.3f else 1f).toArgb(),
            keyPath = arrayOf("**")
        )
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        dynamicProperties = dynamicProperties,
        modifier = Modifier.size(200.dp)
    )
}

@Composable
fun SuccessLottie() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.checkjson)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
        restartOnPlay = false
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(100.dp)
    )
}

@Composable
fun NfcBadge(active: Boolean, label: String? = null) {
    val colors = MaterialTheme.colorScheme
    val dotColor = if (active) colors.tertiary else colors.error
    val text = label ?: if (active) "NFC activo" else "NFC inactivo"
    val textColor = if (active) colors.onSurface.copy(alpha = 0.6f) else colors.error

    Surface(shape = RoundedCornerShape(20.dp), color = colors.surfaceVariant) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier
                .size(6.dp)
                .background(dotColor, CircleShape))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

@Composable
fun ResultRow(
    label: String,
    value: String,
    isLast: Boolean,
    mono: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, color = colors.onSurface.copy(alpha = 0.5f))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
            )
        }
        if (!isLast) HorizontalDivider(
            thickness = 0.5.dp,
            color = colors.primary.copy(alpha = 0.08f)
        )
    }
}

fun formatCardNumber(cardNumber: String): String {
    return cardNumber
        .chunked(4)
        .joinToString("-")
}

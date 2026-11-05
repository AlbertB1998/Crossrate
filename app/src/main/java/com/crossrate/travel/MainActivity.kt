package com.crossrate.travel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFF0D1017)
private val Panel = Color(0xFF191D28)
private val PanelAlt = Color(0xFF212633)
private val Outline = Color(0xFF353C4C)
private val Lavender = Color(0xFFC9B7F3)
private val White = Color(0xFFF5F3FA)
private val Muted = Color(0xFFA6AABB)
private val Green = Color(0xFF9AD5B0)
private val Warning = Color(0xFFFFC78C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Lavender,
                    onPrimary = Background,
                    background = Background,
                    onBackground = White,
                    surface = Panel,
                    onSurface = White,
                )
            ) { CrossrateScreen() }
        }
    }
}

@Composable
private fun CrossrateScreen() {
    val context = LocalContext.current
    val repo = remember(context) { RatesRepository(context.applicationContext) }
    var snapshot by remember { mutableStateOf(repo.cached()) }
    var selectedCode by rememberSaveable { mutableStateOf(CurrencyUnit.USD.name) }
    var amountText by rememberSaveable { mutableStateOf("100") }
    var refreshing by remember { mutableStateOf(false) }
    var refreshError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selected = CurrencyUnit.valueOf(selectedCode)
    val amount = remember(amountText) { FxMath.parseAmount(amountText) }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) { repo.fetchAndCache() }
                snapshot = fresh
                refreshError = null
            } catch (_: Exception) {
                refreshError = if (snapshot == null) {
                    "Could not load rates. Connect to the internet and try again."
                } else {
                    "Could not refresh. Showing your last saved rates."
                }
            } finally {
                refreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val old = snapshot
        if (old == null || System.currentTimeMillis() - old.savedAtMillis > 12L * 60 * 60 * 1000) {
            refresh()
        }
    }

    fun chooseSource(unit: CurrencyUnit) {
        if (selected == unit) return
        val oldSnapshot = snapshot
        if (amount != null && oldSnapshot != null) {
            FxMath.convert(amount, selected.name, unit.name, oldSnapshot.unitsPerUsd)
                ?.let { amountText = FxMath.asInput(it) }
        }
        selectedCode = unit.name
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 25.dp),
        verticalArrangement = Arrangement.spacedBy(21.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CROSSRATE", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp)
                Text("Your four-country price companion", color = Muted, fontSize = 13.sp)
            }
            Surface(color = PanelAlt, shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, Outline)) {
                Text("FX  /  04", color = Lavender, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Caption("CONVERT FROM")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CurrencyUnit.entries.forEach { unit ->
                    val isSelected = unit == selected
                    Surface(
                        modifier = Modifier.weight(1f).clickable { chooseSource(unit) },
                        color = if (isSelected) Lavender else Panel,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isSelected) Lavender else Outline),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(unit.flag, fontSize = 22.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(unit.name, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (isSelected) Background else White)
                        }
                    }
                }
            }
        }

        Surface(color = Panel, shape = RoundedCornerShape(23.dp), border = BorderStroke(1.dp, Outline)) {
            Column(modifier = Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Caption("AMOUNT IN ${selected.name}")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new ->
                        if (new.length <= 24 && new.all { it.isDigit() || it == '.' || it == ',' }) {
                            amountText = new
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(color = White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
                    prefix = { Text(selected.symbol, color = Lavender, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Lavender,
                        unfocusedBorderColor = Outline,
                        focusedContainerColor = PanelAlt,
                        unfocusedContainerColor = PanelAlt,
                        cursorColor = Lavender,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                val hint = if (amountText.isNotEmpty() && amount == null) {
                    "Use numbers with one decimal separator ('.' or ','). No thousands separators."
                } else {
                    "Type a price, then tap another currency to switch."
                }
                Text(hint, color = if (amount == null && amountText.isNotEmpty()) Warning else Muted,
                    fontSize = 12.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("EQUIVALENT VALUES", color = Muted, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    modifier = Modifier.weight(1f))
                Text("TAP TO USE AS INPUT", color = Muted, fontSize = 10.sp)
            }
            CurrencyUnit.entries.filter { it != selected }.forEach { target ->
                val result = if (amount == null || snapshot == null) null else
                    FxMath.convert(amount, selected.name, target.name, snapshot!!.unitsPerUsd)
                val oneUnit = snapshot?.let {
                    FxMath.convert(BigDecimal.ONE, selected.name, target.name, it.unitsPerUsd)
                }
                TargetCard(
                    currency = target,
                    value = result,
                    subtext = oneUnit?.let { "1 ${selected.name} ≈ ${FxMath.formatRate(it)} ${target.name}" }
                        ?: "Waiting for rates",
                    onClick = { chooseSource(target) }
                )
            }
        }

        Surface(color = Panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Outline)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EXCHANGE RATE DATA", color = White, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(if (snapshot != null) "Reference dates: ${snapshot!!.dateLabel()}" else "No rates saved yet",
                            color = Muted, fontSize = 12.sp)
                        snapshot?.let {
                            val checked = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
                                .format(Date(it.savedAtMillis))
                            Text("Last checked: $checked", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Button(onClick = { refresh() }, enabled = !refreshing,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp)) {
                        if (refreshing) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Background, strokeWidth = 2.dp)
                        } else {
                            Text("Refresh", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                val isOld = snapshot?.let { System.currentTimeMillis() - it.savedAtMillis > 24L * 60 * 60 * 1000 } == true
                if (refreshError != null) {
                    Text(refreshError!!, color = Warning, fontSize = 12.sp)
                } else if (isOld) {
                    Text("Saved rates may be outdated. Refresh when online.", color = Warning, fontSize = 12.sp)
                } else if (snapshot != null) {
                    Text("Saved on this device for offline use.", color = Green, fontSize = 12.sp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Indicative mid-market reference rates. Actual bank, cash and card rates may differ.",
                color = Muted, fontSize = 12.sp)
            Text("Argentina: ARS is not the dólar blue or MEP rate.", color = Muted, fontSize = 12.sp)
            Text("Data: Frankfurter · api.frankfurter.dev", color = Lavender, fontSize = 12.sp,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://frankfurter.dev/")))
                })
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Caption(label: String) {
    Text(label, color = Muted, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
private fun TargetCard(
    currency: CurrencyUnit,
    value: BigDecimal?,
    subtext: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Panel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(color = PanelAlt, shape = RoundedCornerShape(14.dp)) {
                Text(currency.flag, fontSize = 26.sp, modifier = Modifier.padding(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(currency.name, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(currency.fullName, color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtext, color = Muted, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (value != null) "${currency.symbol} ${FxMath.formatAmount(value)}" else "—",
                    color = if (value != null) Lavender else Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.End
                )
                Text("↗", color = Muted, fontSize = 14.sp)
            }
        }
    }
}

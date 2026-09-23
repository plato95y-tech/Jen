package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import com.example.util.CalculatorUtils
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.data.entity.TransactionType
import com.example.ui.components.FormatUtils
import com.example.ui.components.StoreEditDialog
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedContainer
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import com.example.ui.viewmodel.DebtViewModel
import com.example.util.ImageStorageUtils
import com.example.util.TafqeetUtils
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    initialStoreId: Long,
    initialTypeString: String,
    txId: Long,
    isStoreLocked: Boolean = false,
    viewModel: DebtViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stores by viewModel.allStores.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val allCurrencies by viewModel.allCurrencies.collectAsStateWithLifecycle()
    val defaultCurrencyCode by viewModel.defaultCurrencyCode.collectAsStateWithLifecycle()

    var selectedStoreId by remember { mutableStateOf(initialStoreId) }
    var selectedType by remember {
        mutableStateOf(
            if (initialTypeString == "PAYMENT") TransactionType.PAYMENT else TransactionType.DEBT
        )
    }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var isStoreDropdownExpanded by remember { mutableStateOf(false) }
    var showNewStoreDialog by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var storeError by remember { mutableStateOf<String?>(null) }
    var attachedImagePath by remember { mutableStateOf<String?>(null) }
    var isCalculatorExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isCalculatorExpanded) {
        if (isCalculatorExpanded) {
            keyboardController?.hide()
        }
    }

    // Multi-currency selection state: default to defaultCurrencyCode from settings
    var selectedCurrencyCode by remember { mutableStateOf("") }
    var isCurrencyDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(defaultCurrencyCode) {
        if (selectedCurrencyCode.isBlank() && defaultCurrencyCode.isNotBlank()) {
            selectedCurrencyCode = defaultCurrencyCode
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = ImageStorageUtils.saveImageToInternalStorage(context, uri)
            if (saved != null) {
                attachedImagePath = saved
            }
        }
    }

    LaunchedEffect(stores, initialStoreId) {
        if (selectedStoreId == 0L) {
            if (initialStoreId > 0L) {
                selectedStoreId = initialStoreId
            } else if (stores.isNotEmpty()) {
                selectedStoreId = stores.first().id
            }
        }
    }

    // If editing existing transaction, load it
    LaunchedEffect(txId) {
        if (txId > 0L) {
            val existing = viewModel.getTransactionById(txId)
            existing?.let {
                selectedStoreId = it.storeId
                selectedType = it.type
                amountText = if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString()
                noteText = it.note
                timestamp = it.timestamp
                attachedImagePath = it.imageUri
                if (it.currencyCode.isNotBlank()) {
                    selectedCurrencyCode = it.currencyCode
                }
            }
        }
    }

    val selectedStore = stores.find { it.id == selectedStoreId }

    val activeCurrency = remember(allCurrencies, selectedCurrencyCode, defaultCurrencyCode) {
        allCurrencies.find { it.code.equals(selectedCurrencyCode, ignoreCase = true) }
            ?: allCurrencies.find { it.code.equals(defaultCurrencyCode, ignoreCase = true) }
            ?: allCurrencies.find { it.isDefault }
    }
    val activeCurrencySymbol = activeCurrency?.symbol ?: currency
    val activeCurrencyCode = activeCurrency?.code ?: defaultCurrencyCode.ifBlank { "YER" }
    val activeExchangeRate = activeCurrency?.exchangeRate ?: 1.0

    val defaultCurrencyEntity = remember(allCurrencies, defaultCurrencyCode) {
        allCurrencies.find { it.code.equals(defaultCurrencyCode, ignoreCase = true) }
            ?: allCurrencies.find { it.isDefault }
    }
    val defaultSymbol = defaultCurrencyEntity?.symbol ?: currency

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (txId > 0L) "تعديل المعاملة" else if (selectedType == TransactionType.DEBT) "تسجيل دين جديد" else "تسجيل دفعة سداد",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector: Debt vs Payment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Debt option
                val isDebtSelected = selectedType == TransactionType.DEBT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDebtSelected) DebtRed else Color.Transparent)
                        .clickable { selectedType = TransactionType.DEBT }
                        .padding(vertical = 10.dp)
                        .testTag("type_debt_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NorthEast,
                            contentDescription = null,
                            tint = if (isDebtSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دين جديد (إضافة)",
                            fontWeight = FontWeight.Bold,
                            color = if (isDebtSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Payment option
                val isPaymentSelected = selectedType == TransactionType.PAYMENT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPaymentSelected) PaymentGreen else Color.Transparent)
                        .clickable { selectedType = TransactionType.PAYMENT }
                        .padding(vertical = 10.dp)
                        .testTag("type_payment_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SouthWest,
                            contentDescription = null,
                            tint = if (isPaymentSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دفعة سداد (خصم)",
                            fontWeight = FontWeight.Bold,
                            color = if (isPaymentSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Amount Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedType == TransactionType.DEBT) DebtRedContainer.copy(alpha = 0.5f) else PaymentGreenContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedType == TransactionType.DEBT) "مبلغ الدين" else "مبلغ السداد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == TransactionType.DEBT) DebtRed else PaymentGreen
                        )

                        // Currency Selector Dropdown (Menu)
                        Box {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { isCurrencyDropdownExpanded = true }
                                    .testTag("currency_selector_trigger"),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$activeCurrencyCode ($activeCurrencySymbol)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "اختيار العملة",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isCurrencyDropdownExpanded,
                                onDismissRequest = { isCurrencyDropdownExpanded = false }
                            ) {
                                allCurrencies.forEach { curr ->
                                    val isCurrent = curr.code.equals(activeCurrencyCode, ignoreCase = true)
                                    val isDef = curr.code.equals(defaultCurrencyCode, ignoreCase = true) || curr.isDefault
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${curr.name} (${curr.symbol})",
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (isDef) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer
                                                        ) {
                                                            Text(
                                                                text = "الافتراضية",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (!isDef) {
                                                    Text(
                                                        text = com.example.util.CurrencyUtils.formatRateRelation(
                                                            currencyCode = curr.code,
                                                            currencySymbol = curr.symbol,
                                                            rateToDefault = curr.exchangeRate,
                                                            defaultCurrencyCode = defaultCurrencyCode,
                                                            defaultCurrencySymbol = defaultSymbol
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCurrencyCode = curr.code
                                            isCurrencyDropdownExpanded = false
                                        },
                                        leadingIcon = if (isCurrent) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val isMath = remember(amountText) { CalculatorUtils.isMathExpression(amountText) }
                    val evaluatedMath = remember(amountText) { CalculatorUtils.evaluate(amountText) }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                if (amountError != null) amountError = null
                            },
                            readOnly = isCalculatorExpanded,
                            placeholder = {
                                Text(
                                    text = "0.00 (مثال: 150+30)",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            },
                            trailingIcon = {
                                Surface(
                                    onClick = {
                                        isCalculatorExpanded = !isCalculatorExpanded
                                        if (isCalculatorExpanded) {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCalculatorExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCalculatorExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .testTag("calculator_toggle_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Calculate,
                                            contentDescription = "آلة حاسبة مدمجة",
                                            tint = if (isCalculatorExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activeCurrencySymbol,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCalculatorExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("amount_input"),
                            singleLine = true,
                            isError = amountError != null,
                            supportingText = amountError?.let {
                                {
                                    Text(
                                        text = it,
                                        color = DebtRed,
                                        style = TextStyle(textDirection = TextDirection.Rtl)
                                    )
                                }
                            },
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textDirection = TextDirection.Ltr,
                                textAlign = TextAlign.Start
                            )
                        )
                    }

                    // Calculator Keypad & Live Result when Expanded
                    if (isCalculatorExpanded) {
                        val currentCalcResult = evaluatedMath ?: CalculatorUtils.evaluate(amountText)
                        if (currentCalcResult != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "= ${CalculatorUtils.formatResult(currentCalcResult)} $activeCurrencySymbol",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PaymentGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calculator_keypad")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Header bar with title and close button to return to keyboard
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Calculate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "الحاسبة المدمجة",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Surface(
                                        onClick = {
                                            isCalculatorExpanded = false
                                            keyboardController?.show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "إغلاق الحاسبة",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "إغلاق",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    val keypadRows = listOf(
                                        listOf("÷" to "OP", "9" to "NUM", "8" to "NUM", "7" to "NUM"),
                                        listOf("×" to "OP", "6" to "NUM", "5" to "NUM", "4" to "NUM"),
                                        listOf("-" to "OP", "3" to "NUM", "2" to "NUM", "1" to "NUM"),
                                        listOf("+" to "OP", "⌫" to "BACKSPACE", "." to "NUM", "0" to "NUM"),
                                        listOf("=" to "EQUALS", "(" to "PAREN", ")" to "PAREN", "C" to "CLEAR")
                                    )

                                    keypadRows.forEach { rowKeys ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowKeys.forEach { (label, type) ->
                                                val isEquals = type == "EQUALS"
                                                val isClear = type == "CLEAR"
                                                val isOp = type == "OP" || type == "PAREN"
                                                val isBackspace = type == "BACKSPACE"

                                                val btnColor = when {
                                                    isEquals -> if (selectedType == TransactionType.DEBT) DebtRed else PaymentGreen
                                                    isClear || isOp -> (if (selectedType == TransactionType.DEBT) DebtRedContainer else PaymentGreenContainer).copy(alpha = 0.6f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                }

                                                val textColor = when {
                                                    isEquals -> Color.White
                                                    isClear || isOp -> if (selectedType == TransactionType.DEBT) DebtRed else PaymentGreen
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }

                                                Surface(
                                                    onClick = {
                                                        if (amountError != null) amountError = null
                                                        when (type) {
                                                            "CLEAR" -> amountText = ""
                                                            "BACKSPACE" -> {
                                                                if (amountText.isNotEmpty()) amountText = amountText.dropLast(1)
                                                            }
                                                            "EQUALS" -> {
                                                                val res = CalculatorUtils.evaluate(amountText)
                                                                if (res != null) {
                                                                    amountText = CalculatorUtils.formatResult(res)
                                                                }
                                                            }
                                                            "OP" -> {
                                                                val trimmed = amountText.trimEnd()
                                                                if (trimmed.isNotEmpty() && trimmed.last() in "+-×÷*/") {
                                                                    amountText = trimmed.dropLast(1) + label
                                                                } else if (trimmed.isNotEmpty() || label == "-") {
                                                                    amountText = trimmed + label
                                                                }
                                                            }
                                                            else -> amountText += label
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = btnColor,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(46.dp)
                                                        .testTag("keypad_btn_$label")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (isBackspace) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                                contentDescription = "مسح رمز",
                                                                tint = textColor,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                text = label,
                                                                fontWeight = if (isEquals) FontWeight.ExtraBold else FontWeight.Bold,
                                                                fontSize = if (isEquals) 20.sp else if (isOp) 18.sp else 16.sp,
                                                                color = textColor
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
                    } else if (isMath && evaluatedMath != null) {
                        // Quick badge when keypad is collapsed but formula entered via software keyboard
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            onClick = {
                                amountText = CalculatorUtils.formatResult(evaluatedMath)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calculator_preview_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الناتج: = ${CalculatorUtils.formatResult(evaluatedMath)} $activeCurrencySymbol",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "اضغط للتطبيق =",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Real-time conversion preview if different from default currency
                    val enteredAmount = remember(amountText) {
                        CalculatorUtils.evaluate(amountText) ?: amountText.toDoubleOrNull()
                    }
                    if (enteredAmount != null && enteredAmount > 0.0 && !activeCurrencyCode.equals(defaultCurrencyCode, ignoreCase = true)) {
                        val convertedToDefault = enteredAmount * activeExchangeRate
                        val df = java.text.DecimalFormat("#,##0.##")
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "يعادل بالعملة الافتراضية ($defaultSymbol): ${df.format(convertedToDefault)} $defaultSymbol (سعر الصرف: $activeExchangeRate)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Real-time live Tafqeet (الكتابة الحرفية للمبلغ بالكلمات العربية فور كتابة الرقم أو حسابه)
                    val textForTafqeet = remember(amountText, isMath, evaluatedMath) {
                        if (isMath && evaluatedMath != null) CalculatorUtils.formatResult(evaluatedMath) else amountText
                    }
                    val spelledAmount = remember(textForTafqeet, activeCurrencySymbol) {
                        TafqeetUtils.spellFromInputString(textForTafqeet, activeCurrencySymbol, includePrefixSuffix = true)
                    }

                    if (!spelledAmount.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spelled_amount_container"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            tonalElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedType == TransactionType.DEBT) DebtRed.copy(alpha = 0.4f) else PaymentGreen.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedType == TransactionType.DEBT) DebtRedContainer else PaymentGreenContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (selectedType == TransactionType.DEBT) DebtRed else PaymentGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "المبلغ كتابةً:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = spelledAmount,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedType == TransactionType.DEBT) DebtRed else PaymentGreen,
                                        modifier = Modifier.testTag("spelled_amount_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Store Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المحل أو العميل *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isStoreLocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "مقفل لهذا العميل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isStoreLocked) {
                    // Locked Card for the specific store/client (cannot pick any other store)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("locked_store_card"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedStore?.name ?: if (stores.isEmpty()) "جاري التحميل..." else "المحل المحدد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!selectedStore?.phone.isNullOrBlank()) {
                                    Text(
                                        text = selectedStore?.phone ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "المحل مقفل",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Editable Dropdown (when accessed from Home Screen, can choose any store or add new)
                    ExposedDropdownMenuBox(
                        expanded = isStoreDropdownExpanded,
                        onExpandedChange = { isStoreDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedStore?.name ?: if (stores.isEmpty()) "لا توجد محلات مضافة" else "اختر المحل أو العميل...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStoreDropdownExpanded) },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("store_selector"),
                            isError = storeError != null,
                            supportingText = storeError?.let { { Text(it, color = DebtRed) } }
                        )
                        ExposedDropdownMenu(
                            expanded = isStoreDropdownExpanded,
                            onDismissRequest = { isStoreDropdownExpanded = false }
                        ) {
                            stores.forEach { store ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(store.name, fontWeight = FontWeight.Bold)
                                            if (store.phone.isNotBlank()) {
                                                Text(store.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedStoreId = store.id
                                        isStoreDropdownExpanded = false
                                        storeError = null
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("+ إضافة محل جديد", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    isStoreDropdownExpanded = false
                                    showNewStoreDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Date and Time Row (Pre-filled automatically, fully editable)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "تاريخ ووقت المعاملة",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date Button
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = timestamp
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    timestamp = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("date_picker_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(FormatUtils.formatDate(timestamp))
                    }

                    // Time Button
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = timestamp
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, m)
                                    }
                                    timestamp = newCal.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("time_picker_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(FormatUtils.formatTime(timestamp))
                    }
                }
            }

            // Note field
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("ملاحظات أو بيان المشتريات (اختياري)") },
                placeholder = { Text("مثال: فاتورة بقالة، شراء مستلزمات...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            // Receipt / Invoice Attachment Section (Proposal #2)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مرفق الفاتورة أو الإيصال الورقي (اختياري)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (attachedImagePath != null) {
                        val file = File(attachedImagePath!!)
                        if (file.exists()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SubcomposeAsyncImage(
                                        model = file,
                                        contentDescription = "صورة الفاتورة",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            Box(
                                                modifier = Modifier.size(56.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "تم إرفاق صورة الفاتورة",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PaymentGreen
                                        )
                                        Text(
                                            text = "انقر لتغيير الصورة أو حذفها",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "تغيير الصورة",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            ImageStorageUtils.deleteImage(attachedImagePath)
                                            attachedImagePath = null
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف المرفق",
                                            tint = DebtRed
                                        )
                                    }
                                }
                            }
                        } else {
                            attachedImagePath = null
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_receipt_image_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرفاق صورة الفاتورة / سند القبض")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Button
            Button(
                onClick = {
                    val evaluatedAmount = CalculatorUtils.evaluate(amountText) ?: amountText.toDoubleOrNull()
                    if (evaluatedAmount == null || evaluatedAmount <= 0.0) {
                        amountError = "يرجى إدخال مبلغ صحيح أكبر من 0 (مثال: 150 أو 150+30)"
                        return@Button
                    }
                    val amount = evaluatedAmount
                    if (selectedStoreId == 0L || selectedStore == null) {
                        storeError = "يرجى اختيار المحل أو العميل"
                        return@Button
                    }

                    viewModel.saveTransaction(
                        id = txId,
                        storeId = selectedStore.id,
                        storeName = selectedStore.name,
                        type = selectedType,
                        amount = amount,
                        currencyCode = activeCurrencyCode,
                        exchangeRate = activeExchangeRate,
                        note = noteText,
                        imageUri = attachedImagePath,
                        timestamp = timestamp
                    ) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TransactionType.DEBT) DebtRed else PaymentGreen
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (txId > 0L) "تعديل وحفظ المعاملة" else if (selectedType == TransactionType.DEBT) "تسجيل الدين" else "تسجيل السداد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showNewStoreDialog) {
        StoreEditDialog(
            initialStore = null,
            onDismiss = { showNewStoreDialog = false },
            onSave = { name, phone, notes, limit, dueDate ->
                viewModel.saveStore(
                    name = name,
                    phone = phone,
                    notes = notes,
                    debtLimit = limit,
                    dueDate = dueDate
                ) { newId ->
                    selectedStoreId = newId
                    storeError = null
                    showNewStoreDialog = false
                }
            }
        )
    }
}

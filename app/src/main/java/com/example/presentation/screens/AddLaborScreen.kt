package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.SavedContact
import com.example.presentation.theme.LaborBackground
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborDivider
import com.example.presentation.theme.LaborTextHint
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen
import com.example.core.util.AppStrings
import java.util.UUID

@Composable
fun AddLaborScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isExpanded by viewModel.isAddStaffExpanded.collectAsState()
    val laborName by viewModel.newLaborName.collectAsState()
    val laborPhone by viewModel.newLaborPhone.collectAsState()
    val laborWage by viewModel.newLaborWage.collectAsState()
    val searchQuery by viewModel.contactsSearchQuery.collectAsState()
    val contacts by viewModel.filteredContacts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val lang = userProfile.language

    val isFormValid = laborName.isNotBlank() && laborPhone.isNotBlank()

    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedContactForAdd by remember { mutableStateOf<SavedContact?>(null) }
    var contactDailyWage by remember { mutableStateOf("800") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactPermission = isGranted
        if (isGranted) {
            viewModel.refreshContacts(context)
            viewModel.showMessage("Contacts loaded successfully")
        } else {
            viewModel.showMessage("Contact permission not granted. You can still add labors manually.")
        }
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
                        val name = if (nameIdx >= 0) it.getString(nameIdx) else "New Labor"
                        val contactId = if (idIdx >= 0) it.getString(idIdx) else ""

                        var phone = ""
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val pIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (pIdx >= 0) phone = pc.getString(pIdx)
                            }
                        }
                        val cleanPhone = phone.replace(" ", "").replace("-", "").ifBlank { "9876543210" }
                        val pickedContact = SavedContact(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            phoneNumber = cleanPhone,
                            avatarColorHex = "#1656D6",
                            initial = name.take(1).uppercase()
                        )
                        selectedContactForAdd = pickedContact
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Refresh contacts when permission is granted
    LaunchedEffect(hasContactPermission) {
        if (hasContactPermission) {
            viewModel.refreshContacts(context)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LaborBackground,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.LaborHome) },
                            modifier = Modifier.testTag("add_labor_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.get("add_labor", lang),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (hasContactPermission) {
                                    viewModel.refreshContacts(context)
                                    viewModel.showMessage("Contacts refreshed")
                                } else {
                                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            modifier = Modifier.testTag("refresh_contacts_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = AppStrings.get("refresh_contacts", lang),
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, bottom = 16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!hasContactPermission) {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            pickContactLauncher.launch(null)
                        }
                    },
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("sync_contacts_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Pick Contact from Phone",
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Permission Banner (if not granted yet)
            if (!hasContactPermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = LaborBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.get("permission_contacts_title", lang),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = AppStrings.get("permission_contacts_desc", lang),
                                fontSize = 13.sp,
                                color = LaborTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(AppStrings.get("allow_contacts", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { pickContactLauncher.launch(null) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(AppStrings.get("pick_from_device", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2.2 Collapsible 'Add Staff' Section Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collapsible_add_staff_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Card Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleAddStaffExpanded() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(LaborBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = LaborBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = AppStrings.get("add_staff", lang),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborBlue
                                    )
                                    Text(
                                        text = AppStrings.get("non_contact_staff", lang),
                                        fontSize = 13.sp,
                                        color = LaborTextSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = LaborTextSecondary
                            )
                        }

                        // Expanded Form
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            ) {
                                HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                // Field 1: Enter Labor Name
                                OutlinedTextField(
                                    value = laborName,
                                    onValueChange = { viewModel.onNewLaborNameChanged(it) },
                                    placeholder = { Text(AppStrings.get("enter_labor_name", lang), color = LaborTextHint) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_labor_name"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LaborBlue,
                                        unfocusedBorderColor = LaborDivider
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Field 2: Mobile Number
                                OutlinedTextField(
                                    value = laborPhone,
                                    onValueChange = { viewModel.onNewLaborPhoneChanged(it) },
                                    placeholder = { Text(AppStrings.get("mobile_number", lang), color = LaborTextHint) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_labor_phone"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LaborBlue,
                                        unfocusedBorderColor = LaborDivider
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Daily Wage Rate Field
                                OutlinedTextField(
                                    value = laborWage,
                                    onValueChange = { viewModel.onNewLaborWageChanged(it) },
                                    placeholder = { Text(AppStrings.get("daily_wage", lang), color = LaborTextHint) },
                                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = LaborTextPrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_labor_wage"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LaborBlue,
                                        unfocusedBorderColor = LaborDivider
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val success = viewModel.addLaborFromForm()
                                        if (success) {
                                            viewModel.showMessage("Labor added successfully")
                                        }
                                    },
                                    enabled = isFormValid,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_submit_add_labor"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LaborBlue,
                                        disabledContainerColor = Color(0xFFD1D5DB),
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = AppStrings.get("add_labor", lang),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onContactsSearchQueryChanged(it) },
                    placeholder = { Text(AppStrings.get("search_contact", lang), fontSize = 14.sp, color = LaborTextHint) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = LaborBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contacts_search_bar"),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LaborBlue,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Contacts List Section Label
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("saved_contacts", lang) + " (${contacts.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LaborTextSecondary
                    )

                    Text(
                        text = "Tap contact to add",
                        fontSize = 12.sp,
                        color = LaborBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Continuous Contact list with fast Lazy recycling
            if (contacts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No contacts found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use the 'Add Staff' form above to register staff directly, or tap 'Allow Contact Access' to import from your phone.",
                                fontSize = 13.sp,
                                color = LaborTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = contacts,
                    key = { _, contact -> contact.id }
                ) { index, contact ->
                    val isFirst = index == 0
                    val isLast = index == contacts.size - 1
                    val cardShape = when {
                        isFirst && isLast -> RoundedCornerShape(16.dp)
                        isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> androidx.compose.ui.graphics.RectangleShape
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedContactForAdd = contact }
                            .testTag("contact_item_${contact.id}"),
                        shape = cardShape,
                        color = Color.White,
                        shadowElevation = if (isFirst || isLast) 1.dp else 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val avatarBgColor = remember(contact.avatarColorHex) {
                                        try {
                                            Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                                        } catch (e: Exception) {
                                            Color(0xFFFFD1B3)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(avatarBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.initial,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = contact.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LaborTextPrimary
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            fontSize = 13.sp,
                                            color = LaborTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "+ Add",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborBlue,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (!isLast) {
                                HorizontalDivider(
                                    color = Color(0xFFE5E7EB),
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(60.dp)) }
            }
        }
    }

    // Quick Confirm Wage & Add Contact as Labor Dialog
    if (selectedContactForAdd != null) {
        val contact = selectedContactForAdd!!
        AlertDialog(
            onDismissRequest = { selectedContactForAdd = null },
            title = {
                Text(
                    text = "Add ${contact.name} as Labor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Phone: ${contact.phoneNumber}",
                        fontSize = 14.sp,
                        color = LaborTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Set Daily Wage (₹)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LaborTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = contactDailyWage,
                        onValueChange = { contactDailyWage = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wage = contactDailyWage.toDoubleOrNull() ?: 800.0
                        viewModel.addLaborFromContact(contact.copy(), wage)
                        viewModel.showMessage("Added ${contact.name} to labors list!")
                        selectedContactForAdd = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text("Confirm & Add Labor", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedContactForAdd = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

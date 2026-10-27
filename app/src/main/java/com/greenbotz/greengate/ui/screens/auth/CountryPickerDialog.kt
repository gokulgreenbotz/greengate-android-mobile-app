package com.greenbotz.greengate.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.greenbotz.greengate.R
import com.greenbotz.greengate.ui.theme.DMSans
import java.util.Locale

@Composable
internal fun CountryPickerDialog(
    selectedIso2: String,
    onSelected: (CountryInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    val allCountries = remember { CountryCatalog.all.sortedBy { it.name } }
    val countries = remember(query) {
        val term = query.trim().lowercase(Locale.ROOT)
        allCountries.filter {
            term.isEmpty() || it.name.lowercase(Locale.ROOT).contains(term) ||
                it.dialCode.contains(term) || it.iso2.lowercase(Locale.ROOT).contains(term)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(query) { listState.scrollToItem(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        fun dismiss() {
            focusManager.clearFocus()
            keyboard?.hide()
            onDismiss()
        }
        // Align the glass sample with the same full-screen artwork as the login page.
        // Only the safe-area container moves for the IME; the backdrop remains fixed.
        val artwork = ImageBitmap.imageResource(R.drawable.login_bg_luxury)
        var viewportSize by remember { mutableStateOf(IntSize.Zero) }
        var viewportOrigin by remember { mutableStateOf(Offset.Zero) }
        Box(
            Modifier.fillMaxSize()
                .onSizeChanged { viewportSize = it }
                .onGloballyPositioned { viewportOrigin = it.positionInRoot() }
        ) {
            Box(Modifier.matchParentSize().pointerInput(onDismiss) {
                detectTapGestures { dismiss() }
            })
            CompositionLocalProvider(
                LocalLoginGlassBackdrop provides LoginGlassBackdrop(artwork, viewportSize, viewportOrigin)
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxSize().systemBarsPadding().imePadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val compact = maxHeight < 420.dp
                    LoginGlassPanel(
                        Modifier.widthIn(max = 480.dp).fillMaxWidth()
                            .heightIn(max = 700.dp).fillMaxHeight(if (compact) 1f else .86f)
                            .pointerInput(Unit) { detectTapGestures { /* Keep blank card taps inside. */ } }
                            .testTag("country_picker_panel")
                    ) {
                        Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 20.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Choose country", color = Ivory, fontFamily = DMSans,
                                    fontSize = 22.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f).semantics { heading() }
                                )
                                IconButton(onClick = ::dismiss, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Outlined.Close, "Close country picker", tint = Muted)
                                }
                            }
                            if (!compact) {
                                Text(
                                    "Search by country name or dial code", color = Muted,
                                    fontFamily = DMSans, fontSize = 12.sp, lineHeight = 18.sp
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            CountrySearchField(
                                query = query, focused = searchFocused,
                                onQueryChange = { query = it },
                                onFocusChange = { searchFocused = it },
                                onSearch = { keyboard?.hide() }
                            )
                            Spacer(Modifier.height(12.dp))
                            if (countries.isEmpty()) {
                                Column(
                                    Modifier.fillMaxWidth().weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Outlined.SearchOff, null, tint = Gold, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.height(10.dp))
                                    Text("No countries found", color = Ivory, fontFamily = DMSans, fontSize = 16.sp)
                                    Text(
                                        "Try a country name or dial code.", color = Muted,
                                        fontFamily = DMSans, fontSize = 12.sp, textAlign = TextAlign.Center
                                    )
                                    TextButton(onClick = { query = "" }) {
                                        Text("Clear search", color = Gold, fontFamily = DMSans)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(countries, key = { it.iso2 }) { country ->
                                        val selected = country.iso2 == selectedIso2
                                        Row(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                                .then(if (selected) Modifier.background(Gold.copy(alpha = .10f)) else Modifier)
                                                .selectable(selected, role = Role.RadioButton) {
                                                    focusManager.clearFocus()
                                                    keyboard?.hide()
                                                    onSelected(country)
                                                }
                                                .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(country.flag, fontSize = 23.sp)
                                            Text(
                                                country.name, color = if (selected) GoldLight else Ivory,
                                                fontFamily = DMSans, fontSize = 14.sp, lineHeight = 20.sp,
                                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                            )
                                            Text(country.dialCode, color = Muted, fontFamily = DMSans, fontSize = 13.sp)
                                            Box(Modifier.padding(start = 8.dp).size(20.dp)) {
                                                if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = Gold)
                                            }
                                        }
                                    }
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
private fun CountrySearchField(
    query: String,
    focused: Boolean,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearch: () -> Unit
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .onFocusChanged { onFocusChange(it.isFocused) }.testTag("login_country_search"),
        singleLine = true,
        textStyle = TextStyle(color = Ivory, fontFamily = DMSans, fontSize = 15.sp),
        cursorBrush = SolidColor(Gold),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Box(Modifier.fillMaxSize()) {
                if (focused) LoginSurfaceGlow(Modifier.matchParentSize(), strength = .12f)
                Row(
                    Modifier.fillMaxSize().loginGlassControl(focused).padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, null, tint = Gold, modifier = Modifier.size(22.dp))
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        if (query.isEmpty()) Text("Search countries", color = Muted, fontFamily = DMSans, fontSize = 15.sp)
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.Close, "Clear country search", tint = Muted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    )
}

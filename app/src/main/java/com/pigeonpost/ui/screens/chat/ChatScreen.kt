package com.pigeonpost.ui.screens.chat

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pigeonpost.data.model.Message
import com.pigeonpost.data.model.MessageStatus
import com.pigeonpost.ui.animation.PigeonDeliveryAnimation
import com.pigeonpost.ui.animation.PigeonFlyingAnimation
import com.pigeonpost.ui.components.ParchmentBackground
import com.pigeonpost.ui.components.parchmentTextFieldColors
import com.pigeonpost.ui.theme.DeepBrown700
import com.pigeonpost.ui.theme.DeepBrown900
import com.pigeonpost.ui.theme.GoldAccent400
import com.pigeonpost.ui.theme.Parchment100
import com.pigeonpost.ui.theme.Parchment300
import com.pigeonpost.ui.theme.WaxSealRed500

/**
 * Chat screen with messages displayed as parchment cards/scroll fragments.
 * Sent messages on right as sealed scrolls, received on left as opened scrolls.
 * Flying messages show animated pigeon icon with ETA.
 * Lost messages show broken pigeon icon with "Lost in transit" text.
 * Input bar styled as quill writing area with pigeon send button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: String,
    onNavigateToMap: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ParchmentBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = uiState.otherUserName,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                        )
                    )
                },
                bottomBar = {
                    MessageInputBar(
                        text = uiState.inputText,
                        onTextChange = viewModel::updateInput,
                        onSend = viewModel::sendMessage,
                        onAttachmentSelected = { uri ->
                            val fileName = getFileName(context, uri) ?: "attachment"
                            viewModel.setAttachment(uri, fileName)
                        },
                        pendingAttachmentName = uiState.pendingAttachmentName,
                        onClearAttachment = viewModel::clearAttachment,
                        isUploading = uiState.isUploading
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        MessageBubble(
                            message = message,
                            isOwn = message.senderId == uiState.currentUserId,
                            onMapClick = { onNavigateToMap(message.id) },
                            onImageClick = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            onPdfClick = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    setDataAndType(Uri.parse(url), "application/pdf")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // Pigeon delivery animation overlay
        uiState.deliveryAnimationMessage?.let { message ->
            PigeonDeliveryAnimation(
                messageText = message.content,
                visible = true,
                onDismiss = viewModel::dismissDeliveryAnimation
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    onMapClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onPdfClick: (String) -> Unit
) {
    val alignment = if (isOwn) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOwn) {
                    Parchment300.copy(alpha = 0.95f)
                } else {
                    Parchment100.copy(alpha = 0.95f)
                }
            ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isOwn) 12.dp else 2.dp,
                bottomEnd = if (isOwn) 2.dp else 12.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Attachment display
                if (message.attachmentUrl != null) {
                    AttachmentContent(
                        url = message.attachmentUrl,
                        onImageClick = onImageClick,
                        onPdfClick = onPdfClick
                    )
                    if (message.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepBrown900
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Status indicator
                when (message.status) {
                    MessageStatus.FLYING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onMapClick)
                        ) {
                            PigeonFlyingAnimation(
                                size = 20.dp,
                                tint = GoldAccent400
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pigeon en route...",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepBrown700,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "\uD83D\uDDFA",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    MessageStatus.DELIVERED -> {
                        Text(
                            text = "\u2714 Delivered by faithful pigeon",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepBrown700.copy(alpha = 0.9f)
                        )
                    }
                    MessageStatus.LOST -> {
                        Text(
                            text = "\u2620 Lost in transit - pigeon perished",
                            style = MaterialTheme.typography.labelSmall,
                            color = WaxSealRed500,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentContent(
    url: String,
    onImageClick: (String) -> Unit,
    onPdfClick: (String) -> Unit
) {
    val isImage = url.contains(".jpg", ignoreCase = true) ||
        url.contains(".jpeg", ignoreCase = true) ||
        url.contains(".png", ignoreCase = true) ||
        url.contains(".gif", ignoreCase = true) ||
        url.contains(".webp", ignoreCase = true) ||
        url.contains("image", ignoreCase = true)

    val isPdf = url.contains(".pdf", ignoreCase = true)

    when {
        isImage -> {
            AsyncImage(
                model = url,
                contentDescription = "Attached image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick(url) },
                contentScale = ContentScale.Crop
            )
        }
        isPdf -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldAccent400.copy(alpha = 0.15f))
                    .clickable { onPdfClick(url) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\uD83D\uDCDC",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scroll attached",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepBrown900,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        else -> {
            // Generic attachment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldAccent400.copy(alpha = 0.15f))
                    .clickable { onImageClick(url) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\uD83D\uDCCE",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Attachment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepBrown900,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentSelected: (Uri) -> Unit,
    pendingAttachmentName: String?,
    onClearAttachment: () -> Unit,
    isUploading: Boolean
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAttachmentSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        // Pending attachment indicator
        if (pendingAttachmentName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldAccent400.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\uD83D\uDCDC",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pendingAttachmentName,
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepBrown700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClearAttachment,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = DeepBrown700,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment button
            IconButton(
                onClick = { launcher.launch("*/*") },
                enabled = !isUploading
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach a scroll",
                    tint = GoldAccent400
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Inscribe thy message...",
                        fontStyle = FontStyle.Italic
                    )
                },
                // Explicit dark-ink text style so what the user types is always visible
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = DeepBrown900),
                colors = parchmentTextFieldColors(),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button (pigeon release)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = !isUploading, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Release Pigeon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Retrieves the file name from a content URI using the ContentResolver.
 */
private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                name = it.getString(index)
            }
        }
    }
    return name
}

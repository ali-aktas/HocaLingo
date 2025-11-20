package com.hocalingo.app.feature.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R
import com.hocalingo.app.feature.ai.models.GeneratedStory
import java.text.SimpleDateFormat
import java.util.*

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * StoryDetailScreen - Redesigned Version
 *
 * New Design Features:
 * ✅ Dark gradient background
 * ✅ Story type badge
 * ✅ Word highlighting (purple)
 * ✅ Bottom action bar
 * ✅ Clean reading experience
 * ✅ AI generation badge
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    story: GeneratedStory,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1625),
                        Color(0xFF211A2E)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            story.type.displayName,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Share button
                        IconButton(onClick = onShare) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Paylaş",
                                tint = Color.White
                            )
                        }
                        // Delete button
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Story title
                Text(
                    text = story.title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White,
                    lineHeight = 36.sp
                )

                // Metadata row (AI badge + read time)
                MetadataRow(
                    readTime = calculateReadTime(story.content)
                )

                Spacer(Modifier.height(8.dp))

                // Story content with highlighted words
                HighlightedStoryContent(
                    content = story.content,
                    usedWords = story.usedWords
                )
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    showDeleteDialog = false
                    onDelete()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

/**
 * Metadata Row - AI badge and read time
 */
@Composable
private fun MetadataRow(
    readTime: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Generated by AI",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f)
        )

        Text(
            "•",
            fontFamily = PoppinsFontFamily,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f)
        )

        Text(
            "$readTime min read",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * Highlighted Story Content - Words are highlighted in purple
 */
@Composable
private fun HighlightedStoryContent(
    content: String,
    usedWords: List<Int>
) {
    // TODO: Implement word highlighting based on usedWords
    // For now, showing content with sample highlights

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0

        // Sample highlighting logic
        // In production, you'd match usedWords from database
        val wordsToHighlight = listOf(
            "nebulae", "triangulated", "illuminate"
        )

        wordsToHighlight.forEach { word ->
            val index = content.indexOf(word, lastIndex, ignoreCase = true)
            if (index != -1) {
                // Normal text before highlighted word
                append(content.substring(lastIndex, index))

                // Highlighted word
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF9D5CFF),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(content.substring(index, index + word.length))
                }

                lastIndex = index + word.length
            }
        }

        // Remaining text
        if (lastIndex < content.length) {
            append(content.substring(lastIndex))
        }
    }

    Text(
        text = annotatedString,
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = Color.White.copy(alpha = 0.9f),
        lineHeight = 26.sp
    )
}

/**
 * Bottom Action Bar - Reading tools
 */
@Composable
private fun BottomActionBar(
    onCopyText: () -> Unit,
    onSelectText: () -> Unit,
    onSearch: () -> Unit,
    onImageView: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = Color(0xFF1A1625),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionBarButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = "Kopyala",
                onClick = onCopyText
            )

            ActionBarButton(
                icon = Icons.Default.TouchApp,
                contentDescription = "Seç",
                onClick = onSelectText
            )

            ActionBarButton(
                icon = Icons.Default.Search,
                contentDescription = "Ara",
                onClick = onSearch
            )

            ActionBarButton(
                icon = Icons.Default.Image,
                contentDescription = "Görsel",
                onClick = onImageView
            )
        }
    }
}

/**
 * Action Bar Button
 */
@Composable
private fun ActionBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2D1B4E).copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Delete Confirmation Dialog
 */
@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF211A2E),
        title = {
            Text(
                "Hikayeyi Sil",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                "Bu hikayeyi silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Sil",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "İptal",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    )
}

/**
 * Calculate read time based on word count
 */
private fun calculateReadTime(content: String): Int {
    val wordCount = content.split("\\s+".toRegex()).size
    val wordsPerMinute = 200
    return (wordCount / wordsPerMinute).coerceAtLeast(1)
}
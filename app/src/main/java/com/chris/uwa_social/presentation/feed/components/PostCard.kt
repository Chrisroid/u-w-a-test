package com.chris.uwa_social.presentation.feed.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chris.uwa_social.domain.model.Post
import com.chris.uwa_social.presentation.feed.util.TimeFormatter

private val UwaEmerald = Color(0xFF22A447)
private val UwaLeafGreen = Color(0xFF38B449)
private val HeartRed = Color(0xFFFA3E3E)

enum class FacebookReaction(
    val emoji: String,
    val label: String,
    val color: Color
) {
    LIKE("👍", "Like", Color(0xFF22A447)),
    LOVE("❤️", "Love", Color(0xFFE91E63)),
    CARE("🥰", "Care", Color(0xFF38B449)),
    HAHA("😆", "Haha", Color(0xFFF5A623)),
    WOW("😮", "Wow", Color(0xFFF5A623)),
    SAD("😢", "Sad", Color(0xFF5C6BC0)),
    ANGRY("😡", "Angry", Color(0xFFD32F2F))
}

@Composable
fun PostCard(
    post: Post,
    onLikeClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedReaction: FacebookReaction? = null,
    onReactionSelected: (FacebookReaction) -> Unit = {},
    onCommentClicked: (String) -> Unit = {},
    onShareClicked: (Post) -> Unit = {}
) {
    var showReactions by remember { mutableStateOf(false) }
    val effectiveReaction = if (post.isLiked) (selectedReaction ?: FacebookReaction.LIKE) else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // 1. Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${post.user.name} profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.user.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val relativeTime = remember(post.createdAt) {
                            TimeFormatter.formatRelativeTime(post.createdAt)
                        }
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!post.location.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = post.location,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = "Public",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 2. Post Text
            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            // 3. Full-Bleed Attached Media
            if (!post.mediaUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post image attached by ${post.user.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }

            // 4. Social Proof Metrics Summary Row
            if (post.likesCount > 0 || post.commentsCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Reactions Badge & Count
                    if (post.likesCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (post.likesCount == 1) {
                                val singleReaction = if (post.isLiked) (effectiveReaction ?: FacebookReaction.LIKE) else FacebookReaction.LIKE
                                ReactionBadge(reaction = singleReaction)
                            } else {
                                ReactionBadge(reaction = FacebookReaction.LIKE)
                                if (post.isLiked && effectiveReaction != null && effectiveReaction != FacebookReaction.LIKE) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    ReactionBadge(reaction = effectiveReaction)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${post.likesCount}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Right: Comments count
                    if (post.commentsCount > 0) {
                        Text(
                            text = if (post.commentsCount == 1) "1 comment" else "${post.commentsCount} comments",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onCommentClicked(post.id) }
                        )
                    }
                }
            }

            // 5. Hairline Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 6. Facebook 3-Action Footer Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button Slot with Floating Reaction Pill
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (showReactions) {
                        ReactionBar(
                            onReactionSelected = { reaction ->
                                showReactions = false
                                onReactionSelected(reaction)
                            },
                            onDismissRequest = { showReactions = false }
                        )
                    }

                    val isLiked = post.isLiked
                    val activeReaction = effectiveReaction

                    FacebookActionButton(
                        icon = if (activeReaction == null) Icons.Outlined.ThumbUp else if (activeReaction == FacebookReaction.LIKE) Icons.Filled.ThumbUp else null,
                        emoji = if (activeReaction != null && activeReaction != FacebookReaction.LIKE) activeReaction.emoji else null,
                        label = activeReaction?.label ?: "Like",
                        contentDescription = if (isLiked) "Unlike post" else "Like post",
                        iconColor = activeReaction?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        textColor = activeReaction?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            onLikeClicked(post.id)
                        },
                        onLongClick = {
                            showReactions = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Comment Button
                FacebookActionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = "Comment",
                    contentDescription = "View comments",
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onCommentClicked(post.id) },
                    modifier = Modifier.weight(1f)
                )

                // Share Button
                FacebookActionButton(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    contentDescription = "Share post",
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onShareClicked(post) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReactionBar(
    onReactionSelected: (FacebookReaction) -> Unit,
    onDismissRequest: () -> Unit
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x = 10, y = -110),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(0.5.dp, Color(0xFFE2E4E8))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FacebookReaction.entries.forEach { reaction ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReactionSelected(reaction) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = reaction.emoji,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FacebookActionButton(
    label: String,
    contentDescription: String,
    iconColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 16.sp)
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = textColor
        )
    }
}

@Composable
private fun ReactionBadge(
    reaction: FacebookReaction,
    modifier: Modifier = Modifier
) {
    when (reaction) {
        FacebookReaction.LIKE -> {
            Box(
                modifier = modifier
                    .size(18.dp)
                    .background(UwaEmerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ThumbUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        FacebookReaction.LOVE -> {
            Box(
                modifier = modifier
                    .size(18.dp)
                    .background(HeartRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        else -> {
            Box(
                modifier = modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = reaction.emoji, fontSize = 13.sp)
            }
        }
    }
}


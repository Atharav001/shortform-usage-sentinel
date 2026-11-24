package com.example.scrollersdashboard.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.ui.theme.AppGradients
import com.example.scrollersdashboard.ui.theme.Gray400
import com.example.scrollersdashboard.ui.theme.Gray500
import com.example.scrollersdashboard.ui.theme.OrbAmber
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val RingBounceEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private val GoalsAccent = Color(0xFFF59E0B)
private val GoalsEmerald = Color(0xFF34D399)
private val GoalsTeal = Color(0xFF10B981)
private val EmeraldTealGradient = AppGradients.HabitsAccent
private val HoverEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

@Composable
fun GlassGoalsInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassHabitTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            onDone = onAdd,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        GlassAddButton(onClick = onAdd)
    }
}

@Composable
fun GlassHabitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = tween(200, easing = HoverEasing),
        label = "inputScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) GoalsAccent else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(200),
        label = "inputBorder"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFocused) Color.White.copy(alpha = 0.07f)
                    else Color.White.copy(alpha = 0.04f)
                )
                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                .drawBehind {
                    if (isFocused) {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GoalsAccent.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.maxDimension
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(GoalsAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 16.sp
                            )
                        }
                        AnimatedContent(
                            targetState = value.length,
                            transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(80)) },
                            label = "charFade"
                        ) { _ ->
                            inner()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun GlassAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isHovered -> 1.1f
            else -> 1f
        },
        animationSpec = tween(if (isPressed) 150 else 200, easing = HoverEasing),
        label = "addBtnScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isHovered) 12f else 8f,
        animationSpec = tween(200, easing = HoverEasing),
        label = "addBtnShadow"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation.dp, CircleShape, spotColor = GoalsEmerald.copy(alpha = 0.4f))
            .clip(CircleShape)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            }
            .background(EmeraldTealGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableGoalListItem(
    title: String,
    isCompleted: Boolean,
    accentGradient: Brush,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPressLift: (Boolean) -> Unit = {}
) {
    var isRemoving by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                isRemoving = true
                true
            } else false
        }
    )

    LaunchedEffect(isRemoving) {
        if (isRemoving) {
            delay(300)
            onDelete()
        }
    }

    val removeScaleY by animateFloatAsState(
        targetValue = if (isRemoving) 0f else 1f,
        animationSpec = tween(300),
        label = "removeScale"
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .graphicsLayer {
                scaleY = removeScaleY
                alpha = removeScaleY
            },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val progress = dismissState.progress
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppGradients.SoftDelete)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = progress.coerceIn(0f, 1f)),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        GlassGoalListItem(
            title = title,
            isCompleted = isCompleted,
            accentGradient = accentGradient,
            onToggle = onToggle,
            onLongPressLift = onLongPressLift
        )
    }
}

@Composable
fun GlassGoalListItem(
    title: String,
    isCompleted: Boolean,
    accentGradient: Brush,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPressLift: (Boolean) -> Unit = {}
) {
    val view = LocalView.current
    var wasCompleted by remember { mutableStateOf(isCompleted) }
    var showBurst by remember { mutableStateOf(false) }
    val itemScale = remember { Animatable(1f) }
    var isLifted by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(isCompleted) {
        if (isCompleted && !wasCompleted) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            showBurst = true
            itemScale.snapTo(1f)
            itemScale.animateTo(1.05f, tween(200, easing = RingBounceEasing))
            itemScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
            delay(600)
            showBurst = false
        }
        wasCompleted = isCompleted
    }

    val liftScale by animateFloatAsState(
        targetValue = if (isLifted) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "liftScale"
    )
    val shadowElevation by animateFloatAsState(
        targetValue = if (isLifted) 16f else 4f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "itemShadow"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isCompleted) GoalsEmerald.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f),
        animationSpec = tween(300),
        label = "itemBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .graphicsLayer {
                scaleX = itemScale.value * liftScale
                scaleY = itemScale.value * liftScale
            }
            .shadow(shadowElevation.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.25f))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isLifted = true
                        onLongPressLift(true)
                    },
                    onDragEnd = {
                        isLifted = false
                        dragOffset = 0f
                        onLongPressLift(false)
                    },
                    onDragCancel = {
                        isLifted = false
                        dragOffset = 0f
                        onLongPressLift(false)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .clickable { onToggle() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AnimatedGlassCheckbox(isChecked = isCompleted, gradient = accentGradient)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    color = if (isCompleted) Color.White.copy(alpha = 0.45f) else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
            }

            if (showBurst) {
                CompletionParticleBurst(
                    modifier = Modifier.fillMaxSize(),
                    color = GoalsEmerald
                )
            }
        }
    }
}

@Composable
fun AnimatedGlassCheckbox(isChecked: Boolean, gradient: Brush) {
    val fillProgress by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = tween(300, easing = HoverEasing),
        label = "checkFill"
    )
    val checkDraw by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = tween(200, delayMillis = 100),
        label = "checkDraw"
    )

    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 2.dp.toPx()
            if (fillProgress < 1f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = size.minDimension / 2f - strokeW,
                    style = Stroke(width = strokeW)
                )
            }
            if (fillProgress > 0.01f) {
                drawCircle(
                    brush = gradient,
                    radius = (size.minDimension / 2f - strokeW) * fillProgress
                )
            }
            if (checkDraw > 0f) {
                val w = size.width
                val h = size.height
                val pathStart = Offset(w * 0.28f, h * 0.52f)
                val pathMid = Offset(w * 0.42f, h * 0.66f)
                val pathEnd = Offset(w * 0.72f, h * 0.34f)
                val t = checkDraw
                drawLine(
                    color = Color.White,
                    start = pathStart,
                    end = Offset(
                        pathStart.x + (pathMid.x - pathStart.x) * t,
                        pathStart.y + (pathMid.y - pathStart.y) * t
                    ),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                if (t > 0.5f) {
                    val t2 = ((t - 0.5f) * 2f).coerceIn(0f, 1f)
                    drawLine(
                        color = Color.White,
                        start = pathMid,
                        end = Offset(
                            pathMid.x + (pathEnd.x - pathMid.x) * t2,
                            pathMid.y + (pathEnd.y - pathMid.y) * t2
                        ),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionParticleBurst(modifier: Modifier, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(600))
    }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        repeat(12) { i ->
            val angle = Math.toRadians((i * 30).toDouble())
            val t = progress.value
            val radius = size.minDimension * 0.35f * t
            drawCircle(
                color = color.copy(alpha = (1f - t) * 0.85f),
                radius = 3f,
                center = Offset(
                    cx + (cos(angle) * radius).toFloat(),
                    cy + (sin(angle) * radius).toFloat()
                )
            )
        }
    }
}

@Composable
fun GoalsProgressCard(
    progress: Float,
    countLabel: String,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val percent = (progress * 100).toInt()
    val isComplete = progress >= 1f && percent > 0

    var showCelebration by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var previousComplete by remember { mutableStateOf(false) }

    LaunchedEffect(isComplete) {
        if (isComplete && !previousComplete) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showCelebration = true
            showFlash = true
            delay(200)
            showFlash = false
            delay(2000)
            showCelebration = false
        }
        previousComplete = isComplete
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessLow
        ),
        label = "goalProgress"
    )

    val trophyScale = remember { Animatable(0f) }
    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            trophyScale.snapTo(0f)
            trophyScale.animateTo(1.2f, tween(300, easing = RingBounceEasing))
            trophyScale.animateTo(1f, tween(200))
        } else {
            trophyScale.snapTo(0f)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (showFlash) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.36f))
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(24.dp)
                        } else Modifier
                    )
                    .background(Color.White.copy(alpha = 0.06f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showCelebration) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = trophyScale.value
                                scaleY = trophyScale.value
                            }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    "TODAY'S COMPLETION",
                    color = Gray500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(contentAlignment = Alignment.Center) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Text(
                            text = "$percent%",
                            style = TextStyle(
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                brush = EmeraldTealGradient
                            ),
                            modifier = Modifier.blur(16.dp).graphicsLayer { alpha = 0.3f }
                        )
                    }
                    Text(
                        text = "$percent%",
                        style = TextStyle(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            brush = EmeraldTealGradient
                        )
                    )
                }

                Text(countLabel, color = Gray400, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(4.dp))
                            .drawBehind {
                                drawRect(brush = EmeraldTealGradient)
                            }
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && animatedProgress > 0f) {
                                    Modifier.blur(4.dp)
                                } else Modifier
                            )
                    )
                }
            }

            if (showCelebration) {
                ConfettiOverlay(modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun ConfettiOverlay(modifier: Modifier) {
    val particles = remember {
        List(50) {
            ConfettiParticle(
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 0.5f + 0.5f,
                color = listOf(GoalsEmerald, GoalsTeal, Color(0xFFFBBF24), Color.White).random(),
                size = Random.nextFloat() * 4f + 3f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2000))
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val t = progress.value
        particles.forEach { p ->
            val angleRad = Math.toRadians(p.angle.toDouble())
            val dist = size.minDimension * 0.55f * t * p.speed
            val x = cx + (cos(angleRad) * dist).toFloat()
            val y = cy + (sin(angleRad) * dist).toFloat() - (t * 40f)
            drawCircle(
                color = p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val color: Color,
    val size: Float
)

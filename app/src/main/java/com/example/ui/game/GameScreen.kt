package com.example.ui.game

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.sin
import kotlin.random.Random

// Helper to convert hexadecimal hex strings to jetpack compose Color
fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.White
    }
}

@Composable
fun TopHud(
    scoreProvider: () -> Int,
    highScore: Int,
    livesProvider: () -> Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Score & Highscore Panel
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.SportsScore,
                    contentDescription = "Skor",
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SKOR: ${scoreProvider()}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "EN YÜKSEK: $highScore",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        // Hearts / Lives Panel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val lives = livesProvider()
            repeat(5) { index ->
                val isFilled = index < lives
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Can",
                    tint = if (isFilled) Color(0xFFF43F5E) else Color(0xFF475569),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(horizontal = 1.dp)
                )
            }
        }
    }
}

@Composable
fun StatusRow(
    speedProvider: () -> Float,
    turboTimeProvider: () -> Int,
    shieldTimeProvider: () -> Int,
    slowTimeProvider: () -> Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val speed = speedProvider()
        val turboTime = turboTimeProvider()
        val shieldTime = shieldTimeProvider()
        val slowTime = slowTimeProvider()

        // Speed Meter
        val displaySpeed = if (turboTime > 0) speed * 1000f * 1.8f else speed * 1000f
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = "Hız",
                tint = if (turboTime > 0) Color(0xFFF39C12) else Color(0xFF00E676),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${displaySpeed.toInt()} km/h",
                color = if (turboTime > 0) Color(0xFFF39C12) else Color(0xFF00E676),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Active effects list
        Row(horizontalArrangement = Arrangement.End) {
            if (shieldTime > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(Color(0xFF00BCD4).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00BCD4), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🛡️ ${shieldTime / 60}s",
                        color = Color(0xFFE0F7FA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (slowTime > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(Color(0xFF9C27B0).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF9C27B0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🌀 ${slowTime / 60}s",
                        color = Color(0xFFF3E5F5),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TurboBar(
    turboChargeProvider: () -> Float,
    turboTimeProvider: () -> Int
) {
    val turboChargeVal = turboChargeProvider()
    val turboTime = turboTimeProvider()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (turboTime > 0) "⚡ TURBO AKTİF! ⚡" else "TURBO GÜCÜ",
                color = if (turboTime > 0) Color(0xFFF39C12) else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "${turboChargeVal.toInt()}%",
                color = if (turboTime > 0) Color(0xFFF39C12) else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { turboChargeVal / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (turboTime > 0) Color(0xFFFFFF00) else Color(0xFFF39C12),
            trackColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    
    val isStarted = viewModel.isGameStarted.value
    val isGameOver = viewModel.isGameOver.value
    val isPaused = viewModel.isPaused.value

    // Pulsing animations for powerups/shield
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A) // Sleek Slate-Dark theme
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP STATS HEADER (HUD)
            TopHud(
                scoreProvider = { viewModel.score.value },
                highScore = stats.highScore,
                livesProvider = { viewModel.lives.value }
            )

            // 2. POWER-UP ACTIVE STATUSES & SPEED INDICATOR
            StatusRow(
                speedProvider = { viewModel.speed.value },
                turboTimeProvider = { viewModel.turboTimeRemaining.value },
                shieldTimeProvider = { viewModel.shieldTimeRemaining.value },
                slowTimeProvider = { viewModel.slowTimeRemaining.value }
            )

            // Turbo Progress Bar
            TurboBar(
                turboChargeProvider = { viewModel.turboCharge.value },
                turboTimeProvider = { viewModel.turboTimeRemaining.value }
            )

            // 3. GAME PLAYFIELD (CANVAS)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val density = LocalDensity.current
                val emojiPaint = remember(density) {
                    Paint().apply {
                        textSize = 14f * density.density
                        textAlign = Paint.Align.CENTER
                    }
                }
                val coinPaint = remember(density) {
                    Paint().apply {
                        textSize = 11f * density.density
                        color = android.graphics.Color.parseColor("#854D0E")
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(stats.controlMode) {
                            if (stats.controlMode == 0 || stats.controlMode == 2) {
                                // For mode 0 (Touch) we use tap. For mode 2 (Buttons), we also allow tap as a fallback or they can just use buttons.
                                detectTapGestures { offset ->
                                    if (isStarted && !isGameOver && !isPaused) {
                                        if (offset.x < size.width / 2) {
                                            viewModel.moveLeft()
                                        } else {
                                            viewModel.moveRight()
                                        }
                                    }
                                }
                            } else if (stats.controlMode == 1) {
                                detectDragGestures(
                                    onDragEnd = { },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (isStarted && !isGameOver && !isPaused) {
                                            if (dragAmount.x < -15) viewModel.moveLeft()
                                            else if (dragAmount.x > 15) viewModel.moveRight()
                                        }
                                    }
                                )
                            }
                        }
                        .testTag("game_canvas")
                ) {
                    val w = size.width
                    val h = size.height

                    val screenShake = viewModel.screenShakeTicks.value
                    val scrollOffset = viewModel.roadOffset.value
                    val slowTime = viewModel.slowTimeRemaining.value
                    val turboTime = viewModel.turboTimeRemaining.value
                    val shieldTime = viewModel.shieldTimeRemaining.value
                    val hitFlash = viewModel.hitFlashTicks.value
                    val currentX = viewModel.playerX.value
                    val pulseScale = pulseScaleState.value

                    val activeEnemies = viewModel.enemies
                    val activePowerUps = viewModel.powerups
                    val activeParticles = viewModel.particles

                    // Handle Screen Shake
                    val shakeX = if (screenShake > 0) Random.nextFloat() * 16f - 8f else 0f
                    val shakeY = if (screenShake > 0) Random.nextFloat() * 16f - 8f else 0f

                    withTransform({
                        translate(left = shakeX, top = shakeY)
                    }) {
                        // A. Draw Road surface
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            ),
                            size = size
                        )

                        // B. Draw lanes & lines manually for ultra-smooth rendering without allocating PathEffect/FloatArray
                        val laneWidth = w / 4f
                        val segmentLength = 45f
                        val spaceLength = 35f
                        val totalLength = segmentLength + spaceLength
                        val startY = -totalLength + (scrollOffset % totalLength)

                        for (i in 1..3) {
                            val lx = i * laneWidth
                            var currY = startY
                            while (currY < h) {
                                drawLine(
                                    color = Color(0xFF334155),
                                    start = Offset(lx, currY),
                                    end = Offset(lx, minOf(currY + segmentLength, h)),
                                    strokeWidth = 2.dp.toPx()
                                )
                                currY += totalLength
                            }
                        }

                        // Draw outer glowing borders
                        drawLine(
                            color = Color(0xFF475569),
                            start = Offset(4f, 0f),
                            end = Offset(4f, h),
                            strokeWidth = 4.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFF475569),
                            start = Offset(w - 4f, 0f),
                            end = Offset(w - 4f, h),
                            strokeWidth = 4.dp.toPx()
                        )

                        // C. Draw Slow-motion overlay tint
                        if (slowTime > 0) {
                            drawRect(
                                color = Color(0xFF9C27B0).copy(alpha = 0.08f),
                                size = size
                            )
                        }

                        // D. Draw PowerUps
                        activePowerUps.forEach { pu ->
                            val puX = (pu.lane + 0.5f) / 4f * w
                            val puY = pu.y * h
                            val radius = 16.dp.toPx() * if (pu.type == PowerUpType.SHIELD) pulseScale else 1f

                            // Draw glowing aura
                            drawCircle(
                                color = parseHexColor(pu.type.colorHex).copy(alpha = 0.35f),
                                radius = radius * 1.3f,
                                center = Offset(puX, puY)
                            )

                            // Draw inner solid circle
                            drawCircle(
                                color = parseHexColor(pu.type.colorHex),
                                radius = radius,
                                center = Offset(puX, puY)
                            )

                            // Draw label emoji cleanly
                            drawContext.canvas.nativeCanvas.drawText(
                                pu.type.label,
                                puX,
                                puY + 6.dp.toPx(),
                                emojiPaint
                            )
                        }

                        // DD. Draw Coins
                        val activeCoins = viewModel.coinsList
                        activeCoins.forEach { coin ->
                            val cX = (coin.lane + 0.5f) / 4f * w
                            val cY = coin.y * h
                            val radius = 10.dp.toPx()

                            // Outer gold glow
                            drawCircle(
                                color = Color(0xFFFACC15).copy(alpha = 0.4f),
                                radius = radius * 1.4f,
                                center = Offset(cX, cY)
                            )

                            // Inner gold coin
                            drawCircle(
                                color = Color(0xFFFACC15),
                                radius = radius,
                                center = Offset(cX, cY)
                            )

                            // Coin border
                            drawCircle(
                                color = Color(0xFFCA8A04),
                                radius = radius,
                                center = Offset(cX, cY),
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            // Draw "¢" symbol in the center
                            drawContext.canvas.nativeCanvas.drawText(
                                "¢",
                                cX,
                                cY + 4.dp.toPx(),
                                coinPaint
                            )
                        }

                        // E. Draw Enemies
                        activeEnemies.forEach { enemy ->
                            val eX = (enemy.lane + 0.5f) / 4f * w
                            val eY = enemy.y * h
                            val eW = 0.08f * w
                            val eH = 0.12f * h

                            val eColor = parseHexColor(enemy.colorHex)

                            // Wheels
                            drawRoundRect(
                                color = Color(0xFF020617),
                                topLeft = Offset(eX - eW/2 - 4f, eY - eH/2 + 8f),
                                size = Size(8f, 16f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFF020617),
                                topLeft = Offset(eX + eW/2 - 4f, eY - eH/2 + 8f),
                                size = Size(8f, 16f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFF020617),
                                topLeft = Offset(eX - eW/2 - 4f, eY + eH/2 - 24f),
                                size = Size(8f, 16f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFF020617),
                                topLeft = Offset(eX + eW/2 - 4f, eY + eH/2 - 24f),
                                size = Size(8f, 16f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )

                            // Main Chassis
                            drawRoundRect(
                                color = eColor,
                                topLeft = Offset(eX - eW/2, eY - eH/2),
                                size = Size(eW, eH),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            // Windshield Window (positioned closer to the front/top)
                            drawRect(
                                color = Color(0x99FFFFFF),
                                topLeft = Offset(eX - eW/2 + 6f, eY - eH/4),
                                size = Size(eW - 12f, eH / 4.5f)
                            )

                            // Rear Spoiler (positioned at the bottom/back)
                            drawRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(eX - eW/2 - 2f, eY + eH/2 - 6f),
                                size = Size(eW + 4f, 6f)
                            )

                            // Red Taillights at the back (since we are overtaking them from behind)
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 3.5f,
                                center = Offset(eX - eW/3, eY + eH/2 - 4f)
                            )
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 3.5f,
                                center = Offset(eX + eW/3, eY + eH/2 - 4f)
                            )
                        }

                        // F. Draw Particles
                        activeParticles.forEach { p ->
                            drawCircle(
                                color = parseHexColor(p.colorHex).copy(alpha = p.alpha),
                                radius = p.size,
                                center = Offset(p.x * w, p.y * h)
                            )
                        }

                        // G. Draw Player Car
                        val showPlayer = hitFlash == 0 || (hitFlash / 5) % 2 == 0
                        if (showPlayer) {
                            val pX = currentX * w
                            val pY = 0.82f * h
                            val pW = 0.08f * w
                            val pH = 0.13f * h

                            val selectedCarIdx = stats.selectedCarIndex
                            val activeCar = CAR_MODELS.getOrElse(selectedCarIdx) { CAR_MODELS[0] }
                            val baseCarColor = parseHexColor(activeCar.colorHex)
                            val pColor = if (turboTime > 0) Color(0xFFFFFF00) else baseCarColor // Neon Underglow and Chassis color

                            // Wheels with metallic silver hubcaps/rims
                            val wheelColor = Color(0xFF1E293B) // Distinct charcoal color
                            val rimColor = Color(0xFFF1F5F9)   // Silver metallic rim

                            // Front Left Wheel
                            drawRoundRect(
                                color = wheelColor,
                                topLeft = Offset(pX - pW/2 - 6f, pY - pH/2 + 8f),
                                size = Size(8f, 18f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawCircle(
                                color = rimColor,
                                radius = 2.5f,
                                center = Offset(pX - pW/2 - 2f, pY - pH/2 + 17f)
                            )

                            // Front Right Wheel
                            drawRoundRect(
                                color = wheelColor,
                                topLeft = Offset(pX + pW/2 - 2f, pY - pH/2 + 8f),
                                size = Size(8f, 18f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawCircle(
                                color = rimColor,
                                radius = 2.5f,
                                center = Offset(pX + pW/2 + 2f, pY - pH/2 + 17f)
                            )

                            // Rear Left Wheel
                            drawRoundRect(
                                color = wheelColor,
                                topLeft = Offset(pX - pW/2 - 6f, pY + pH/2 - 26f),
                                size = Size(8f, 18f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawCircle(
                                color = rimColor,
                                radius = 2.5f,
                                center = Offset(pX - pW/2 - 2f, pY + pH/2 - 17f)
                            )

                            // Rear Right Wheel
                            drawRoundRect(
                                color = wheelColor,
                                topLeft = Offset(pX + pW/2 - 2f, pY + pH/2 - 26f),
                                size = Size(8f, 18f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawCircle(
                                color = rimColor,
                                radius = 2.5f,
                                center = Offset(pX + pW/2 + 2f, pY + pH/2 - 17f)
                            )

                            // 1. Neon Underglow Effect
                            drawRoundRect(
                                color = pColor.copy(alpha = 0.35f),
                                topLeft = Offset(pX - pW/2 - 8f, pY - pH/2 - 4f),
                                size = Size(pW + 16f, pH + 8f),
                                cornerRadius = CornerRadius(16f, 16f)
                            )

                            // 2. Headlight Beams shining forward onto the road
                            val beamBrush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (turboTime > 0) Color(0x7FCEF3FF) else Color(0x66A7F3D0)
                                ),
                                startY = pY - pH/2 - 140.dp.toPx(),
                                endY = pY - pH/2
                            )
                            drawRect(
                                brush = beamBrush,
                                topLeft = Offset(pX - pW/2 - 16f, pY - pH/2 - 140.dp.toPx()),
                                size = Size(pW + 32f, 140.dp.toPx())
                            )

                            // Chassis body
                            drawRoundRect(
                                color = pColor,
                                topLeft = Offset(pX - pW/2, pY - pH/2),
                                size = Size(pW, pH),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            // Windshield window pointing forward
                            drawRect(
                                color = Color(0x99FFFFFF),
                                topLeft = Offset(pX - pW/2 + 6f, pY - pH/4),
                                size = Size(pW - 12f, pH / 4f)
                            )

                            // Spoiler
                            drawRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(pX - pW/2 - 4f, pY + pH/2 - 4f),
                                size = Size(pW + 8f, 6f)
                            )

                            // Glowing Headlights
                            drawCircle(
                                color = Color(0xFFF8FAFC),
                                radius = 4.5f,
                                center = Offset(pX - pW/3, pY - pH/2 + 4f)
                            )
                            drawCircle(
                                color = Color(0xFFF8FAFC),
                                radius = 4.5f,
                                center = Offset(pX + pW/3, pY - pH/2 + 4f)
                            )

                            // Rear red brake/tail lights
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 3.5f,
                                center = Offset(pX - pW/3, pY + pH/2 - 4f)
                            )
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 3.5f,
                                center = Offset(pX + pW/3, pY + pH/2 - 4f)
                            )
                            // Tail light flares
                            drawCircle(
                                color = Color(0x55EF4444),
                                radius = 10f,
                                center = Offset(pX - pW/3, pY + pH/2 - 4f)
                            )
                            drawCircle(
                                color = Color(0x55EF4444),
                                radius = 10f,
                                center = Offset(pX + pW/3, pY + pH/2 - 4f)
                            )

                            // Draw Shield Bubble around player
                            if (shieldTime > 0) {
                                val pulseRadius = (pH / 1.35f) * pulseScale
                                drawCircle(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                                    radius = pulseRadius,
                                    center = Offset(pX, pY)
                                )
                                drawCircle(
                                    color = Color(0xFF00E5FF),
                                    radius = pulseRadius,
                                    center = Offset(pX, pY),
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), phase = scrollOffset)
                                    )
                                )
                            }
                        }
                    }
                }

                // G. OVERLAY SCREEN STATES (Not Started / Paused / Game Over)
                if (!isStarted || isPaused || isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xD9090D16)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            when {
                                isGameOver -> {
                                    Text(
                                        text = "🏎️ OYUN BİTTİ!",
                                        color = Color(0xFFEF4444),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "SKORUNUZ",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "${viewModel.score.value}",
                                        color = Color(0xFFFACC15),
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "EN YÜKSEK SKOR: ${stats.highScore}",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.startGame() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(50.dp)
                                            .testTag("restart_button")
                                    ) {
                                        Text("Yeniden Başla", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                isPaused -> {
                                    Text(
                                        text = "⏸️ OYUN DURDURULDU",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "🎮 KONTROL MODU",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        val modes = listOf("Dokun", "Kaydır", "Tuşlar")
                                        modes.forEachIndexed { idx, title ->
                                            val isSel = stats.controlMode == idx
                                            Button(
                                                onClick = { viewModel.setControlMode(idx) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSel) Color(0xFF3B82F6) else Color(0xFF1E293B)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(title, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.togglePause() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(50.dp)
                                            .testTag("resume_button")
                                    ) {
                                        Text("Devam Et", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                else -> { // First Launch / Welcome Screen
                                    Text(
                                        text = "🏎️ ARABA YARIŞI",
                                        color = Color(0xFF00E676),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Rakiplerden kaçın, güçlendiricileri ve altınları topla!",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // COINS BALANCE DISPLAY
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFFFACC15).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("🪙", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PARANIZ: ${stats.coins} ¢",
                                            color = Color(0xFFFACC15),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // GARAJ / CAR SHOP SELECTION
                                    Text(
                                        text = "🚗 GARAJ (ARABA SEÇİMİ)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        items(CAR_MODELS.size) { index ->
                                            val car = CAR_MODELS[index]
                                            val isUnlocked = stats.unlockedCars.split(",").contains(index.toString())
                                            val isSelected = stats.selectedCarIndex == index
                                            
                                            Card(
                                                modifier = Modifier
                                                    .width(170.dp)
                                                    .clickable {
                                                        if (isUnlocked) {
                                                            viewModel.selectCar(index)
                                                        }
                                                    },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                                                ),
                                                border = BorderStroke(
                                                    2.dp,
                                                    if (isSelected) parseHexColor(car.colorHex) else Color(0xFF334155)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    // Car preview icon
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .background(parseHexColor(car.colorHex), RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("🏎️", fontSize = 18.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = car.name,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = car.description,
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 9.sp,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    
                                                    // Stats
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Hız:", color = Color(0xFF64748B), fontSize = 9.sp)
                                                        Text("${(car.speedStat * 1000f).toInt()} km/h", color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Manevra:", color = Color(0xFF64748B), fontSize = 9.sp)
                                                        Text("${(car.handlingStat * 100f).toInt()}/100", color = Color(0xFF3B82F6), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    // Unlock / Select actions
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(parseHexColor(car.colorHex).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                                .border(1.dp, parseHexColor(car.colorHex), RoundedCornerShape(6.dp))
                                                                .padding(vertical = 4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("SEÇİLİ", color = parseHexColor(car.colorHex), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else if (isUnlocked) {
                                                        Button(
                                                            onClick = { viewModel.selectCar(index) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.fillMaxWidth().height(24.dp)
                                                        ) {
                                                            Text("SEÇ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        val canAfford = stats.coins >= car.cost
                                                        Button(
                                                            onClick = { if (canAfford) viewModel.buyCar(index, car.cost) },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (canAfford) Color(0xFFFACC15) else Color(0xFF1E293B)
                                                            ),
                                                            enabled = canAfford,
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.fillMaxWidth().height(24.dp)
                                                        ) {
                                                            val btnText = if (canAfford) "SATIN AL: ${car.cost}" else "${car.cost} ¢"
                                                            Text(
                                                                text = btnText,
                                                                color = if (canAfford) Color(0xFF0F172A) else Color(0xFF475569),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "🎮 KONTROL MODU",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        val modes = listOf("Dokun", "Kaydır", "Tuşlar")
                                        modes.forEachIndexed { idx, title ->
                                            val isSel = stats.controlMode == idx
                                            Button(
                                                onClick = { viewModel.setControlMode(idx) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSel) Color(0xFF3B82F6) else Color(0xFF1E293B)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(title, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.startGame() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(48.dp)
                                            .testTag("start_button")
                                    ) {
                                        Text("Oyunu Başlat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF090D16))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Help instruction text
            if (isStarted && !isPaused && !isGameOver) {
                Text(
                    text = when (stats.controlMode) {
                        0 -> "Yönlendirmek için ekranın sağına veya soluna dokun!"
                        1 -> "Yönlendirmek için sağa veya sola kaydır!"
                        else -> "Yönlendirmek için aşağıdaki butonları kullan!"
                    },
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // 4. ACTION CONTROLLERS (Tactile navigation buttons at bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stats.controlMode == 2) {
                    // Left Button
                    Button(
                        onClick = { viewModel.moveLeft() },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(end = 8.dp)
                            .testTag("control_left"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Sola Git",
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFF00E676)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f).padding(end = 8.dp))
                }

                // Pause/Resume Button
                IconButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(1.dp, Color(0xFF334155), CircleShape)
                        .testTag("control_pause")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (isPaused) "Devam Et" else "Durdur",
                        tint = Color.White
                    )
                }

                if (stats.controlMode == 2) {
                    // Right Button
                    Button(
                        onClick = { viewModel.moveRight() },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(start = 8.dp)
                            .testTag("control_right"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Sağa Git",
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFF00E676)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f).padding(start = 8.dp))
                }
            }
        }
    }
}

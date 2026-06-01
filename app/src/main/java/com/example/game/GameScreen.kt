package com.example.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/**
 * Representing particles generated dynamically on points scored or milestones reached.
 */
data class VisualParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val life: Float,        // Fraction from 1f down to 0f
    val isStar: Boolean = false,
    val rotation: Float = 0f,
    val rotationSpeed: Float = 0f
)

/**
 * A stunning, beautifully-polished Canvas UI rendering the Flappy Bird game.
 * Adheres to professional mobile game styles:
 * - Parallax layered background (Sky, Mountains, Pines, Clouds, Grass, Dirt, Pebbles)
 * - Highly detailed Flappy Bird with multi-feathered flapping wing, expressive anime eye, and cute beak
 * - Glossy green cylindrical metallic pipes with 3D shadow capping details
 * - Score popping scale animation
 * - Camera crash screen shake effect on collision
 * - Score explosion gold stars and sparkly particles
 * - High-end bouncy spring animated Game Over scorecard showing bronze/silver/gold medal accolades
 */
@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel
) {
    val state by viewModel.gameState.collectAsState()

    // Local timing ticker to sync distance traveled
    var distanceTraveled by remember { mutableStateOf(0f) }
    var particles by remember { mutableStateOf(emptyList<VisualParticle>()) }

    // Reset distance on restart
    LaunchedEffect(state.status) {
        if (state.status == GameStatus.READY) {
            distanceTraveled = 0f
            particles = emptyList()
        }
    }

    // High precision game physics update loop driven by Compose UI MonotonicFrameClock
    LaunchedEffect(Unit) {
        var lastTimeNanos = System.nanoTime()
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val elapsedSeconds = ((frameTimeNanos - lastTimeNanos) / 1_000_000_000f)
                val dt = elapsedSeconds.coerceAtMost(0.1f)
                lastTimeNanos = frameTimeNanos
                
                // Update physics
                viewModel.updatePhysics(dt)

                // Accumulate distance during play for parallax
                if (state.status == GameStatus.PLAYING) {
                    distanceTraveled += PipePair.SPEED * dt
                }

                // Update particles ticks
                particles = particles.mapNotNull { p ->
                    val nextLife = p.life - dt * 1.8f // Decay particle life
                    if (nextLife <= 0f) null
                    else {
                        p.copy(
                            x = p.x + p.vx * dt,
                            y = p.y + p.vy * dt + 400f * dt, // Apply gravity pull on particles
                            life = nextLife,
                            rotation = p.rotation + p.rotationSpeed * dt
                        )
                    }
                }
            }
        }
    }

    // Spark star burst trigger whenever point is notched
    var previousScore by remember { mutableStateOf(0) }
    LaunchedEffect(state.score) {
        if (state.score > previousScore) {
            // Trigger beautiful particle explosion around the bird
            val birdX = GameState.BIRD_X + 25f
            val birdY = state.bird.y + 25f
            val newParticles = mutableListOf<VisualParticle>()
            
            // Gold stars & multi-colored sparkles
            repeat(15) {
                val angle = (0..359).random() * (Math.PI / 180f)
                val forceSpeed = (150..400).random().toFloat()
                val size = (12..28).random().toFloat()
                val isGoldenStar = (0..10).random() > 4
                newParticles.add(
                    VisualParticle(
                        x = birdX,
                        y = birdY,
                        vx = Math.cos(angle).toFloat() * forceSpeed,
                        vy = Math.sin(angle).toFloat() * forceSpeed,
                        color = if (isGoldenStar) Color(0xFFFFD700) else listOf(
                            Color(0xFFFF5252), // Red
                            Color(0xFFFF4081), // Pink
                            Color(0xFFE040FB), // Magenta
                            Color(0xFF00E5FF), // Cyan
                            Color(0xFF00E676), // Green
                            Color(0xFFFFEB3B)  // Yellow
                        ).random(),
                        size = size,
                        life = 1.0f,
                        isStar = isGoldenStar,
                        rotation = (0..360).random().toFloat(),
                        rotationSpeed = (-360..360).random().toFloat()
                    )
                )
            }
            particles = particles + newParticles
        }
        previousScore = state.score
    }

    // Pulse animation for Ready state "TAP TO START" message
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ready_pulse"
    )

    // Camera Crash Screen Shake offset triggered on collision
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.status) {
        if (state.status == GameStatus.GAME_OVER) {
            // High-velocity impact vibration decay
            repeat(8) { index ->
                val magnitude = if (index % 2 == 0) 14f - index else -14f + index
                shakeOffset.animateTo(
                    targetValue = magnitude,
                    animationSpec = keyframes { durationMillis = 40 }
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(80))
        }
    }

    // Dynamic Score popping scale effect
    val scoreScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(state.score) {
        if (state.score > 0) {
            scoreScaleAnim.animateTo(1.4f, animationSpec = tween(90, easing = FastOutSlowInEasing))
            scoreScaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // Game over card entrance glide bounce animation
    val cardTranslateY by animateFloatAsState(
        targetValue = if (state.status == GameStatus.GAME_OVER) 0f else 800f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_slide"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    viewModel.onScreenTap()
                }
            }
            .graphicsLayer {
                // Apply physics screenshake displacement
                translationX = shakeOffset.value
                translationY = shakeOffset.value
            }
            .testTag("game_field_container"),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Main game painting canvas
        Canvas(modifier = Modifier.fillMaxSize().testTag("game_canvas")) {
            val canvasW = size.width
            val canvasH = size.height

            val scaleX = canvasW / GameState.VIRTUAL_WIDTH
            val scaleY = canvasH / GameState.VIRTUAL_HEIGHT

            // Use Transformation matrix to convert all virtual coordinate operations seamlessly to native resolution densities
            withTransform({
                scale(scaleX, scaleY, pivot = Offset.Zero)
            }) {
                // ==========================================
                // 1. BACKDROP PARALLAX SKIES & LANDSCAPES
                // ==========================================

                // Clean calming sky gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF6FBFF), // Sky Light Top
                            Color(0xFFEAF6FF), // Mid Pastel Sky
                            Color(0xFFDDF1FF)  // Horizon Air Bottom
                        ),
                        startY = 0f,
                        endY = GameState.VIRTUAL_HEIGHT
                    ),
                    size = Size(GameState.VIRTUAL_WIDTH, GameState.VIRTUAL_HEIGHT)
                )

                // Drifting Soft Ambient Clouds (with simple flat curves and no outlines)
                val cloudAX = ((distanceTraveled * 0.05f) % (GameState.VIRTUAL_WIDTH + 500f)) - 300f
                drawDetailedCloud(Offset(cloudAX, 120f), scale = 1.3f)

                val cloudBX = (((distanceTraveled * 0.03f) + 400f) % (GameState.VIRTUAL_WIDTH + 600f)) - 350f
                drawDetailedCloud(Offset(cloudBX, 280f), scale = 1.0f)

                val cloudCX = (((distanceTraveled * 0.07f) + 800f) % (GameState.VIRTUAL_WIDTH + 500f)) - 300f
                drawDetailedCloud(Offset(cloudCX, 80f), scale = 0.8f)

                // ==========================================
                // PARALLAX LAYER 2: SIMPLE ROLLING HILLS (Nature Green)
                // ==========================================
                // Distant soft rolling hills
                val hillOffset1 = (distanceTraveled * 0.08f) % GameState.VIRTUAL_WIDTH
                for (i in 0..1) {
                    val hx1 = i * GameState.VIRTUAL_WIDTH - hillOffset1
                    withTransform({
                        translate(left = hx1, top = 0f)
                    }) {
                        val hillPath1 = Path().apply {
                            moveTo(0f, 1320f)
                            quadraticTo(250f, 1140f, 500f, 1290f)
                            quadraticTo(750f, 1180f, 1000f, 1320f)
                            lineTo(1000f, GameState.PLAYABLE_HEIGHT)
                            lineTo(0f, GameState.PLAYABLE_HEIGHT)
                            close()
                        }
                        drawPath(
                            color = Color(0xFFC0EDC9), // Softest remote green-blue hill
                            path = hillPath1
                        )
                    }
                }

                // Middle rolling hills
                val hillOffset2 = (distanceTraveled * 0.16f) % GameState.VIRTUAL_WIDTH
                for (i in 0..1) {
                    val hx2 = i * GameState.VIRTUAL_WIDTH - hillOffset2
                    withTransform({
                        translate(left = hx2, top = 0f)
                    }) {
                        val hillPath2 = Path().apply {
                            moveTo(0f, 1380f)
                            quadraticTo(300f, 1220f, 600f, 1350f)
                            quadraticTo(820f, 1260f, 1000f, 1380f)
                            lineTo(1000f, GameState.PLAYABLE_HEIGHT)
                            lineTo(0f, GameState.PLAYABLE_HEIGHT)
                            close()
                        }
                        drawPath(
                            color = Color(0xFF9ADB9F), // Mid layer soft green hill
                            path = hillPath2
                        )
                        
                        // Standalone minimal lollipop trees on midground hills
                        drawStylizedTree(150f, 1340f, 1.1f)
                        drawStylizedTree(380f, 1300f, 0.9f)
                        drawStylizedTree(620f, 1350f, 1.2f)
                        drawStylizedTree(820f, 1290f, 0.8f)
                    }
                }

                // ==========================================
                // 2. OBSTACLE PIPES RENDERING SYSTEM (ROUNDED & ELEGANT)
                // ==========================================
                state.pipes.forEach { pipe ->
                    val pipeWidth = pipe.width
                    val capWidth = pipeWidth + 16f
                    val capHeight = 36f
                    val capX = pipe.x - (capWidth - pipeWidth) / 2f

                    // Modern flat vector vertical split brush
                    val pipeBrush = Brush.horizontalGradient(
                        0.0f to Color(0xFF6BCB77), // Primary Nature
                        0.7f to Color(0xFF6BCB77),
                        0.7f to Color(0xFF4CAF50), // Muted dark green shadow
                        1.0f to Color(0xFF4CAF50),
                        startX = pipe.x,
                        endX = pipe.x + pipeWidth
                    )
                    
                    val capBrush = Brush.horizontalGradient(
                        0.0f to Color(0xFF6BCB77),
                        0.7f to Color(0xFF6BCB77),
                        0.7f to Color(0xFF4CAF50),
                        1.0f to Color(0xFF4CAF50),
                        startX = capX,
                        endX = capX + capWidth
                    )

                    // 2a. Top Pipe (extends from y=0 down to gapTop)
                    val topBodyHeight = (pipe.gapTop - capHeight).coerceAtLeast(0f)
                    if (topBodyHeight > 0f) {
                        drawRoundRect(
                            brush = pipeBrush,
                            topLeft = Offset(pipe.x, -20f), // overlap top boundary
                            size = Size(pipeWidth, topBodyHeight + 20f),
                            cornerRadius = CornerRadius(0f, 0f)
                        )
                    }
                    
                    // Top Cap
                    drawRoundRect(
                        brush = capBrush,
                        topLeft = Offset(capX, pipe.gapTop - capHeight),
                        size = Size(capWidth, capHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    // Soft shadow underneath cap
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.08f),
                        topLeft = Offset(pipe.x + 2f, pipe.gapTop),
                        size = Size(pipeWidth - 4f, 12f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // 2b. Bottom Pipe (extends from bottomPipeY down to groundY)
                    val bottomPipeY = pipe.gapTop + pipe.gapSize
                    
                    // Bottom Cap
                    drawRoundRect(
                        brush = capBrush,
                        topLeft = Offset(capX, bottomPipeY),
                        size = Size(capWidth, capHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    val bottomBodyY = bottomPipeY + capHeight
                    val bottomBodyHeight = (GameState.PLAYABLE_HEIGHT - bottomBodyY).coerceAtLeast(0f)
                    if (bottomBodyHeight > 0f) {
                        drawRoundRect(
                            brush = pipeBrush,
                            topLeft = Offset(pipe.x, bottomBodyY),
                            size = Size(pipeWidth, bottomBodyHeight),
                            cornerRadius = CornerRadius(0f, 0f)
                        )
                    }
                    // Soft shadow underneath bottom cap
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.08f),
                        topLeft = Offset(pipe.x + 2f, bottomBodyY),
                        size = Size(pipeWidth - 4f, 12f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // ==========================================
                // 3. FLAPPY BIRD SPRITE RENDERING (POLISHED VECTOR MASCOT)
                // ==========================================
                val bird = state.bird
                val birdCenterX = GameState.BIRD_X + bird.width / 2f
                val birdCenterY = bird.y + bird.height / 2f

                // Squash/Stretch animation based on velocity
                val velocityRatio = (bird.vy / Bird.TERMINAL_VELOCITY).coerceIn(-1.0f, 1.0f)
                val scaleY = (1.0f - velocityRatio * 0.08f).coerceIn(0.85f, 1.15f)
                val scaleX = (1.0f + velocityRatio * 0.08f).coerceIn(0.85f, 1.15f)

                withTransform({
                    rotate(degrees = bird.rotation, pivot = Offset(birdCenterX, birdCenterY))
                    scale(scaleX, scaleY, pivot = Offset(birdCenterX, birdCenterY))
                }) {
                    // Soft vector shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.06f),
                        center = Offset(birdCenterX, birdCenterY + 8f),
                        radius = 27f
                    )

                    // Rounded Mascot Body (#FFD54F base with #FFB300 subtle gradient)
                    val birdBrush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300)),
                        center = Offset(birdCenterX - 6f, birdCenterY - 6f),
                        radius = 26f
                    )
                    drawCircle(
                        brush = birdBrush,
                        center = Offset(birdCenterX, birdCenterY),
                        radius = 25f
                    )

                    // Minimal body outline for premium visual touch
                    drawCircle(
                        color = Color(0xFF2E3A59).copy(alpha = 0.15f),
                        center = Offset(birdCenterX, birdCenterY),
                        radius = 25f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )

                    // Soft blush cheek
                    drawCircle(
                        color = Color(0xFFFF8A80).copy(alpha = 0.5f),
                        center = Offset(birdCenterX + 5f, birdCenterY + 7f),
                        radius = 7f
                    )

                    // Expressive single vector eye
                    val eyeX = birdCenterX + 10f
                    val eyeY = birdCenterY - 6f
                    
                    drawCircle(
                        color = Color.White,
                        center = Offset(eyeX, eyeY),
                        radius = 9f
                    )
                    drawCircle(
                        color = Color(0xFF2E3A59),
                        center = Offset(eyeX + 1.5f, eyeY),
                        radius = 4.5f
                    )
                    drawCircle(
                        color = Color.White,
                        center = Offset(eyeX + 3.0f, eyeY - 1.5f),
                        radius = 1.6f
                    )

                    // Beautiful clean triangular beak
                    val beakPath = Path().apply {
                        moveTo(birdCenterX + 18f, birdCenterY - 3f)
                        lineTo(birdCenterX + 28f, birdCenterY + 1f)
                        lineTo(birdCenterX + 16f, birdCenterY + 6f)
                        close()
                    }
                    drawPath(
                        path = beakPath,
                        color = Color(0xFFFFB300)
                    )
                    drawPath(
                        path = beakPath,
                        color = Color(0xFFE65100).copy(alpha = 0.25f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )

                    // Flapping Wing
                    val isMovingUp = bird.vy < 0f
                    val wingOffset = if (state.status == GameStatus.PLAYING) {
                        if (isMovingUp) -12f else 6f
                    } else {
                        val wave = Math.sin(System.currentTimeMillis() / 120.0).toFloat()
                        wave * 6f
                    }

                    val wingPath = Path().apply {
                        moveTo(birdCenterX - 18f, birdCenterY)
                        quadraticTo(
                            birdCenterX - 8f, birdCenterY - 14f + wingOffset,
                            birdCenterX + 1f, birdCenterY - 2f + wingOffset
                        )
                        quadraticTo(
                            birdCenterX - 7f, birdCenterY + 10f + wingOffset,
                            birdCenterX - 18f, birdCenterY
                        )
                        close()
                    }
                    drawPath(
                        path = wingPath,
                        color = Color(0xFFFFE082)
                    )
                    drawPath(
                        path = wingPath,
                        color = Color(0xFFFFC107).copy(alpha = 0.4f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }

                // ==========================================
                // 4. ACTIVE SPARK CONFETTI / GOLD STARS PARTICLES
                // ==========================================
                particles.forEach { p ->
                    val alphaColor = p.color.copy(alpha = p.life)
                    if (p.isStar) {
                        withTransform({
                            translate(p.x, p.y)
                            rotate(p.rotation, pivot = Offset.Zero)
                        }) {
                            val starPath = Path().apply {
                                createStarShape(0f, 0f, spikes = 5, outerRadius = p.size * 0.8f, innerRadius = p.size * 0.35f)
                            }
                            drawPath(color = alphaColor, path = starPath)
                        }
                    } else {
                        drawCircle(
                            color = alphaColor,
                            center = Offset(p.x, p.y),
                            radius = p.size / 2f
                        )
                    }
                }

                // ==========================================
                // 5. FOREGROUND GROUND (CLEAN MODERN MOSS/GRASS STRIP)
                // ==========================================
                // Solid underlying clean earth ground
                drawRect(
                    color = Color(0xFFFFFBFF),
                    topLeft = Offset(0f, GameState.PLAYABLE_HEIGHT),
                    size = Size(GameState.VIRTUAL_WIDTH, GameState.GROUND_HEIGHT)
                )

                // Background rolling humps (shadow Nature green)
                val bgHumpWidth = 100f
                val bgHumpHeight = 12f
                val bgHumpScroll = (distanceTraveled * 0.8f) % bgHumpWidth
                var bhx = -bgHumpScroll
                while (bhx < GameState.VIRTUAL_WIDTH + bgHumpWidth) {
                    drawRoundRect(
                        color = Color(0xFF4CAF50).copy(alpha = 0.4f),
                        topLeft = Offset(bhx, GameState.PLAYABLE_HEIGHT - bgHumpHeight + 1f),
                        size = Size(bgHumpWidth - 6f, bgHumpHeight * 2),
                        cornerRadius = CornerRadius(bgHumpHeight, bgHumpHeight)
                    )
                    bhx += bgHumpWidth
                }

                // Foreground rolling humps (primary Nature green #6BCB77)
                val fgHumpWidth = 80f
                val fgHumpHeight = 15f
                val fgHumpScroll = (distanceTraveled) % fgHumpWidth
                var fhx = -fgHumpScroll
                while (fhx < GameState.VIRTUAL_WIDTH + fgHumpWidth) {
                    drawRoundRect(
                        color = Color(0xFF6BCB77),
                        topLeft = Offset(fhx, GameState.PLAYABLE_HEIGHT - fgHumpHeight + 3f),
                        size = Size(fgHumpWidth - 5f, fgHumpHeight * 2),
                        cornerRadius = CornerRadius(fgHumpHeight, fgHumpHeight)
                    )
                    fhx += fgHumpWidth
                }

                // Solid base grass ribbon
                drawRect(
                    color = Color(0xFF6BCB77),
                    topLeft = Offset(0f, GameState.PLAYABLE_HEIGHT),
                    size = Size(GameState.VIRTUAL_WIDTH, GameState.GROUND_HEIGHT)
                )
            }
        }

        // ==========================================
        // 6. JETPACK COMPOSE HUD AND OVERLAY LAYERS
        // ==========================================

        // HUD Active Gameplay Score
        if (state.status != GameStatus.READY) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 70.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Front primary white score with subtle modern drop shadow
                Text(
                    text = "${state.score}",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-1.5).sp,
                        textAlign = TextAlign.Center,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.12f),
                            offset = Offset(0f, 6f),
                            blurRadius = 12f
                        )
                    ),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scoreScaleAnim.value
                            scaleY = scoreScaleAnim.value
                        }
                        .testTag("on_screen_score_counter")
                )
            }
        }

        // 6a. Action Ready Stage: "TAP TO START" pulsing panel with modern styling
        if (state.status == GameStatus.READY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-65).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TAP TO START",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF2E3A59),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.05f),
                            offset = Offset(0f, 4f),
                            blurRadius = 8f
                        )
                    ),
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = pulseScale,
                            scaleY = pulseScale
                        )
                        .testTag("tap_to_start_msg")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.75f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Flap your wings to dodge columns!",
                        color = Color(0xFF475569),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 6b. Bouncy Spring Slide Game Over Card & Badge Accolades
        if (state.status == GameStatus.GAME_OVER) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)), // Dim background modal focus
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .width(310.dp)
                        .offset(y = cardTranslateY.dp) // glide spring offset
                        .testTag("game_over_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title Board: Premium Slate Blue Accent
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E3A59))
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "GAME OVER",
                                style = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.testTag("game_over_title")
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Scoreboard info block (Sleek minimalist style)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Medals Accolade Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MEDAL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                
                                val medalColor = when {
                                    state.score >= 40 -> Color(0xFFFFB300) // Gold
                                    state.score >= 20 -> Color(0xFF90A4AE) // Silver
                                    state.score >= 5  -> Color(0xFFA1887F) // Bronze
                                    else -> Color(0xFFCBD5E1)              // Amateur
                                }
                                val medalLabel = when {
                                    state.score >= 40 -> "🏆 GOLD"
                                    state.score >= 20 -> "🥈 SILVER"
                                    state.score >= 5 -> "🥉 BRONZE"
                                    else -> "🐣 AMATEUR"
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(medalColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = medalLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = medalColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))

                            // Score points Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SCORE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "${state.score}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.testTag("card_final_score")
                                )
                            }

                            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))

                            // High score row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "BEST",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                    if (state.score > 0 && state.score >= state.highScore) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFFFB300))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "NEW!",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${state.highScore}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.testTag("card_high_score")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Large Pill-shaped fully rounded modern button
                        Button(
                            onClick = { viewModel.onScreenTap() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6BCB77), // Clean primary Green
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("restart_button")
                        ) {
                            Text(
                                text = "RESTART",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom math helper to construct star vectors easily on canvas
 */
private fun Path.createStarShape(
    cx: Float,
    cy: Float,
    spikes: Int,
    outerRadius: Float,
    innerRadius: Float
) {
    var rot = Math.PI.toFloat() / 2f * 3f
    val step = Math.PI.toFloat() / spikes
    moveTo(cx, cy - outerRadius)
    for (i in 0 until spikes) {
        val px = cx + Math.cos(rot.toDouble()).toFloat() * outerRadius
        val py = cy + Math.sin(rot.toDouble()).toFloat() * outerRadius
        lineTo(px, py)
        rot += step

        val qx = cx + Math.cos(rot.toDouble()).toFloat() * innerRadius
        val qy = cy + Math.sin(rot.toDouble()).toFloat() * innerRadius
        lineTo(qx, qy)
        rot += step
    }
    close()
}

/**
 * Helper inside DrawScope to draw detailed vector fluffy cloud structures.
 */
private fun DrawScope.drawDetailedCloud(
    offset: Offset,
    scale: Float = 1.0f
) {
    val cloudColor = Color.White.copy(alpha = 0.45f)
    val baseW = 120f * scale
    val baseH = 34f * scale

    drawRoundRect(
        color = cloudColor,
        topLeft = offset,
        size = Size(baseW, baseH),
        cornerRadius = CornerRadius(baseH / 2f, baseH / 2f)
    )
    drawCircle(
        color = cloudColor,
        center = Offset(offset.x + 38f * scale, offset.y + 4f * scale),
        radius = 24f * scale
    )
    drawCircle(
        color = cloudColor,
        center = Offset(offset.x + 72f * scale, offset.y + 8f * scale),
        radius = 18f * scale
    )
}

/**
 * Helper inside DrawScope to draw detailed vector evergreen pine trees.
 */
private fun DrawScope.drawStylizedTree(
    x: Float,
    y: Float,
    scale: Float = 1.0f
) {
    val trunkW = 4f * scale
    val trunkH = 35f * scale
    
    // Flat minimalist trunk
    drawRect(
        color = Color(0xFFC5E1A5).copy(alpha = 0.8f),
        topLeft = Offset(x - trunkW / 2f, y - trunkH),
        size = Size(trunkW, trunkH)
    )

    // Flat circle crown
    val leafRadius = 14f * scale
    drawCircle(
        color = Color(0xFF6BCB77),
        center = Offset(x, y - trunkH),
        radius = leafRadius
    )

    // Flat crescent shadow side
    val shadowPath = Path().apply {
        addArc(
            androidx.compose.ui.geometry.Rect(x - leafRadius, y - trunkH - leafRadius, x + leafRadius, y - trunkH + leafRadius),
            90f,
            180f
        )
    }
    drawPath(
        path = shadowPath,
        color = Color(0xFF4CAF50).copy(alpha = 0.3f)
    )
}

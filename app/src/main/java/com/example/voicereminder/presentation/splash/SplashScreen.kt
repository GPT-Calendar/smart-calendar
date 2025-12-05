package com.example.voicereminder.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.R
import kotlinx.coroutines.delay

// 60-30-10 Color Scheme
private val DeepBlue = Color(0xFF305CDE)      // 30% - Primary branding
private val LightBlue = Color(0xFF5D83FF)     // 10% - Accents
private val White = Color(0xFFFFFFFF)         // 60% - Background

/**
 * Splash Screen for Smart Calendar app
 * Features:
 * - Animated logo with pulsing effect
 * - Animated loading indicator
 * - Smooth fade-in animations
 * - 60-30-10 color scheme compliance
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showLoadingDots by remember { mutableStateOf(false) }
    
    // Trigger animations
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(400)
        showTagline = true
        delay(300)
        showLoadingDots = true
        delay(1800) // Total splash duration ~2.5 seconds
        onSplashComplete()
    }
    
    // Logo scale animation
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    // Logo alpha animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logoAlpha"
    )
    
    // Tagline alpha animation
    val taglineAlpha by animateFloatAsState(
        targetValue = if (showTagline) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "taglineAlpha"
    )

    // Pulsing animation for the outer ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlue), // Deep Blue background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo Container with animations
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
            ) {
                // Outer pulsing ring (Light Blue accent on deep blue bg)
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    White.copy(alpha = 0.3f),
                                    White.copy(alpha = 0f)
                                )
                            )
                        )
                )
                
                // App Logo Image
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Smart Calendar Logo",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name
            Text(
                text = "Smart Calendar",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = White, // White text on deep blue background
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Your AI-powered personal assistant",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = White.copy(alpha = 0.8f), // Light white for tagline
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Loading indicator
            if (showLoadingDots) {
                AnimatedLoadingDots()
            }
        }
        
        // Bottom branding
        Text(
            text = "Powered by AI",
            fontSize = 12.sp,
            color = White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(taglineAlpha)
        )
    }
}


/**
 * Animated loading dots that bounce in sequence
 */
@Composable
private fun AnimatedLoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 600
                        0f at 0
                        -8f at 150
                        0f at 300
                        0f at 600
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(White) // White dots on deep blue background
            )
        }
    }
}

/**
 * Alternative: Circular progress indicator style
 */
@Composable
private fun AnimatedCircularProgress() {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Canvas(
        modifier = Modifier.size(32.dp)
    ) {
        val strokeWidth = 3.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        
        // Background circle
        drawCircle(
            color = Color(0xFFE0E0E0),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        
        // Animated arc
        drawArc(
            color = LightBlue,
            startAngle = rotation,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Feature highlights that can be shown during loading
 */
@Composable
private fun FeatureHighlights() {
    val features = listOf(
        "📅 Smart Reminders",
        "💰 Finance Tracking", 
        "🗺️ Location-based Alerts",
        "🎤 Voice Commands"
    )
    
    var currentFeature by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(800)
            currentFeature = (currentFeature + 1) % features.size
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "featureAlpha"
    )
    
    Text(
        text = features[currentFeature],
        fontSize = 14.sp,
        color = Color(0xFF666666),
        modifier = Modifier.alpha(alpha)
    )
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

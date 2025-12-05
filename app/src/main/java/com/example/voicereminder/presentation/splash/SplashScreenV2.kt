package com.example.voicereminder.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Alternative Splash Screen V2 - Feature Showcase Style
 * Shows rotating feature icons around the main logo
 */
@Composable
fun SplashScreenV2(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }
    var currentFeatureIndex by remember { mutableStateOf(0) }
    
    val features = listOf(
        Feature(Icons.Default.Notifications, "Smart Reminders"),
        Feature(Icons.Default.AccountBalance, "Finance Tracking"),
        Feature(Icons.Default.LocationOn, "Location Alerts"),
        Feature(Icons.Default.Mic, "Voice Commands"),
        Feature(Icons.Default.CalendarToday, "Calendar Sync")
    )
    
    // Trigger animations
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(500)
        showFeatures = true
        
        // Cycle through features
        repeat(features.size) {
            delay(400)
            currentFeatureIndex = (currentFeatureIndex + 1) % features.size
        }
        
        delay(300)
        onSplashComplete()
    }
    
    // Logo animations
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "logoAlpha"
    )
    
    // Rotating ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
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
            // Logo with orbiting feature icons
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            ) {
                // Orbiting feature icons
                if (showFeatures) {
                    features.forEachIndexed { index, feature ->
                        val angle = (360f / features.size) * index + rotation
                        val radius = 80.dp
                        
                        val featureAlpha by animateFloatAsState(
                            targetValue = if (index == currentFeatureIndex) 1f else 0.4f,
                            animationSpec = tween(300),
                            label = "featureAlpha$index"
                        )
                        
                        val featureScale by animateFloatAsState(
                            targetValue = if (index == currentFeatureIndex) 1.2f else 0.8f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "featureScale$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (radius.value * kotlin.math.cos(angle * kotlin.math.PI / 180)).dp,
                                    y = (radius.value * kotlin.math.sin(angle * kotlin.math.PI / 180)).dp
                                )
                                .scale(featureScale)
                                .alpha(featureAlpha)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (index == currentFeatureIndex) White
                                    else White.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = feature.name,
                                tint = if (index == currentFeatureIndex) DeepBlue else White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                // Center logo - App Logo Image
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Smart Calendar Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // App Name
            Text(
                text = "Smart Calendar",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = White, // White text on deep blue background
                modifier = Modifier.alpha(logoAlpha)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current feature name
            if (showFeatures) {
                val featureTextAlpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(200),
                    label = "featureTextAlpha"
                )
                
                Text(
                    text = features[currentFeatureIndex].name,
                    fontSize = 14.sp,
                    color = White.copy(alpha = 0.9f), // Light white for feature name
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alpha(featureTextAlpha)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Progress indicator
            if (showFeatures) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    features.forEachIndexed { index, _ ->
                        val dotColor by animateColorAsState(
                            targetValue = if (index <= currentFeatureIndex) White else White.copy(alpha = 0.3f),
                            animationSpec = tween(200),
                            label = "dotColor$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color = dotColor)
                        )
                    }
                }
            }
        }
        
        // Bottom text
        Text(
            text = "Powered by AI",
            fontSize = 12.sp,
            color = White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(logoAlpha)
        )
    }
}

private data class Feature(
    val icon: ImageVector,
    val name: String
)

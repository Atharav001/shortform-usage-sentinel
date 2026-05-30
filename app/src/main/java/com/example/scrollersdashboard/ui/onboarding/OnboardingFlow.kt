package com.example.scrollersdashboard.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollersdashboard.ui.BrainLottieLayer
import com.example.scrollersdashboard.ui.BrainState
import com.example.scrollersdashboard.ui.WarmScreenBackground
import com.example.scrollersdashboard.ui.theme.AppGradients
import com.example.scrollersdashboard.ui.theme.GlassBorder
import com.example.scrollersdashboard.ui.theme.Gray400
import com.example.scrollersdashboard.ui.theme.Gray500
import com.example.scrollersdashboard.ui.theme.OrbAmber
import com.example.scrollersdashboard.ui.theme.OrbCopper
import kotlinx.coroutines.launch

@Composable
fun OnboardingFlow(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    WarmScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WelcomeScreen()
                    1 -> ProblemScreen()
                    2 -> SolutionScreen()
                    3 -> FeaturesScreen()
                    4 -> PermissionsScreen(onComplete = onComplete)
                }
            }

            OnboardingPageIndicators(
                pageCount = 5,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage in 1..3) {
                    TextButton(onClick = onComplete) {
                        Text("Skip", color = Gray400)
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                if (pagerState.currentPage < 4) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrbCopper),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = currentPage == index
            val width by animateFloatAsState(
                targetValue = if (isActive) 28f else 8f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "indicatorWidth"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(width.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) Color.White else Color.White.copy(alpha = 0.28f)
                    )
            )
        }
    }
}

@Composable
private fun OnboardingPageShell(
    brainState: BrainState,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(AppGradients.HeroCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            BrainLottieLayer(
                state = brainState,
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = subtitle,
            fontSize = 17.sp,
            color = Color.White.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(28.dp))
        content()
    }
}

@Composable
fun WelcomeScreen() {
    OnboardingPageShell(
        brainState = BrainState.HEALTHY,
        title = "Meet Your Brain",
        subtitle = "Build healthier scrolling habits with a companion that reacts to every reel and short."
    )
}

@Composable
fun ProblemScreen() {
    OnboardingPageShell(
        brainState = BrainState.MELTING,
        title = "Endless Scrolling",
        subtitle = "Reels, Shorts, TikTok — hours disappear before you notice.",
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard("2.5 hrs", "Daily avg")
                StatCard("150+", "Reels/day")
            }
        }
    )
}

@Composable
fun SolutionScreen() {
    OnboardingPageShell(
        brainState = BrainState.CONCERNED,
        title = "Awareness First",
        subtitle = "Track every scroll in real time. See your brain change as usage climbs — then take back control."
    )
}

@Composable
fun FeaturesScreen() {
    OnboardingPageShell(
        brainState = BrainState.TIRED,
        title = "Everything You Need",
        subtitle = "One dashboard for habits, goals, analytics, and gentle nudges when you've had enough.",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureRow(Icons.Default.Analytics, "Usage analytics", "Day, week, and month charts")
                FeatureRow(Icons.Default.TouchApp, "Live scroll counter", "Floating overlay while you browse")
                FeatureRow(Icons.Default.Notifications, "Smart limits", "Alerts before your brain melts")
            }
        }
    )
}

@Composable
fun PermissionsScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    OnboardingPageShell(
        brainState = BrainState.HEALTHY,
        title = "Almost There",
        subtitle = "Grant permissions so we can track scrolling and show your live counter.",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionCard(
                    title = "Usage Access",
                    description = "Measure time in Instagram & YouTube",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
                PermissionCard(
                    title = "Accessibility",
                    description = "Detect scroll gestures accurately",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                PermissionCard(
                    title = "Display Overlay",
                    description = "Show the floating scroll counter",
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrbAmber),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Get Started", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    )
}

@Composable
private fun StatCard(value: String, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(label, color = Gray500, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(OrbAmber, OrbCopper))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Gray400, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, null, tint = OrbAmber, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, color = Gray400, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Gray400, modifier = Modifier.size(18.dp))
        }
    }
}

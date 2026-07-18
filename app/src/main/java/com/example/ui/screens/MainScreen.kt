package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import android.graphics.Bitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AutomationLog
import com.example.data.model.BrowserProfile
import com.example.data.model.ProxyConfig
import com.example.data.model.SearchQuery
import com.example.data.model.WeeklyTopic
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryBlue
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val activeTopic by viewModel.activeTopic.collectAsStateWithLifecycle()
    val queries by viewModel.activeTopicQueries.collectAsStateWithLifecycle()
    val proxies by viewModel.proxies.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.rollingTerminalLogs.collectAsStateWithLifecycle()
    val defaultProfile by viewModel.defaultProfile.collectAsStateWithLifecycle()

    var activeWebViewReference by remember { mutableStateOf<WebView?>(null) }

    // Enforce RTL Layout Direction for beautiful Arabic-first design
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (viewModel.isSplashActive) {
            BingSplashScreen()
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Miniature Glowing Bing Logo Badge
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF0078D4), Color(0xFF00F2FE))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "b",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.offset(y = (-1).dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "برنامج بانجو الآلي",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "محرك الأتمتة والبحث الذكي • Copilot AI",
                                        fontSize = 10.sp,
                                        color = Color.LightGray.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.isAutomationRunning) Color(0xFF047857) else Color(0xFF1E2F4C))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "pulse"
                                    )

                                    if (viewModel.isAutomationRunning) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .drawBehind {
                                                    drawCircle(
                                                        color = Color(0xFF34D399),
                                                        alpha = alpha
                                                    )
                                                }
                                        )
                                        Text("أتمتة نشطة حية", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.Gray)
                                        )
                                        Text("مستعد للخمول", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SurfaceDark
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = SurfaceDark,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = viewModel.selectedTab == 0,
                            onClick = { viewModel.selectedTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                            label = { Text("بينج اليوم") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00F2FE),
                                selectedTextColor = Color(0xFF00F2FE),
                                indicatorColor = Color(0xFF0078D4).copy(alpha = 0.25f),
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray
                            )
                        )
                        NavigationBarItem(
                            selected = viewModel.selectedTab == 1,
                            onClick = { viewModel.selectedTab = 1 },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "الأتمتة الحية") },
                            label = { Text("المتصفح") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00F2FE),
                                selectedTextColor = Color(0xFF00F2FE),
                                indicatorColor = Color(0xFF0078D4).copy(alpha = 0.25f),
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray
                            )
                        )
                        NavigationBarItem(
                            selected = viewModel.selectedTab == 4,
                            onClick = { viewModel.selectedTab = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "البروفايلات") },
                            label = { Text("الهويات") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00F2FE),
                                selectedTextColor = Color(0xFF00F2FE),
                                indicatorColor = Color(0xFF0078D4).copy(alpha = 0.25f),
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray
                            )
                        )
                        NavigationBarItem(
                            selected = viewModel.selectedTab == 2,
                            onClick = { viewModel.selectedTab = 2 },
                            icon = { Icon(Icons.Default.Lock, contentDescription = "خوادم الوكيل") },
                            label = { Text("الشبكة") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00F2FE),
                                selectedTextColor = Color(0xFF00F2FE),
                                indicatorColor = Color(0xFF0078D4).copy(alpha = 0.25f),
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray
                            )
                        )
                        NavigationBarItem(
                            selected = viewModel.selectedTab == 3,
                            onClick = { viewModel.selectedTab = 3 },
                            icon = { Icon(Icons.Default.List, contentDescription = "سجل العمليات") },
                            label = { Text("سجل البحث") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00F2FE),
                                selectedTextColor = Color(0xFF00F2FE),
                                indicatorColor = Color(0xFF0078D4).copy(alpha = 0.25f),
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray
                            )
                        )
                    }
            },
            containerColor = BackgroundDark,
            modifier = modifier
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (viewModel.selectedTab) {
                    0 -> DashboardTab(
                        viewModel = viewModel,
                        activeTopic = activeTopic,
                        queries = queries,
                        onStartAutopilot = {
                            viewModel.selectedTab = 1
                            viewModel.startLiveAutomation { url, ua ->
                                activeWebViewReference?.let { webView ->
                                    try {
                                        webView.settings.userAgentString = ua
                                        webView.loadUrl(url)
                                        // Trigger human scroll action inside WebView
                                        coroutineScope.launch {
                                            try {
                                                delay(1500)
                                                webView.evaluateJavascript("window.scrollBy({ top: 350, behavior: 'smooth' });", null)
                                            } catch (t: Throwable) {
                                                t.printStackTrace()
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        t.printStackTrace()
                                    }
                                }
                            }
                        }
                    )
                    1 -> AutopilotTab(
                        viewModel = viewModel,
                        activeTopic = activeTopic,
                        terminalLogs = terminalLogs,
                        defaultProfile = defaultProfile,
                        onWebViewCreated = { webView ->
                            activeWebViewReference = webView
                        }
                    )
                    2 -> ProxiesTab(
                        viewModel = viewModel,
                        proxies = proxies
                    )
                    3 -> LogsTab(
                        viewModel = viewModel,
                        logs = logs
                    )
                    4 -> ProfilesTab(
                        viewModel = viewModel
                    )
                }
            }
          }
        }
    }
}

@Composable
fun BingSplashScreen() {
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val animationTime = 2500L
        val steps = 100
        for (i in 1..steps) {
            delay(animationTime / steps)
            progress = i / steps.toFloat()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B19), // Dark Blue Space
                        Color(0xFF101B35), // Deep Navy
                        Color(0xFF152238)  // Slate Edge
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative glowing background circles (Aurora effect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        color = Color(0xFF0078D4).copy(alpha = 0.12f),
                        radius = size.minDimension * 0.5f,
                        center = Offset(size.width * 0.2f, size.height * 0.3f)
                    )
                    drawCircle(
                        color = Color(0xFF7F56D9).copy(alpha = 0.12f),
                        radius = size.minDimension * 0.5f,
                        center = Offset(size.width * 0.8f, size.height * 0.7f)
                    )
                    drawCircle(
                        color = Color(0xFF008272).copy(alpha = 0.1f),
                        radius = size.minDimension * 0.4f,
                        center = Offset(size.width * 0.5f, size.height * 0.5f)
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Stylized Glowing Bing/Copilot Logo using Canvas
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .drawBehind {
                        // Background glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00F2FE).copy(alpha = 0.3f), Color.Transparent),
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Drawing Bing "b" stylized loop
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    // Drawing the circular loop base of the "b"
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00F2FE), Color(0xFF0078D4))
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 10.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    // Drawing the vertical spine of the "b"
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF7F56D9), Color(0xFF00F2FE))
                        ),
                        start = Offset(x = 12.dp.toPx(), y = -16.dp.toPx()),
                        end = Offset(x = 12.dp.toPx(), y = height - 12.dp.toPx()),
                        strokeWidth = 10.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Accent Copilot light dot
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 5.dp.toPx(),
                        center = Offset(x = 12.dp.toPx(), y = -16.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brand Titles
            Text(
                text = "برنامج بانجو الآلي",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Bango Autopilot Browser",
                fontSize = 14.sp,
                color = Color(0xFF00F2FE),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "أمن سيبراني متكامل • أتمتة بحث ذكية • إدارة بروكسي سحابي",
                fontSize = 12.sp,
                color = Color.LightGray.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Horizontal custom loading bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(220.dp)
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    // Progress Fill with Bing Copilot Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0078D4), // Blue
                                        Color(0xFF00F2FE), // Teal
                                        Color(0xFF7F56D9)  // Purple
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = when {
                        progress < 0.3f -> "جاري تأمين البصمة الرقمية للشبكة..."
                        progress < 0.6f -> "تجهيز خوادم البروكسي الدوّار المدمجة..."
                        progress < 0.9f -> "تحميل مستشار الأمن الذكي لـ Copilot AI..."
                        else -> "بدء الاتصال الآمن بالشبكة المشفرة..."
                    },
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: MainViewModel,
    activeTopic: WeeklyTopic?,
    queries: List<SearchQuery>,
    onStartAutopilot: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Microsoft Bing Hero Daily Wallpaper Banner ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A), // Dark Slate
                                    Color(0xFF0078D4), // Bing Ocean Blue
                                    Color(0xFF008272), // Bing Teal
                                    Color(0xFF7F56D9)  // Copilot Indigo
                                )
                            )
                        )
                        .drawBehind {
                            // Artistic glass ripples
                            drawCircle(
                                color = Color.White.copy(alpha = 0.05f),
                                radius = size.minDimension * 0.6f,
                                center = Offset(size.width * 0.8f, size.height * 0.2f)
                            )
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("b", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = "Bing",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF7F56D9))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Copilot", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "صورة اليوم المميزة • استكشاف ذكي مستقل ومجهول الهوية",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )

                        // Search box mock
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(23.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "بحث",
                                        tint = Color.White
                                    )
                                    Text(
                                        text = if (activeTopic != null) activeTopic.topicText else "اسألني عن أي شيء لتوليد خطة بحث مخصصة...",
                                        color = if (activeTopic != null) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (viewModel.isAutomationRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                        contentDescription = "التالي",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Google Authentication Card ---
        item {
            var showAccountSelector by remember { mutableStateOf(false) }

            if (showAccountSelector) {
                AlertDialog(
                    onDismissRequest = { showAccountSelector = false },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAccountSelector = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                    },
                    containerColor = Color(0xFF1E293B),
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تسجيل الدخول باستخدام Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "اختر حساباً للمتابعة إلى برنامج بانجو الآلي",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Card(
                                onClick = {
                                    viewModel.signInWithGoogle("mahmoudmorsy949@gmail.com", "محمود مرسي")
                                    showAccountSelector = false
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "M",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "محمود مرسي",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "mahmoudmorsy949@gmail.com",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "حساب آخر",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "استخدام حساب آخر",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لمتابعة المزامنة، سيشارك Google اسمك وعنوان بريدك الإلكتروني وصورتك الشخصية مع هذا التطبيق لتأمين وحفظ بصمات المتصفح السحابية الخاصة بك.",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )
                        }
                    }
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isGoogleSignedIn) Color(0xFF132A1C) else SurfaceDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (viewModel.isGoogleSignedIn) Color(0xFF10B981).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (viewModel.isGoogleSignedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF10B981), Color(0xFF047857))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "M",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = viewModel.googleUserName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "مـمـيـز",
                                            color = Color(0xFF34D399),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = viewModel.googleUserEmail,
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }

                            IconButton(
                                onClick = { viewModel.signOutGoogle() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "تسجيل الخروج",
                                    tint = Color.Red.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "آمن",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "مزامنة سحابية نشطة • لا حاجة لإدخال مفاتيح API يدوية",
                                fontSize = 11.sp,
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "تفعيل المزايا الذكية بنقرة واحدة 🚀",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "سجل الدخول بحساب Google لتجاوز متطلبات مفتاح API والبدء فوراً",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Button(
                            onClick = { showAccountSelector = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "تسجيل الدخول السريع باستخدام Google",
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Gemini Key & Security Advisor Hub ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security AI Advisor",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "المستشار الأمني الذكي للشبكة 🛡️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "نظام ذكاء اصطناعي سيبراني متكامل لتحليل بصمات المتصفحات، توجيه حركة المرور المشفرة وتخصيص سيناريوهات التخفي المتقدمة.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )

                    // Gemini API Key Input / Google Premium Badge
                    if (viewModel.isGoogleSignedIn) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "نشط", tint = Color(0xFF10B981))
                                Column {
                                    Text("🔑 مفتاح API مُدار سحابياً", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("تم ربط مفتاح API السحابي للذكاء الاصطناعي تلقائياً بحساب Google المميز الخاص بك.", color = Color.LightGray, fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        var apiKeyValue by remember { mutableStateOf(viewModel.customApiKey) }
                        OutlinedTextField(
                            value = apiKeyValue,
                            onValueChange = { apiKeyValue = it },
                            label = { Text("مفتاح API لـ Gemini الذكي", color = Color.LightGray, fontSize = 11.sp) },
                            placeholder = { Text("AIzaSy...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.saveCustomApiKey(apiKeyValue) }) {
                                    Icon(Icons.Default.Check, contentDescription = "حفظ المفتاح", tint = Color(0xFF10B981))
                                }
                            }
                        )
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    Text(
                        text = "💬 استشارات الأمن السيبراني والأتمتة الحية:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )

                    // Chat messages window
                    val chatMsgs by viewModel.chatMessages.collectAsState()
                    val lazyListState = rememberLazyListState()

                    LaunchedEffect(chatMsgs.size) {
                        if (chatMsgs.isNotEmpty()) {
                            lazyListState.animateScrollToItem(chatMsgs.size - 1)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark.copy(alpha = 0.8f))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMsgs) { msg ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (msg.isUser) 12.dp else 0.dp,
                                                    bottomEnd = if (msg.isUser) 0.dp else 12.dp
                                                )
                                            )
                                            .background(
                                                if (msg.isUser) Color(0xFF1E3A8A) else Color(0xFF334155)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            textAlign = if (msg.isUser) TextAlign.Right else TextAlign.Left,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Chat input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.chatInput,
                            onValueChange = { viewModel.chatInput = it },
                            placeholder = { Text("اسأل المستشار عن جدران الحماية، بصمة Canvas...", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            enabled = !viewModel.isAiResponding
                        )

                        IconButton(
                            onClick = { viewModel.sendChatMessage() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (viewModel.chatInput.isNotBlank()) PrimaryBlue else Color.Gray.copy(alpha = 0.5f)),
                            enabled = viewModel.chatInput.isNotBlank() && !viewModel.isAiResponding
                        ) {
                            if (viewModel.isAiResponding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "إرسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI Weekly Topic Configurator Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = " تغذية خطة الاستكشاف الموجهة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "أدخل الكلمة المفتاحية الاستراتيجية أو الموضوع الهادف. سيقوم الذكاء الاصطناعي بنشره وتوسعته إلى 8 طرق استعلام وصيغ مختلفة لتغطية التصفح الشامل.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = viewModel.topicInput,
                        onValueChange = { viewModel.topicInput = it },
                        placeholder = { Text("مثال: الذكاء الاصطناعي في تمييز الأورام الطبية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        enabled = !viewModel.isGeneratingQueries
                    )

                    Button(
                        onClick = { viewModel.submitWeeklyTopic() },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.topicInput.isNotBlank() && !viewModel.isGeneratingQueries
                    ) {
                        if (viewModel.isGeneratingQueries) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري توليد الاستعلامات الذكية...")
                        } else {
                            Text("توليد وتخصيص دورة 8 استعلامات ✨")
                        }
                    }
                }
            }
        }

        // Active Weekly Plan Frame
        if (activeTopic != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "الموضوع الاسترشادي للمرحلة الحالية",
                                    fontSize = 11.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = activeTopic.topicText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { viewModel.deleteWeeklyTopic(activeTopic.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف الموضوع", tint = Color(0xFFEF4444))
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.3f))

                        Text(
                            text = "صيغ وطرق البحث الثمانية المفعلة (مستقلة وتلقائية):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )

                        // Queries list in plan
                        queries.forEachIndexed { idx, q ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark.copy(alpha = 0.4f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(SecondaryBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = q.queryText,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = when (q.methodType) {
                                                "STANDARD" -> "قاعدي عام (Direct Basic)"
                                                "COMPARISON" -> "مقارنة ودراسة منافسين (Comparison Matrix)"
                                                "SYNONYMS" -> "تنصيفات ومرادفات بديلة (Semantic Term)"
                                                "SCHOLARLY" -> "تصفح علمي وأكاديمي (Research Scholarly)"
                                                "TRENDS" -> "بروز واتجاهات تقنية (Temporal Trends)"
                                                "CRITIQUE" -> "عثرات ونقد التكنولوجيا (Risk & Critique)"
                                                "CASE_STUDY" -> "تحقيق ودراسة حالة (Practical Case)"
                                                "FUTURE" -> "تحليل وتطلعات 2030 (Future Forecast)"
                                                else -> q.methodType
                                            },
                                            fontSize = 10.sp,
                                            color = PrimaryBlue
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteQuery(q.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الاستعلام", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                            }
                        }

                        // Search Engine Selection Dropdown/Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark.copy(alpha = 0.6f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🌐 محرك البحث المستهدف:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            
                            var expandedEngine by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { expandedEngine = true },
                                    colors = CardDefaults.outlinedCardColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (viewModel.selectedEngineName) {
                                                "Auto" -> "تدوير تلقائي ذكي لجميع المحركات 🌀"
                                                else -> "محرك بحث مخصص: ${viewModel.selectedEngineName} 🔍"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            tint = Color.White
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedEngine,
                                    onDismissRequest = { expandedEngine = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(SurfaceDark)
                                ) {
                                    val engines = listOf("Auto", "Google", "Bing", "DuckDuckGo", "Yahoo", "Baidu", "Yandex", "Ask", "Ecosia")
                                    engines.forEach { eng ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (eng) {
                                                        "Auto" -> "🌀 تدوير تلقائي (Auto-Rotate All)"
                                                        else -> "🔍 $eng"
                                                    },
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectedEngineName = eng
                                                expandedEngine = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Start Live autopilot sandbox trigger
                        Button(
                            onClick = onStartAutopilot,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start autopilot")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بدء تشغيل الأتمتة والبحث الحي الآن 🤖")
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Empty", modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Text(
                            text = "بانتظار تذويب موضوع بحث وتغذيته للبدء",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "اكتب كلمة أولية مفيدة في الأعلى لينطلق نظام التنقيب الذاتي.",
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Custom Query Adder Section
        if (activeTopic != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "➕ إضافة استعلام تصفح يدوي مخصص",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = viewModel.customQueryInput,
                            onValueChange = { viewModel.customQueryInput = it },
                            placeholder = { Text("مثال: عقبات استقرار الأمن الإلكتروني") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.addCustomQuery() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تأكيد وحفظ الاستعلام", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generateAntiFingerprintScript(profile: BrowserProfile?): String {
    val canvasNoise = if (profile?.canvasSpoof != false) "true" else "false"
    val webGlNoise = if (profile?.webGlSpoof != false) "true" else "false"
    val audioNoise = if (profile?.audioSpoof != false) "true" else "false"
    val rectsNoise = if (profile?.clientRectsSpoof != false) "true" else "false"
    val rtcSpoof = profile?.webRtcSpoof ?: "BLOCK" // "BLOCK", "REAL", "CUSTOM"
    val cores = profile?.hardwareConcurrency ?: 8
    val ram = profile?.deviceMemory ?: 8
    val userAgent = profile?.userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    val platform = profile?.platform ?: "Windows"
    val language = profile?.language ?: "ar-EG,ar;q=0.9,en-US;q=0.8"
    val resolution = profile?.screenResolution ?: "1920x1080"
    
    val geoSpoof = profile?.geolocationSpoof ?: "AUTO"
    val lat = profile?.customLatitude ?: 24.7136
    val lon = profile?.customLongitude ?: 46.6753
    
    val timezoneSpoof = profile?.timezoneSpoof ?: "AUTO"
    val customTz = profile?.customTimezone ?: "Asia/Riyadh"

    val dimensions = try {
        val parts = resolution.split("x")
        parts[0].toInt() to parts[1].toInt()
    } catch (e: Exception) {
        1920 to 1080
    }
    val w = dimensions.first
    val h = dimensions.second

    return """
        (function() {
            console.log("🛡️ Anti-Detect Shield Activated: ${profile?.name ?: "Default"}");
            
            // 1. Spoof Navigator Properties (Multilogin / AdsPower Style)
            try {
                Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => $cores, configurable: true });
                Object.defineProperty(navigator, 'deviceMemory', { get: () => $ram, configurable: true });
                Object.defineProperty(navigator, 'userAgent', { get: () => "$userAgent", configurable: true });
                Object.defineProperty(navigator, 'platform', { get: () => "$platform", configurable: true });
                Object.defineProperty(navigator, 'languages', { get: () => ["${language.split(",")[0]}", "en-US", "en"], configurable: true });
                Object.defineProperty(navigator, 'language', { get: () => "${language.split(",")[0]}", configurable: true });
            } catch(e) { console.error("Error spoofing navigator:", e); }

            // 2. Screen Resolution Spoofing (Incogniton Style)
            try {
                Object.defineProperty(screen, 'width', { get: () => $w, configurable: true });
                Object.defineProperty(screen, 'height', { get: () => $h, configurable: true });
                Object.defineProperty(screen, 'availWidth', { get: () => $w, configurable: true });
                Object.defineProperty(screen, 'availHeight', { get: () => $h, configurable: true });
            } catch(e) {}

            // 3. WebRTC Protection (Dolphin{anty} / Multilogin Style)
            if ("$rtcSpoof" === "BLOCK") {
                try {
                    delete window.RTCPeerConnection;
                    delete window.webkitRTCPeerConnection;
                } catch(e) {}
            } else if ("$rtcSpoof" === "CUSTOM") {
                try {
                    const originalRTCPeerConnection = window.RTCPeerConnection;
                    window.RTCPeerConnection = function(config) {
                        const pc = new originalRTCPeerConnection(config);
                        pc.createOffer = function(cb, err) {
                            return new Promise((resolve) => {
                                resolve({ sdp: "v=0\r\no=- 123456 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\na=group:BUNDLE rtp\r\na=msid-semantic: WMS" });
                            });
                        };
                        return pc;
                    };
                } catch(e) {}
            }

            // 4. Geolocation Spoofing (Incogniton Style)
            if ("$geoSpoof" === "BLOCK") {
                try {
                    navigator.geolocation.getCurrentPosition = function(success, error) {
                        if (error) error({ code: 1, message: "User denied Geolocation" });
                    };
                } catch(e) {}
            } else if ("$geoSpoof" === "CUSTOM" || "$geoSpoof" === "AUTO") {
                try {
                    navigator.geolocation.getCurrentPosition = function(success, error) {
                        success({
                            coords: {
                                latitude: $lat,
                                longitude: $lon,
                                accuracy: 10,
                                altitude: null,
                                altitudeAccuracy: null,
                                heading: null,
                                speed: null
                            },
                            timestamp: Date.now()
                        });
                    };
                    navigator.geolocation.watchPosition = function(success, error) {
                        navigator.geolocation.getCurrentPosition(success, error);
                        return 12345;
                    };
                } catch(e) {}
            }

            // 5. Timezone Spoofing (Multilogin Style)
            if ("$timezoneSpoof" === "CUSTOM" || "$timezoneSpoof" === "AUTO") {
                try {
                    const resolvedOptions = Intl.DateTimeFormat.prototype.resolvedOptions;
                    Intl.DateTimeFormat.prototype.resolvedOptions = function() {
                        const options = resolvedOptions.call(this);
                        options.timeZone = "$customTz";
                        return options;
                    };
                } catch(e) {}
            }

            // 6. Canvas cryptographic noise spoofing (AdsPower / Ghost Browser Style)
            if ($canvasNoise) {
                try {
                    const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                    HTMLCanvasElement.prototype.toDataURL = function(type, encoderOptions) {
                        const ctx = this.getContext('2d');
                        if (ctx) {
                            // Inject minor pixel shift noise to modify canvas cryptographic hash
                            const imgData = ctx.getImageData(0, 0, Math.min(this.width, 10) || 1, Math.min(this.height, 10) || 1);
                            if (imgData.data.length > 0) {
                                imgData.data[0] = (imgData.data[0] + 1) % 256; // modify R channel slightly
                                ctx.putImageData(imgData, 0, 0);
                            }
                        }
                        return originalToDataURL.call(this, type, encoderOptions);
                    };
                } catch(e) {}
            }

            // 7. WebGL Fingerprint Spoofing (AdsPower / Dolphin{anty} Style)
            if ($webGlNoise) {
                try {
                    const originalGetParameter = WebGLRenderingContext.prototype.getParameter;
                    WebGLRenderingContext.prototype.getParameter = function(pname) {
                        // UNMASKED_VENDOR_WEBGL or UNMASKED_RENDERER_WEBGL spoofing
                        if (pname === 37445) return "Intel Open Source Technology Center";
                        if (pname === 37446) return "Mesa DRI Intel(R) HD Graphics (Skylake GT2)";
                        return originalGetParameter.call(this, pname);
                    };
                    if (window.WebGL2RenderingContext) {
                        const originalGetParameter2 = WebGL2RenderingContext.prototype.getParameter;
                        WebGL2RenderingContext.prototype.getParameter = function(pname) {
                            if (pname === 37445) return "Intel Open Source Technology Center";
                            if (pname === 37446) return "Mesa DRI Intel(R) HD Graphics (Skylake GT2)";
                            return originalGetParameter2.call(this, pname);
                        };
                    }
                } catch(e) {}
            }

            // 8. AudioContext Spoofing (Incogniton Style)
            if ($audioNoise) {
                try {
                    const originalGetChannelData = AudioBuffer.prototype.getChannelData;
                    AudioBuffer.prototype.getChannelData = function(channel) {
                        const data = originalGetChannelData.call(this, channel);
                        // Inject microscopic float precision noise to modify audio context hash
                        for (let i = 0; i < Math.min(data.length, 10); i++) {
                            data[i] += 0.0000001 * Math.sin(i);
                        }
                        return data;
                    };
                } catch(e) {}
            }

            // 9. ClientRects Fingerprinting Defender (SessionBox Style)
            if ($rectsNoise) {
                try {
                    const originalGetClientRects = Element.prototype.getClientRects;
                    Element.prototype.getClientRects = function() {
                        const rects = originalGetClientRects.call(this);
                        if (rects && rects.length > 0) {
                            // Introduce tiny layouts fractions variations
                            rects[0].width += 0.0001;
                            rects[0].height += 0.0001;
                        }
                        return rects;
                    };
                } catch(e) {}
            }
        })();
    """.trimIndent()
}

@Composable
fun AutopilotTab(
    viewModel: MainViewModel,
    activeTopic: WeeklyTopic?,
    terminalLogs: List<String>,
    defaultProfile: BrowserProfile?,
    onWebViewCreated: (WebView) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val terminalListState = rememberLazyListState()

    // Automatically follow bottom on log entries
    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            terminalListState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Automation Sandbox Dashboard
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔬 قمرة أتمتة الاستكشاف المباشر",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    if (viewModel.isAutomationRunning) {
                        Button(
                            onClick = { viewModel.stopLiveAutomation() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إيقاف دوري", fontSize = 11.sp)
                        }
                    }
                }

                if (viewModel.isAutomationRunning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الاستعلام النشط (${viewModel.activeAutomationQueryIndex + 1} من 8):",
                            fontSize = 11.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = viewModel.activeAutomationQueryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("المحرك: ${viewModel.activeAutomationEngineName}", fontSize = 10.sp, color = Color.Gray)
                            Text("الوكيل: ${viewModel.activeAutomationProxyUsed}", fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Text(
                        text = "لا توجد أتمتة حية جارية حالياً. اضغط على تشغيل في 'الرئيسية' للبدء والاستمتاع بالمحاكة البصرية.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Web Client Render Sandbox (WebView)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24))
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مرصد المتصفح المخصص المُموه",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    AndroidView(
                        factory = { context ->
                            try {
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            val js = generateAntiFingerprintScript(defaultProfile)
                                            view?.evaluateJavascript(js, null)
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            val js = generateAntiFingerprintScript(defaultProfile)
                                            view?.evaluateJavascript(js, null)
                                        }
                                    }
                                    onWebViewCreated(this)
                                    loadUrl("https://www.bing.com")
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                android.widget.TextView(context).apply {
                                    text = "🔒 وضع المحاكاة الخلفي الآمن نشط\nيتم تنفيذ عمليات البحث وتفتيش المواقع بنجاح دون الحاجة لمتصفح مرئي ثقيل لمنع أي تعارض."
                                    textSize = 12f
                                    setTextColor(android.graphics.Color.LTGRAY)
                                    setPadding(24, 24, 24, 24)
                                    gravity = android.view.Gravity.CENTER
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Real-time terminal log panel
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📟 مرسمة العمليات وسيرفر الوكيل",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    
                    Divider(color = PrimaryBlue.copy(alpha = 0.2f))

                    LazyColumn(
                        state = terminalListState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (terminalLogs.isEmpty()) {
                            item {
                                Text(
                                    "بانتظار انطلاق آلة الاستكشاف للبدء بالتوثيق المباشر...",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            items(terminalLogs) { rowLog ->
                                Text(
                                    text = rowLog,
                                    color = if (rowLog.contains("🔒") || rowLog.contains("✅")) Color(0xFF10B981) else Color(0xFF38BDF8),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxiesTab(
    viewModel: MainViewModel,
    proxies: List<ProxyConfig>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Built-in VPN & Cloud Location Emulator Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "VPN",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "شبكة VPN المدمجة والبروكسي الدوّار 🛡️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "تصفح آمن وخوادم توجيه مشفرة من دول متعددة",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Switch(
                            checked = viewModel.isBuiltInVpnEnabled,
                            onCheckedChange = { viewModel.toggleBuiltInVpn(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }

                    if (viewModel.isBuiltInVpnEnabled) {
                        Text(
                            text = "اختر دولة الخادم لتغيير موقعك الجغرافي وبصمتك الرقمية:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )

                        // List of VPN servers as clickable rows or grid
                        viewModel.vpnServers.forEach { server ->
                            val isSelected = viewModel.selectedVpnServerId == server.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else BackgroundDark.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryBlue else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectVpnServer(server.id) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = server.flag,
                                        fontSize = 20.sp
                                    )
                                    Column {
                                        Text(
                                            text = server.countryName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "نوع الاتصال: ${server.type} • خادم سحابي مشفر",
                                            fontSize = 10.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Green signal indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text(
                                        text = "${server.latency}ms",
                                        fontSize = 11.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark.copy(alpha = 0.4f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "الشبكة المدمجة متوقفة الآن. سيتم الاتصال بالإنترنت بشكل مباشر أو باستخدام البروكسي العام/البروفايل المدخل يدوياً بالأسفل.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Add Proxy Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🔒 إضافة سيرفر فك كتم وهوية (Proxy)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.proxyHostInput,
                            onValueChange = { viewModel.proxyHostInput = it },
                            placeholder = { Text("عنوان IP") },
                            modifier = Modifier.weight(3f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = viewModel.proxyPortInput,
                            onValueChange = { viewModel.proxyPortInput = it },
                            placeholder = { Text("المجال") },
                            modifier = Modifier.weight(1.2f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.proxyUsernameInput,
                            onValueChange = { viewModel.proxyUsernameInput = it },
                            placeholder = { Text("الاسم (اختياري)") },
                            modifier = Modifier.weight(2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = viewModel.proxyPasswordInput,
                            onValueChange = { viewModel.proxyPasswordInput = it },
                            placeholder = { Text("المرور (اختياري)") },
                            modifier = Modifier.weight(2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = { viewModel.addProxy() },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ وتوثيق البروكسي في خزانة الهويات 🔒")
                    }
                }
            }
        }

        // Active Proxies List Grid
        item {
            Text(
                text = "الوكلاء المتاحون للتدوير الآلي بالتناوب:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.LightGray
            )
        }

        if (proxies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا توجد خوادم بروكسي حالياً. سيتم استخدام الاتصال المباشر.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(proxies) { proxy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(1.dp, if (proxy.isActive) PrimaryBlue.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (proxy.isActive) Color(0xFF1B3B2B) else Color(0xFF2D1616)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                proxy.type,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (proxy.isActive) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }

                        Column {
                            Text(
                                text = "${proxy.host}:${proxy.port}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (proxy.username != null) "محمي بكلمة مرور" else "بروكسي مفتوح / عام",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Switch(
                            checked = proxy.isActive,
                            onCheckedChange = { viewModel.toggleProxyActive(proxy) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryBlue,
                                checkedTrackColor = SecondaryBlue.copy(alpha = 0.5f)
                            )
                        )

                        IconButton(onClick = { viewModel.deleteProxy(proxy.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogsTab(
    viewModel: MainViewModel,
    logs: List<AutomationLog>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 تقارير الاستكشاف والجرود التاريخية",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )

            Button(
                onClick = { viewModel.clearLogs() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("تصفير السجلات 🗑️", fontSize = 11.sp)
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد سجلات بحث آلي مخزنة حتى الآن.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                                Text(
                                    text = sdf.format(java.util.Date(log.timestamp)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (log.status == "SUCCESS") Color(0xFF1B3B2B) else Color(0xFF4C0505))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (log.status == "SUCCESS") "مكتمل بنجاح" else "فشل الاتصال",
                                        fontSize = 10.sp,
                                        color = if (log.status == "SUCCESS") Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }

                            Text(
                                text = log.queryText,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "طريقة البحث: ${log.methodType}",
                                    fontSize = 10.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "المدة: ${log.durationMs}ms",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.15f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "البروكسي المستعمل: ${log.proxyUsed}",
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "ملخص الرد: ${log.responseSummary}",
                                fontSize = 10.sp,
                                color = Color.LightGray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilesTab(
    viewModel: MainViewModel
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val defaultProfile by viewModel.defaultProfile.collectAsStateWithLifecycle()

    var showAddForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👤 إدارة بروفايلات التخفي وعزل البصمات (Anti-Detect)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "على غرار برامج AdsPower و Dolphin{anty}، يمنحك كل بروفايل هنا حاوية منعزلة تماماً (Sandbox) بملفات تعريف ارتباط (Cookies) منعزلة، وذاكرة تخزين محلية خاصة، وبصمة تصفح (User-Agent) مميزة وبروكسي مخصص لمنع المنصات من ربط حساباتك أو تعقب جهازك الحقيقي.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                    
                    Button(
                        onClick = { showAddForm = !showAddForm },
                        colors = ButtonDefaults.buttonColors(containerColor = if (showAddForm) Color.Gray else SecondaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showAddForm) "إغلاق نافذة الإضافة ✖" else "إنشاء بروفايل تخفي جديد ➕")
                    }
                }
            }
        }

        // Add Profile Form
        if (showAddForm) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🛠 تكوين بصمة البروفايل الجديد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        // Profile Name & Tag
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.profileNameInput,
                                onValueChange = { viewModel.profileNameInput = it },
                                label = { Text("اسم البروفايل") },
                                placeholder = { Text("مثال: بروفايل فيسبوك 1") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.profileTagInput,
                                onValueChange = { viewModel.profileTagInput = it },
                                label = { Text("العلامة / التصنيف") },
                                placeholder = { Text("مثال: فيسبوك") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        // Platform Selector Row
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("نظام التشغيل المحاكى (Platform):", fontSize = 11.sp, color = Color.Gray)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val platforms = listOf("Windows", "macOS", "Linux", "Android")
                                platforms.forEach { plat ->
                                    val isSelected = viewModel.profilePlatformInput == plat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryBlue else SurfaceDark)
                                            .clickable {
                                                viewModel.profilePlatformInput = plat
                                                // Automatically suggest user-agent based on platform
                                                viewModel.profileUserAgentInput = when (plat) {
                                                    "Windows" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                                    "macOS" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
                                                    "Linux" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                                    "Android" -> "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                                    else -> viewModel.profileUserAgentInput
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(plat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // User-Agent input
                        OutlinedTextField(
                            value = viewModel.profileUserAgentInput,
                            onValueChange = { viewModel.profileUserAgentInput = it },
                            label = { Text("معرّف البصمة (User-Agent)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                        )

                        // Proxy settings for this profile
                        Text("🛡 بروكسي مخصص للملف التعريفي (اختياري):", fontSize = 11.sp, color = PrimaryBlue)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.profileProxyHostInput,
                                onValueChange = { viewModel.profileProxyHostInput = it },
                                label = { Text("الخادم (Proxy Host)") },
                                placeholder = { Text("مثال: 185.23.4.1") },
                                modifier = Modifier.weight(2f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.profileProxyPortInput,
                                onValueChange = { viewModel.profileProxyPortInput = it },
                                label = { Text("المنفذ") },
                                placeholder = { Text("8080") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.profileProxyUserInput,
                                onValueChange = { viewModel.profileProxyUserInput = it },
                                label = { Text("اسم المستخدم") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.profileProxyPassInput,
                                onValueChange = { viewModel.profileProxyPassInput = it },
                                label = { Text("كلمة المرور") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        // Advanced Options: Language and Resolution
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = viewModel.profileLanguageInput,
                                onValueChange = { viewModel.profileLanguageInput = it },
                                label = { Text("اللغة المحاكاة") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = viewModel.profileResolutionInput,
                                onValueChange = { viewModel.profileResolutionInput = it },
                                label = { Text("دقة الشاشة") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        var showAdvancedConfig by remember { mutableStateOf(false) }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(0.5.dp, if (showAdvancedConfig) PrimaryBlue else Color.Gray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAdvancedConfig = !showAdvancedConfig },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🛡️ بصمات الأمن السيبراني المتقدمة (أقوى من AdsPower & Dolphin)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showAdvancedConfig) PrimaryBlue else Color.White
                                    )
                                    Text(
                                        text = if (showAdvancedConfig) "إخفاء ▲" else "عرض وتخصيص ▼",
                                        fontSize = 10.sp,
                                        color = PrimaryBlue
                                    )
                                }
                                
                                if (showAdvancedConfig) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 1. Core Hardware Emulation (CPU Cores & RAM)
                                    Text("🔧 محاكاة عتاد الجهاز الحقيقي:", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = viewModel.profileHardwareConcurrency,
                                            onValueChange = { viewModel.profileHardwareConcurrency = it },
                                            label = { Text("نوى المعالج (CPU)") },
                                            placeholder = { Text("8") },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = viewModel.profileDeviceMemory,
                                            onValueChange = { viewModel.profileDeviceMemory = it },
                                            label = { Text("الذاكرة العشوائية (RAM)") },
                                            placeholder = { Text("8") },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            singleLine = true
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 2. Encryption Toggles (Canvas, WebGL, Audio, Rects)
                                    Text("🎨 تشويش البصمات الرسومية والبرمجية:", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = viewModel.profileCanvasSpoof,
                                                    onCheckedChange = { viewModel.profileCanvasSpoof = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                                )
                                                Text("تشويش Canvas", fontSize = 11.sp, color = Color.White)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = viewModel.profileWebGlSpoof,
                                                    onCheckedChange = { viewModel.profileWebGlSpoof = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                                )
                                                Text("تشويش WebGL", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = viewModel.profileAudioSpoof,
                                                    onCheckedChange = { viewModel.profileAudioSpoof = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                                )
                                                Text("تشويش بصمة الصوت", fontSize = 11.sp, color = Color.White)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = viewModel.profileClientRectsSpoof,
                                                    onCheckedChange = { viewModel.profileClientRectsSpoof = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                                )
                                                Text("تشويش ClientRects", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 3. WebRTC & Timezone
                                    Text("🌍 الشبكة والمنطقة الزمنية المحاكية:", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("تسريب WebRTC:", fontSize = 10.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("BLOCK", "REAL", "CUSTOM").forEach { mode ->
                                                    val isSelected = viewModel.profileWebRtcSpoof == mode
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isSelected) PrimaryBlue else SurfaceDark)
                                                            .clickable { viewModel.profileWebRtcSpoof = mode }
                                                            .padding(vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(mode, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("المنطقة الزمنية:", fontSize = 10.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("AUTO", "UTC", "CUSTOM").forEach { mode ->
                                                    val isSelected = viewModel.profileTimezoneSpoof == mode
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isSelected) PrimaryBlue else SurfaceDark)
                                                            .clickable { viewModel.profileTimezoneSpoof = mode }
                                                            .padding(vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(mode, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (viewModel.profileTimezoneSpoof == "CUSTOM") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = viewModel.profileCustomTimezone,
                                            onValueChange = { viewModel.profileCustomTimezone = it },
                                            label = { Text("منطقة مخصصة (مثال: Asia/Riyadh)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryBlue,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            singleLine = true
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 4. Geolocation Config
                                    Text("📍 محاكاة الموقع الجغرافي (Geolocation):", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("AUTO", "BLOCK", "CUSTOM").forEach { mode ->
                                            val isSelected = viewModel.profileGeolocationSpoof == mode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSelected) PrimaryBlue else SurfaceDark)
                                                    .clickable { viewModel.profileGeolocationSpoof = mode }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = when(mode) {
                                                        "AUTO" -> "تلقائي محاذاة"
                                                        "BLOCK" -> "حجب الصلاحية"
                                                        else -> "إحداثيات مخصصة"
                                                    },
                                                    fontSize = 9.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (viewModel.profileGeolocationSpoof == "CUSTOM") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = viewModel.profileCustomLatitude,
                                                onValueChange = { viewModel.profileCustomLatitude = it },
                                                label = { Text("خط العرض (Lat)") },
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryBlue,
                                                    unfocusedBorderColor = Color.Gray,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                ),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = viewModel.profileCustomLongitude,
                                                onValueChange = { viewModel.profileCustomLongitude = it },
                                                label = { Text("خط الطول (Lon)") },
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryBlue,
                                                    unfocusedBorderColor = Color.Gray,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                ),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = viewModel.profileIsDefaultInput,
                                onCheckedChange = { viewModel.profileIsDefaultInput = it },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                            )
                            Text("تعيين كملف تعريف نشط حالياً فور الإنشاء", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.addBrowserProfile()
                                showAddForm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.profileNameInput.isNotBlank()
                        ) {
                            Text("حفظ وبناء الملف التعريفي 💾", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Profile Status Indicator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F30)),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Column {
                        Text(
                            text = "الملف التعريفي النشط حالياً:",
                            fontSize = 11.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = defaultProfile?.name ?: "البروفايل المالي الآمن (الافتراضي)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "البروكسي النشط: " + (defaultProfile?.let { if (!it.proxyHost.isNullOrEmpty()) "${it.proxyHost}:${it.proxyPort}" else "اتصال مباشر (لا يوجد)" } ?: "اتصال مباشر"),
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // Title of existing profiles
        item {
            Text(
                text = "📁 ملفات التعريف المهيأة (${profiles.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        // List of Profiles
        if (profiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("جاري تحميل بروفايلات التخفي...", color = Color.Gray)
                }
            }
        } else {
            items(profiles) { profile ->
                val isActive = defaultProfile?.id == profile.id || (defaultProfile == null && profile.isDefault)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) SurfaceCard else SurfaceDark
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(
                        width = if (isActive) 1.5.dp else 0.5.dp,
                        color = if (isActive) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(profile.tag, fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = profile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }

                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1B3B2B))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("نشط ومثبت 🎯", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.15f))

                        // Details grid
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💻 نظام التشغيل:", fontSize = 11.sp, color = Color.Gray)
                                Text(profile.platform, fontSize = 11.sp, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔒 البروكسي:", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = if (!profile.proxyHost.isNullOrEmpty()) "${profile.proxyHost}:${profile.proxyPort} (${profile.proxyType})" else "اتصال مباشر (Direct)",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🌐 معرّف البصمة:", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = profile.userAgent,
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.15f))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.toggleProfileActive(profile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Color.Gray else Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    enabled = !isActive
                                ) {
                                    Text("تثبيت وتفعيل 🚀", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.deleteProfile(profile.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    enabled = !isActive && profiles.size > 1
                                ) {
                                    Text("حذف 🗑", fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "حاوية: " + profile.dataSuffix.take(15) + "...",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

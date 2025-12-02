package org.example.project

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Geometry constants
const val PLATFORM_RADIUS = 200f
const val HORSE_ORBIT_RADIUS = 175f
const val HORSE_WIDTH = 30f
const val HORSE_HEIGHT = 10f
const val HORSE_COUNT = 8

// Color palette for horses
fun horseColor(index: Int): Color {
    val palette = listOf(
        Color.Red, Color(0xFFFFA500), // Orange
        Color.Yellow, Color.Green,
        Color.Cyan, Color.Blue,
        Color.Magenta, Color.Gray
    )
    return palette[index % palette.size]
}

@Composable
fun CarouselApp() {
    val revolutions = remember { mutableStateOf(1) }
    val isRunning = remember { mutableStateOf(false) }
    val angle = remember { Animatable(0f) }
    val completedRevolutions = remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun startRide() {
        scope.launch {
            isRunning.value = true
            angle.snapTo(0f)
            completedRevolutions.value = 0
            val targetAngle = angle.value + revolutions.value * 360f

            angle.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = (7500 * revolutions.value),
                    easing = LinearOutSlowInEasing
                )
            )
            isRunning.value = false
        }
    }

    fun stopRide() {
        scope.launch {
            angle.stop()
            completedRevolutions.value = 0
            isRunning.value = false
        }
    }

    LaunchedEffect(angle.value) {
        completedRevolutions.value = (angle.value / 360f).toInt()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment =
            androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
            Canvas(modifier = Modifier.size(500.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Platform circle
                drawCircle(
                    color = Color.LightGray,
                    radius = PLATFORM_RADIUS,
                    center = center
                )

                // Hub
                withTransform({
                    rotate(
                        degrees = angle.value,
                        pivot = center
                    )
                }) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(center.x - 5f, center.y - 5f),
                        size = Size(10f, 10f)
                    )
                }

                // Horses
                val stepDeg = 360f / HORSE_COUNT

                repeat(HORSE_COUNT) { i ->
                    // Angle of this horse on the orbit,
                    // including carousel rotation
                    val thetaDeg = i * stepDeg + angle.value
                    val thetaRad = thetaDeg * PI / 180.0

                    // Horse center on the orbit
                    val cx = center.x +
                            (HORSE_ORBIT_RADIUS * cos(thetaRad)).toFloat()
                    val cy = center.y +
                            (HORSE_ORBIT_RADIUS * sin(thetaRad)).toFloat()

                    // Tangent: perpendicular to radius
                    val orientationDeg = thetaDeg + 90f

                    withTransform({
                        // Move local origin to the horse center
                        translate(cx, cy)

                        // Rotate the horse about its center
                        // so that its long side is tangent to the orbit
                        rotate(orientationDeg, pivot = Offset.Zero)
                    }) {
                        // Draw a rectangle centered at (0, 0)
                        drawRect(
                            color = horseColor(i),
                            topLeft = Offset(-HORSE_WIDTH / 2f,
                                -HORSE_HEIGHT / 2f),
                            size = Size(HORSE_WIDTH, HORSE_HEIGHT)
                        )
                    }
                }
            }

        ControlPanel(
            revolutions = revolutions,
            isRunning = isRunning,
            onStart = { startRide() },
            onStop = { stopRide() }
        )

        InfoPanel(
            isRunning = isRunning.value,
            completedRevolutions = completedRevolutions.value
        )
    }
}

@Composable
fun ControlPanel(
    revolutions: MutableState<Int>,
    isRunning: MutableState<Boolean>,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(horizontalAlignment =
        androidx.compose.ui.Alignment.CenterHorizontally) {
        Button(onClick = {
            isRunning.value = true
            onStart()
        }) {
            Text("Start Ride")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                isRunning.value = false
                onStop()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Emergency Stop", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Revolutions: ${revolutions.value}")
        Slider(
            value = revolutions.value.toFloat(),
            onValueChange = { revolutions.value = it.toInt() },
            valueRange = 1f..4f,
            steps = 2,
            modifier = Modifier.width(300.dp)
        )
    }
}

@Composable
fun InfoPanel(
    isRunning: Boolean,
    completedRevolutions: Int
) {
    Column(horizontalAlignment =
        androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = if (isRunning) "Status: Ride in progress"
                        else "Status: Stopped",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Revolutions: $completedRevolutions",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

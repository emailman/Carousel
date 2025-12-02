package org.example.project

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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

// Data class for horse position (angle in degrees)
data class HorsePosition(val x: Float, val y: Float, val angleDeg: Float)

// Compute position of one horse
fun horsePosition(index: Int, centerX: Float, centerY: Float):
        HorsePosition {
    val angleStep = 360.0 / HORSE_COUNT
    val thetaDeg = index * angleStep
    val thetaRad = thetaDeg * PI / 180.0   // convert degrees to radians

    val x = (centerX + HORSE_ORBIT_RADIUS * cos(thetaRad)).toFloat()
    val y = (centerY + HORSE_ORBIT_RADIUS * sin(thetaRad)).toFloat()

    val orientationDeg = thetaDeg.toFloat() + 90f
    return HorsePosition(x, y, orientationDeg)
}

// Compute all horse positions
fun allHorsePositions(centerX: Float, centerY: Float): List<HorsePosition> {
    return (0 until HORSE_COUNT).map { horsePosition(it, centerX, centerY) }
}

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

// Drawing helper for horses
fun DrawScope.drawHorse(horse: HorsePosition,
                        color: Color,
                        globalRotationDeg: Float) {
    withTransform({
        translate(horse.x, horse.y)
        rotate(horse.angleDeg) // remove globalRotationDeg from here
    }) {
        drawRect(
            color = color,
            topLeft = Offset(-HORSE_WIDTH / 2, -HORSE_HEIGHT / 2),
            size = Size(HORSE_WIDTH, HORSE_HEIGHT)
        )
    }
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
            completedRevolutions.value = 0
            val targetAngle = angle.value + revolutions.value * 360f

            angle.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = (5000 * revolutions.value),
                    easing = LinearEasing
                )
            )
            isRunning.value = false
        }
    }

    fun stopRide() {
        scope.launch {
            angle.stop()
            isRunning.value = false
        }
    }

    LaunchedEffect(angle.value) {
        completedRevolutions.value = (angle.value / 360f).toInt()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
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

            // Rotating hub
            withTransform({
                translate(center.x, center.y)
                rotate(angle.value)
            }) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(-5f, -5f),
                    size = Size(10f, 10f)
                )
            }

            // Horses: include global rotation in theta, then do a single local transform
            repeat(HORSE_COUNT) { i ->
                val stepDeg = 360f / HORSE_COUNT
                val horseBaseDeg = i * stepDeg
                val totalDeg = horseBaseDeg + angle.value    // carousel rotation added here

                val thetaRad = totalDeg * PI / 180.0
                val relX = (HORSE_ORBIT_RADIUS * cos(thetaRad)).toFloat()
                val relY = (HORSE_ORBIT_RADIUS * sin(thetaRad)).toFloat()

                // Tangent orientation
                val orientationDeg = totalDeg + 90f

                withTransform({
                    // Move to this horse’s absolute position
                    translate(center.x + relX, center.y + relY)

                    // Rotate the horse so it faces along the orbit
                    // rotate(orientationDeg)
                }) {
                    drawRect(
                        color = horseColor(i),
                        topLeft = Offset(-HORSE_WIDTH / 2, -HORSE_HEIGHT / 2),
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
            completedRevolutions = completedRevolutions.value,
            totalRevolutions = revolutions.value
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
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
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
    completedRevolutions: Int,
    totalRevolutions: Int
) {
    Column(horizontalAlignment =
        androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = if (isRunning) "Ride in progress" else "Stopped",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Revolutions: $completedRevolutions / $totalRevolutions",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

package com.example.nutritiontracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private val Context.stepsDataStore by preferencesDataStore(name = "steps_tracker")

private object StepsKeys {
    val BASELINE_DATE = stringPreferencesKey("baseline_date")
    val BASELINE_STEPS = longPreferencesKey("baseline_steps")
}

/**
 * The device's step counter sensor reports a cumulative total since the last reboot,
 * not since midnight. This repository stores a daily "baseline" reading, so that
 * (current sensor value - today's baseline) gives us today's actual step count.
 */
class StepsRepository(private val context: Context) {

    suspend fun getTodayStepsFromSensorTotal(sensorTotalSteps: Long): Long {
        val today = todayDateString()
        val prefs = context.stepsDataStore.data.first()
        val baselineDate = prefs[StepsKeys.BASELINE_DATE]
        val baselineSteps = prefs[StepsKeys.BASELINE_STEPS] ?: 0L

        // Start a fresh baseline if it's a new day, or if the sensor value is lower than
        // our stored baseline (which happens if the phone rebooted since we last saved it).
        if (baselineDate != today || sensorTotalSteps < baselineSteps) {
            context.stepsDataStore.edit { prefs ->
                prefs[StepsKeys.BASELINE_DATE] = today
                prefs[StepsKeys.BASELINE_STEPS] = sensorTotalSteps
            }
            return 0L
        }

        return sensorTotalSteps - baselineSteps
    }

    /**
     * Takes a single reading from the step counter sensor and waits (up to 3s) for the
     * first callback. Used by the widget, which runs briefly and can't keep a listener
     * registered like the app screen does.
     */
    private suspend fun getCurrentRawStepCount(): Long? = withTimeoutOrNull(3000) {
        suspendCancellableCoroutine { cont ->
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

            if (stepSensor == null) {
                cont.resume(null, null)
                return@suspendCancellableCoroutine
            }

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sensorManager.unregisterListener(this)
                    if (cont.isActive) cont.resume(event.values[0].toLong(), null)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
        }
    }

    /** One-shot read of today's step count, for use in short-lived contexts like the widget. */
    suspend fun getTodaySteps(): Long {
        val raw = getCurrentRawStepCount() ?: return 0L
        return getTodayStepsFromSensorTotal(raw)
    }
}

/**
 * Listens to the device's step counter sensor while [enabled] is true, and returns
 * today's step count (0 if disabled, unsupported, or no data yet). Used on the app's
 * home screen for a live-updating view while the screen is open.
 */
@Composable
fun rememberTodaySteps(enabled: Boolean): Long {
    val context = LocalContext.current
    val repository = remember { StepsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var todaySteps by remember { mutableStateOf(0L) }

    DisposableEffect(enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val total = event.values[0].toLong()
                scope.launch {
                    todaySteps = repository.getTodayStepsFromSensorTotal(total)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (stepSensor != null) {
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return todaySteps
}
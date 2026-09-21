package com.example.nutritiontracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import kotlin.math.roundToLong

// Default step/distance goal in km. The user can change it under Nutrition Goals; this is the
// starting value and the fallback if the saved value is missing or invalid.
const val DAILY_KM_TARGET = 5.0f

// Average distance covered per step, used to estimate km walked from the step count,
// since the device sensor only reports steps, not distance directly.
const val STRIDE_LENGTH_KM = 0.000762f

// If the reading just before midnight is older than this, the steps in between could have been
// walked at any time, so we don't try to estimate the midnight total (see estimateMidnightTotal).
private const val MAX_MIDNIGHT_GAP_MS = 45 * 60 * 1000L

private val Context.stepsDataStore by preferencesDataStore(name = "steps_tracker")

private object StepsKeys {
    val BASELINE_DATE = stringPreferencesKey("baseline_date")
    val BASELINE_STEPS = longPreferencesKey("baseline_steps")

    // Last step count we successfully calculated, so the widget has something real to show
    // when it can't get a fresh sensor reading.
    val LAST_KNOWN_DATE = stringPreferencesKey("last_known_date")
    val LAST_KNOWN_STEPS = longPreferencesKey("last_known_steps")

    // Steps already counted today before the phone last restarted (the sensor starts from zero
    // again after a restart), and the boot number we saw when we last saved, to notice restarts.
    val CARRIED_STEPS = longPreferencesKey("carried_steps")
    val BOOT_COUNT = intPreferencesKey("boot_count")

    // The most recent sensor reading and when it was taken. At midnight this lets us work out
    // what the sensor total was at 00:00, so a walk that crosses midnight is split correctly.
    val PREV_READ_TIME = longPreferencesKey("prev_read_time")
    val PREV_READ_RAW = longPreferencesKey("prev_read_raw")
}

// Readings come from the app screen, the widget and the background worker, which can overlap.
// They must be applied one at a time, in order, so this lock is shared by the whole app.
private val stepsMutex = Mutex()

/**
 * The device's step counter sensor reports a cumulative total since the last reboot,
 * not since midnight. This repository stores a daily "baseline" reading, so that
 * (current sensor value - today's baseline) gives us today's actual step count. If the phone
 * restarts during the day, the sensor starts from zero again, so the steps counted so far are
 * kept as "carried" steps and added on top.
 */
class StepsRepository(private val context: Context) {

    /**
     * Turns a raw sensor total into today's step count and keeps the saved state up to date.
     *
     * Today's steps = carried steps (counted before a restart) + (sensor total - baseline).
     * - New day: the baseline becomes the sensor total at midnight, estimated from the last
     *   reading before midnight (see estimateMidnightTotal). Without an estimate it is the
     *   current sensor total, and the day starts at 0.
     * - Restart: the sensor counts from zero again, so everything it reports now was walked after
     *   the restart. The steps counted so far today become the carried steps and the baseline
     *   becomes 0. Only the minutes between the last reading and the restart are lost.
     */
    suspend fun getTodayStepsFromSensorTotal(sensorTotalSteps: Long): Long = stepsMutex.withLock {
        val today = todayDateString()
        val bootCount = currentBootCount()
        val prefs = context.stepsDataStore.data.first()

        val baselineDate = prefs[StepsKeys.BASELINE_DATE]
        val baselineSteps = prefs[StepsKeys.BASELINE_STEPS] ?: 0L
        val carriedSteps = prefs[StepsKeys.CARRIED_STEPS] ?: 0L
        val savedBootCount = prefs[StepsKeys.BOOT_COUNT]
        val prevReadTime = prefs[StepsKeys.PREV_READ_TIME]
        val prevReadRaw = prefs[StepsKeys.PREV_READ_RAW]
        val now = System.currentTimeMillis()
        val lastKnownToday = if (prefs[StepsKeys.LAST_KNOWN_DATE] == today) {
            prefs[StepsKeys.LAST_KNOWN_STEPS] ?: 0L
        } else {
            0L
        }

        // New day: count from midnight if we can estimate the total at that moment, otherwise
        // from the current reading.
        if (baselineDate != today) {
            // A restart between the two readings makes the totals incomparable.
            val sameBoot = bootCount == null || savedBootCount == null || bootCount == savedBootCount
            val midnightTotal = if (sameBoot) {
                estimateMidnightTotal(prevReadTime, prevReadRaw, now, sensorTotalSteps)
            } else {
                null
            }
            val baseline = midnightTotal ?: sensorTotalSteps
            val stepsSoFar = (sensorTotalSteps - baseline).coerceAtLeast(0L)

            context.stepsDataStore.edit { p ->
                p[StepsKeys.BASELINE_DATE] = today
                p[StepsKeys.BASELINE_STEPS] = baseline
                p[StepsKeys.CARRIED_STEPS] = 0L
                p[StepsKeys.LAST_KNOWN_DATE] = today
                p[StepsKeys.LAST_KNOWN_STEPS] = stepsSoFar
                p[StepsKeys.PREV_READ_TIME] = now
                p[StepsKeys.PREV_READ_RAW] = sensorTotalSteps
                if (bootCount != null) p[StepsKeys.BOOT_COUNT] = bootCount
            }
            return@withLock stepsSoFar
        }

        val restarted = if (bootCount != null && savedBootCount != null) {
            bootCount != savedBootCount
        } else {
            // No boot number to compare with (older saved data, or an unusual device), so fall
            // back to noticing that the sensor total went down.
            sensorTotalSteps < baselineSteps
        }

        val (newBaseline, newCarried) = if (restarted) {
            0L to lastKnownToday
        } else {
            baselineSteps to carriedSteps
        }
        val steps = newCarried + (sensorTotalSteps - newBaseline).coerceAtLeast(0L)

        // Always save the latest reading and its time, since the next midnight needs the last
        // reading before it.
        context.stepsDataStore.edit { p ->
            p[StepsKeys.BASELINE_STEPS] = newBaseline
            p[StepsKeys.CARRIED_STEPS] = newCarried
            p[StepsKeys.LAST_KNOWN_DATE] = today
            p[StepsKeys.LAST_KNOWN_STEPS] = steps
            p[StepsKeys.PREV_READ_TIME] = now
            p[StepsKeys.PREV_READ_RAW] = sensorTotalSteps
            if (bootCount != null) p[StepsKeys.BOOT_COUNT] = bootCount
        }
        steps
    }

    /**
     * Estimates what the sensor total was at exactly 00:00, from the last reading before
     * midnight and the first one after it, assuming the steps in between were spread evenly over
     * that time. That is close to true when readings are only minutes apart, for example during
     * a walk that crosses midnight, and it lets those first minutes of the new day be counted.
     *
     * Returns null when there is nothing trustworthy to base an estimate on: no earlier reading,
     * a gap too long to assume an even pace, or values that don't fit together. The caller then
     * starts the day at the current reading instead of inventing steps.
     */
    private fun estimateMidnightTotal(
        prevTime: Long?,
        prevTotal: Long?,
        nowTime: Long,
        nowTotal: Long
    ): Long? {
        if (prevTime == null || prevTotal == null) return null

        val midnight = Calendar.getInstance().apply {
            timeInMillis = nowTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val gap = nowTime - prevTime
        if (prevTime >= midnight || gap <= 0L || gap > MAX_MIDNIGHT_GAP_MS || nowTotal < prevTotal) {
            return null
        }

        val fractionBeforeMidnight = (midnight - prevTime).toDouble() / gap
        return prevTotal + ((nowTotal - prevTotal) * fractionBeforeMidnight).roundToLong()
    }

    /** How many times the phone has started since a factory reset. Changes on every restart. */
    private fun currentBootCount(): Int? = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    } catch (e: Settings.SettingNotFoundException) {
        null
    }

    /**
     * Takes a single reading from the step counter sensor and waits (up to 5s) for the
     * first callback. Used by the widget, which runs briefly and can't keep a listener
     * registered like the app screen does.
     */
    private suspend fun getCurrentRawStepCount(): Long? = withTimeoutOrNull(5000) {
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

            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
        }
    }

    /**
     * One-shot read of today's step count, for use in short-lived contexts like the widget.
     *
     * The step counter only reports when the step count changes, so a quick one-shot read often
     * gets no callback at all while the app is closed. Returning 0 in that case made the widget
     * show "0.0 km" until the app was opened. Instead we fall back to the last count we knew.
     */
    suspend fun getTodaySteps(): Long {
        val raw = getCurrentRawStepCount()
        if (raw != null) return getTodayStepsFromSensorTotal(raw)

        val prefs = context.stepsDataStore.data.first()
        return if (prefs[StepsKeys.LAST_KNOWN_DATE] == todayDateString()) {
            prefs[StepsKeys.LAST_KNOWN_STEPS] ?: 0L
        } else {
            0L
        }
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
// Copyright 2025-2026 Citra Emulator Project / Azahar Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the misc/licenses/gplv2.txt file included.

package org.citra.citra_emu.utils

import android.widget.Toast
import org.citra.citra_emu.CitraApplication
import org.citra.citra_emu.NativeLibrary
import org.citra.citra_emu.R
import org.citra.citra_emu.features.settings.model.IntSetting

/**
 * Owns the core's temporary frame limit, which is the single slot both speed modifiers use:
 * Turbo raises it above 100% and slow motion lowers it below 100%, so only one can be latched at
 * a time.
 */
object TurboHelper {
    /** Speed of the slow motion latch, as a percentage of normal speed. */
    private const val SLOW_MOTION_FRAME_LIMIT = 25.0

    private var turboSpeedEnabled = false
    private var slowMotionEnabled = false

    fun isTurboSpeedEnabled(): Boolean = turboSpeedEnabled

    fun isSlowMotionEnabled(): Boolean = slowMotionEnabled

    fun reloadTurbo(showToast: Boolean) {
        val context = CitraApplication.appContext
        val toastMessage: String

        if (slowMotionEnabled) {
            // Slow motion owns the temporary frame limit until it is switched off again.
            NativeLibrary.setTemporaryFrameLimit(SLOW_MOTION_FRAME_LIMIT)
            return
        }

        if (turboSpeedEnabled) {
            NativeLibrary.setTemporaryFrameLimit(IntSetting.TURBO_LIMIT.int.toDouble())
            toastMessage = context.getString(R.string.turbo_enabled_toast)
        } else {
            NativeLibrary.disableTemporaryFrameLimit()
            toastMessage = context.getString(R.string.turbo_disabled_toast)
        }

        if (showToast) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun setTurboEnabled(state: Boolean, showToast: Boolean) {
        turboSpeedEnabled = state
        if (state) {
            slowMotionEnabled = false
        }
        reloadTurbo(showToast)
    }

    fun toggleTurbo(showToast: Boolean) {
        setTurboEnabled(!TurboHelper.isTurboSpeedEnabled(), showToast)
    }

    /**
     * Latches slow motion: the emulation runs at [SLOW_MOTION_FRAME_LIMIT] percent of normal
     * speed until it is called again with `false`. Turning it on stops Turbo, since both share
     * the same temporary frame limit.
     *
     * Like Turbo, this only has an effect while speed limiting is enabled in the settings.
     */
    fun setSlowMotionEnabled(state: Boolean, showToast: Boolean) {
        slowMotionEnabled = state
        if (state) {
            turboSpeedEnabled = false
            NativeLibrary.setTemporaryFrameLimit(SLOW_MOTION_FRAME_LIMIT)
        } else {
            NativeLibrary.disableTemporaryFrameLimit()
        }

        if (showToast) {
            val message = if (state) {
                R.string.slow_motion_enabled_toast
            } else {
                R.string.slow_motion_disabled_toast
            }
            Toast.makeText(CitraApplication.appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleSlowMotion(showToast: Boolean) {
        setSlowMotionEnabled(!slowMotionEnabled, showToast)
    }
}

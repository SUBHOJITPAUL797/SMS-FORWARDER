package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.R
import kotlin.math.abs

object FloatingOtpManager {

    private const val TAG = "FloatingOtpManager"
    private const val AUTO_DISMISS_DELAY_MS = 15_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentOverlayView: View? = null
    private var autoDismissRunnable: Runnable? = null

    /**
     * Checks whether the SYSTEM_ALERT_WINDOW permission is granted.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Opens Android's system "Display over other apps" settings page for this app.
     */
    fun openOverlaySettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open overlay settings with package URI, falling back", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Could not open overlay settings", ex)
            }
        }
    }

    /**
     * Displays the Truecaller-styled floating OTP card on top of other apps.
     */
    fun showFloatingOtp(
        context: Context,
        sender: String,
        otp: String,
        formattedOtp: String,
        timeString: String = "SMS · Just now"
    ) {
        if (!canDrawOverlays(context)) {
            Log.d(TAG, "Cannot show floating OTP: overlay permission not granted")
            return
        }

        mainHandler.post {
            try {
                // Synchronously clean up any prior overlay on the main thread
                dismissInternal()

                val appContext = context.applicationContext
                val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    ?: return@post

                val inflater = LayoutInflater.from(appContext)
                val view = inflater.inflate(R.layout.layout_floating_otp, null)

                val tvInitial = view.findViewById<TextView>(R.id.tv_floating_initial)
                val tvSender = view.findViewById<TextView>(R.id.tv_floating_sender)
                val tvTime = view.findViewById<TextView>(R.id.tv_floating_time)
                val tvOtp = view.findViewById<TextView>(R.id.tv_floating_otp)
                val btnClose = view.findViewById<ImageView>(R.id.btn_floating_close)
                val btnCopy = view.findViewById<View>(R.id.btn_floating_copy)
                val rootLayout = view.findViewById<View>(R.id.floating_root)
                val cardLayout = view.findViewById<View>(R.id.floating_card)

                // 1. Configure Header & Sender details
                val cleanSender = sender.ifBlank { "SMS Bridge" }
                tvSender.text = cleanSender
                tvInitial.text = cleanSender.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "S"

                val formattedTime = if (timeString.startsWith("SMS")) timeString else "SMS · $timeString"
                tvTime.text = formattedTime

                // 2. Configure OTP display
                tvOtp.text = formattedOtp.ifBlank { otp }

                // 3. Actions
                btnClose.setOnClickListener {
                    dismiss()
                }

                btnCopy.setOnClickListener {
                    copyToClipboard(appContext, otp)
                    showToast(appContext, "Copied OTP: $otp")
                    dismiss()
                }

                // Tapping outside the card inside the floating root dismisses
                rootLayout.setOnClickListener {
                    dismiss()
                }

                // Prevent card clicks from triggering rootLayout dismiss
                cardLayout.setOnClickListener {
                    // Consume click
                }

                // Swipe-up gesture to dismiss smoothly
                val gestureDetector = GestureDetector(appContext, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        if (e1 != null && (e1.y - e2.y) > 60 && abs(velocityY) > 150) {
                            dismiss()
                            return true
                        }
                        return false
                    }
                })

                cardLayout.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    false // Return false so click listeners on buttons still fire
                }

                // Dismiss on outside touch (when touching completely outside the window frame)
                view.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        dismiss()
                        true
                    } else {
                        false
                    }
                }

                // 4. WindowManager Layout Parameters
                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val density = appContext.resources.displayMetrics.density
                val topMarginPx = (36 * density).toInt()

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = topMarginPx
                    windowAnimations = android.R.style.Animation_Dialog
                }

                windowManager.addView(view, params)
                currentOverlayView = view

                // Auto-dismiss after 15 seconds
                val runnable = Runnable { dismiss() }
                autoDismissRunnable = runnable
                mainHandler.postDelayed(runnable, AUTO_DISMISS_DELAY_MS)

                Log.d(TAG, "Floating OTP overlay successfully shown for sender=$cleanSender, otp=$otp")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying floating OTP window", e)
            }
        }
    }

    /**
     * Safely dismisses the currently showing floating window.
     */
    fun dismiss() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissInternal()
        } else {
            mainHandler.post { dismissInternal() }
        }
    }

    private fun dismissInternal() {
        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        autoDismissRunnable = null

        currentOverlayView?.let { view ->
            try {
                val windowManager = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                } else {
                    windowManager?.removeView(view)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error removing floating overlay view", e)
            } finally {
                currentOverlayView = null
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("OTP", text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

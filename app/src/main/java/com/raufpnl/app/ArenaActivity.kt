package com.raufpnl.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.hypot
import kotlin.random.Random

class ArenaActivity : Activity() {
    private val dark = Color.rgb(9, 9, 12)
    private val red = Color.rgb(255, 52, 79)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark

        val prefs = getSharedPreferences("rauf_pnl", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(dark)
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = TextView(this).apply {
            text = "‹  BACK"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(dp(8), dp(12), dp(16), dp(12))
            setOnClickListener { finish() }
        }
        header.addView(back)
        val title = TextView(this).apply {
            text = "ARENA PRACTICE"
            textSize = 16f
            setTextColor(red)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        header.addView(title)
        root.addView(header)

        val settingRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val label = TextView(this).apply {
            text = "TOUCH TARGET  ${prefs.getInt("arena_target_size", 56)} dp"
            textSize = 11f
            setTextColor(Color.LTGRAY)
        }
        settingRow.addView(label, LinearLayout.LayoutParams(dp(126), -2))
        val size = prefs.getInt("arena_target_size", 56).coerceIn(28, 88)
        val slider = SeekBar(this).apply {
            max = 60
            progress = size - 28
            progressTintList = android.content.res.ColorStateList.valueOf(red)
            thumbTintList = android.content.res.ColorStateList.valueOf(red)
        }
        settingRow.addView(slider, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(settingRow)

        val game = ArenaView(this, prefs, size)
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 28
                label.text = "TOUCH TARGET  $value dp"
                game.setTargetSize(value)
                prefs.edit().putInt("arena_target_size", value).apply()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        root.addView(game, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8) })

        val foot = TextView(this).apply {
            text = "Tap the small red head area for a headshot. Scores stay on this device."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(dp(4), dp(10), dp(4), dp(4))
        }
        root.addView(foot)
        setContentView(root)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

private class ArenaView(
    context: android.content.Context,
    private val prefs: android.content.SharedPreferences,
    initialTargetSize: Int,
) : View(context), Choreographer.FrameCallback {
    private data class Target(var x: Float, var y: Float, var dx: Float, var dy: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targets = mutableListOf<Target>()
    private var targetSize = initialTargetSize.toFloat()
    private var lastFrameNs = 0L
    private var elapsed = 0f
    private var headshots = 0
    private var bodyHits = 0
    private var best = prefs.getInt("arena_best", 0)
    private val density = resources.displayMetrics.density

    init {
        isFocusable = true
        setBackgroundColor(Color.rgb(16, 16, 22))
    }

    fun setTargetSize(size: Int) {
        targetSize = size.toFloat()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(this)
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        val dt = if (lastFrameNs == 0L) 0f else
            ((frameTimeNanos - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = frameTimeNanos
        elapsed += dt
        updateTargets(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun updateTargets(dt: Float) {
        if (width <= 0 || height <= 0) return
        if (targets.isEmpty()) spawnTarget()
        if (elapsed >= 1.25f) {
            elapsed = 0f
            if (targets.size < 4) spawnTarget()
        }
        val margin = targetSize * density
        targets.forEach { target ->
            target.x += target.dx * dt
            target.y += target.dy * dt
            if (target.x < margin || target.x > width - margin) target.dx *= -1
            if (target.y < margin || target.y > height - margin) target.dy *= -1
            target.x = target.x.coerceIn(margin, (width - margin).coerceAtLeast(margin))
            target.y = target.y.coerceIn(margin, (height - margin).coerceAtLeast(margin))
        }
    }

    private fun spawnTarget() {
        if (width <= 0 || height <= 0) return
        val margin = (targetSize * density).coerceAtMost(minOf(width, height) / 3f)
        val rangeX = (width - 2 * margin).coerceAtLeast(1f)
        val rangeY = (height - 2 * margin).coerceAtLeast(1f)
        targets.add(
            Target(
                margin + Random.nextFloat() * rangeX,
                margin + Random.nextFloat() * rangeY,
                (Random.nextFloat() - 0.5f) * 110f * density,
                (Random.nextFloat() - 0.5f) * 90f * density,
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(16, 16, 22))
        paint.color = Color.rgb(38, 38, 48)
        paint.strokeWidth = density
        paint.style = Paint.Style.STROKE
        val step = 32f * density
        var x = 0f
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += step }
        var y = 0f
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += step }
        paint.style = Paint.Style.FILL

        targets.forEach { target ->
            val radius = targetSize * density * 0.48f
            paint.color = Color.rgb(210, 214, 225)
            canvas.drawRoundRect(target.x - radius * 0.58f, target.y, target.x + radius * 0.58f,
                target.y + radius * 1.65f, radius * 0.22f, radius * 0.22f, paint)
            paint.color = Color.rgb(255, 52, 79)
            canvas.drawCircle(target.x, target.y - radius * 0.30f, radius * 0.34f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(target.x, target.y - radius * 0.30f, radius * 0.12f, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 15f * density
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("HEADSHOTS  $headshots", 16f * density, 26f * density, paint)
        paint.color = Color.LTGRAY
        paint.textSize = 12f * density
        canvas.drawText("BODY HITS  $bodyHits     BEST  $best", 16f * density, 47f * density, paint)
        if (targets.isEmpty()) {
            paint.color = Color.LTGRAY
            paint.typeface = android.graphics.Typeface.DEFAULT
            canvas.drawText("Tap a target to begin", 16f * density, height / 2f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        val hitIndex = targets.indexOfFirst { target ->
            val headRadius = targetSize * density * 0.20f
            hypot(
                (event.x - target.x).toDouble(),
                (event.y - (target.y - targetSize * density * 0.14f)).toDouble(),
            ) <= headRadius.toDouble()
        }
        if (hitIndex >= 0) {
            headshots++
            if (headshots > best) {
                best = headshots
                prefs.edit().putInt("arena_best", best).apply()
            }
            targets.removeAt(hitIndex)
        } else {
            val bodyIndex = targets.indexOfFirst { target ->
                val radius = targetSize * density * 0.48f
                event.x in (target.x - radius * 0.58f)..(target.x + radius * 0.58f) &&
                    event.y in target.y..(target.y + radius * 1.65f)
            }
            if (bodyIndex >= 0) {
                bodyHits++
                targets.removeAt(bodyIndex)
            }
        }
        invalidate()
        return true
    }
}

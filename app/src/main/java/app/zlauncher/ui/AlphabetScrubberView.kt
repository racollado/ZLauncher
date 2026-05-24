package app.zlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import app.zlauncher.R
import app.zlauncher.helper.getColorFromAttr

/**
 * Vertical strip of the distinct first letters present in the drawer's current list.
 * A dot at the top clears the active letter filter. Touching/dragging maps a
 * y-coordinate to a letter and fires [onLetterSelected].
 */
class AlphabetScrubberView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var letters: List<Char> = emptyList()
    private var selectedIndex: Int = -1
    private var lastNotifiedLetter: Char? = null
    private var filterActive: Boolean = false

    var onLetterSelected: ((Char) -> Unit)? = null
    var onResetSelected: (() -> Unit)? = null
    var onScrubStarted: (() -> Unit)? = null
    var onScrubEnded: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 11f, resources.displayMetrics
        )
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics
        )
    }

    init {
        isClickable = true
        contentDescription = context.getString(R.string.cd_scrubber_reset)
        applyThemeColors()
    }

    private fun applyThemeColors() {
        paint.color = context.getColorFromAttr(R.attr.primaryColor)
        paint.alpha = 255
        highlightPaint.color = context.getColorFromAttr(R.attr.primaryColor)
    }

    fun setLetters(letters: List<Char>) {
        this.letters = letters
        selectedIndex = letters.indexOf(lastNotifiedLetter)
        applyThemeColors()
        requestLayout()
        invalidate()
    }

    /** Sync the highlight with an externally-applied filter (or clear it with `null`). */
    fun setActiveLetter(letter: Char?) {
        lastNotifiedLetter = letter
        selectedIndex = letter?.let { letters.indexOf(it) } ?: -1
        filterActive = letter != null
        invalidate()
    }

    private fun hasResetDot(): Boolean = onResetSelected != null

    private fun sectionMetrics(): Triple<Float, Float, Float> {
        val available = (height - paddingTop - paddingBottom).toFloat()
        val slotCount = letters.size + if (hasResetDot()) 1 else 0
        if (slotCount == 0) return Triple(0f, 0f, available)
        val sectionHeight = available / slotCount
        val resetHeight = if (hasResetDot()) sectionHeight else 0f
        val lettersHeight = available - resetHeight
        return Triple(sectionHeight, resetHeight, lettersHeight)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 22f, resources.displayMetrics
        ).toInt()
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val width = if (widthMode == MeasureSpec.EXACTLY) MeasureSpec.getSize(widthMeasureSpec)
        else defaultWidth
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (letters.isEmpty() && !hasResetDot()) return

        val (sectionHeight, resetHeight, lettersHeight) = sectionMetrics()
        val cx = width / 2f

        if (hasResetDot()) {
            val resetCenterY = paddingTop + resetHeight / 2f
            val resetPaint = if (filterActive) highlightPaint else paint
            val resetBaseline = resetCenterY - (resetPaint.descent() + resetPaint.ascent()) / 2f
            canvas.drawText(RESET_DOT, cx, resetBaseline, resetPaint)
        }

        if (letters.isEmpty()) return

        val letterSectionHeight = lettersHeight / letters.size
        val lettersTop = paddingTop + resetHeight
        for (i in letters.indices) {
            val baseY = lettersTop + letterSectionHeight * i + letterSectionHeight / 2f
            val text = displayLabel(letters[i])
            val p = if (i == selectedIndex) highlightPaint else paint
            val textBaseline = baseY - (p.descent() + p.ascent()) / 2f
            canvas.drawText(text, cx, textBaseline, p)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (letters.isEmpty() && !hasResetDot()) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onScrubStarted?.invoke()
                handleTouch(event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouch(event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onScrubEnded?.invoke()
                return true
            }
        }
        return false
    }

    private fun handleTouch(y: Float) {
        val (_, resetHeight, lettersHeight) = sectionMetrics()
        val clamped = y.coerceIn(paddingTop.toFloat(), (height - paddingBottom).toFloat())

        if (hasResetDot() && clamped < paddingTop + resetHeight) {
            if (filterActive) {
                onResetSelected?.invoke()
            }
            return
        }

        if (letters.isEmpty()) return

        val lettersTop = paddingTop + resetHeight
        val letterSectionHeight = lettersHeight / letters.size
        val index = ((clamped - lettersTop) / letterSectionHeight)
            .toInt()
            .coerceIn(0, letters.size - 1)
        val letter = letters[index]
        if (letter != lastNotifiedLetter) {
            lastNotifiedLetter = letter
            selectedIndex = index
            invalidate()
            onLetterSelected?.invoke(letter)
        }
    }

    private fun displayLabel(letter: Char): String = letter.toString()

    companion object {
        private const val RESET_DOT = "\u00B7"
    }
}

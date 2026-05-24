package app.zlauncher.listener

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import app.zlauncher.data.Constants

/**
 * Per-row gesture listener used by home app TextViews. Horizontal scrolls are owned by
 * ViewPager2, so we only emit a swipe-down, single tap, and long-press.
 */
internal open class ViewSwipeTouchListener(c: Context?, v: View) : OnTouchListener {
    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable { onLongClick(targetView) }
    private val targetView: View = v
    private val gestureDetector: GestureDetector = GestureDetector(c, GestureListener(v))

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> view.isPressed = true
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
                handler.removeCallbacks(longPressRunnable)
            }
        }
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener(private val view: View) : SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(view)
            return super.onSingleTapUp(e)
        }

        override fun onLongPress(e: MotionEvent) {
            handler.removeCallbacks(longPressRunnable)
            handler.postDelayed(longPressRunnable, Constants.LONG_PRESS_DELAY_MS)
            super.onLongPress(e)
        }

        override fun onFling(
            event1: MotionEvent?,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            try {
                val diffY = event2.y - (event1?.y ?: 0F)
                val diffX = event2.x - (event1?.x ?: 0F)
                if (kotlin.math.abs(diffY) > kotlin.math.abs(diffX) &&
                    kotlin.math.abs(diffY) > swipeThreshold &&
                    kotlin.math.abs(velocityY) > swipeVelocityThreshold &&
                    diffY > 0
                ) {
                    onSwipeDown()
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipeDown() {}
    open fun onLongClick(view: View) {}
    open fun onClick(view: View) {}
}

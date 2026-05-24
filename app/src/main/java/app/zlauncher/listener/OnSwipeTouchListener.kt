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
import kotlin.math.abs

/**
 * Home-background gesture listener. After the ZLauncher restructure ViewPager2 owns
 * horizontal scrolls between Widgets, Home, and Drawer, so this only emits swipe-down,
 * single tap, double tap, and long-press.
 */
internal open class OnSwipeTouchListener(c: Context?) : OnTouchListener {
    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable { onLongClick() }
    private val gestureDetector: GestureDetector = GestureDetector(c, GestureListener())

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.action == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(longPressRunnable)
        }
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick()
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return super.onDoubleTap(e)
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
                // ViewPager2 owns horizontal swipes, so we only emit a swipe-down here.
                if (abs(diffY) > abs(diffX) &&
                    abs(diffY) > swipeThreshold &&
                    abs(velocityY) > swipeVelocityThreshold &&
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
    open fun onLongClick() {}
    open fun onDoubleClick() {}
    open fun onClick() {}
}

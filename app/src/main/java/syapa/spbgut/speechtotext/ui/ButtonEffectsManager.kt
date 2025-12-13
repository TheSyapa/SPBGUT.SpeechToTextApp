package syapa.spbgut.speechtotext.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.R

class ButtonEffectsManager(private val activity: MainActivity) {

    @SuppressLint("ClickableViewAccessibility")
    fun setupButtonEffects() {
        val scaleDown = AnimationUtils.loadAnimation(activity, R.anim.scale_down)
        val scaleUp = AnimationUtils.loadAnimation(activity, R.anim.scale_up)

        activity.btnRecord.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(scaleDown)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.startAnimation(scaleUp)
            }
            false
        }

        activity.btnCopy.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(scaleDown)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.startAnimation(scaleUp)
            }
            false
        }
    }
}
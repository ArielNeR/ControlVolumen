package com.example.controlvolumen

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class EdgeGestureDetector(context: Context, private val listener: OnEdgeGestureListener) : View(context) {

    interface OnEdgeGestureListener {
        fun onEdgeSwipeRightToLeft() // Swipe desde DERECHA a IZQUIERDA
        fun onEdgeSwipeLeftToRight() // Swipe desde IZQUIERDA a DERECHA
    }

    private var initialTouchX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.rawX - initialTouchX
                if (deltaX < -50) {
                    listener.onEdgeSwipeRightToLeft()
                } else if (deltaX > 50) {
                    listener.onEdgeSwipeLeftToRight()
                }
            }
        }
        return true
    }
}
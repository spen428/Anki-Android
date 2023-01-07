/*
 * Copyright (c) 2009 Andrew <andrewdubya@gmail.com>
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>
 * Copyright (c) 2021 Nicolai Weitkemper <kontakt@nicolaiweitkemper.de>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ichi2.anki

import android.annotation.SuppressLint
import android.graphics.*
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ichi2.themes.Themes
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import java.util.concurrent.Executors

/**
 * Whiteboard implementation for Onyx devices with a stylus
 */
@SuppressLint("ViewConstructor")
open class OnyxBooxWhiteboard(activity: AnkiActivity, handleMultiTouch: Boolean, inverted: Boolean) : Whiteboard(activity, handleMultiTouch, inverted) {
    private val executorService = Executors.newCachedThreadPool()
    private var onyxTouchHelper: TouchHelper? = null

    override fun handleDrawEvent(event: MotionEvent): Boolean {
        if (onyxTouchHelper != null) {
            onyxTouchHelper!!.setRawDrawingEnabled(event.action != MotionEvent.ACTION_UP)
            return true
        }
        return super.handleDrawEvent(event)
    }

    override fun saveStrokeWidth(wbStrokeWidth: Int) {
        super.saveStrokeWidth(wbStrokeWidth)
        onyxTouchHelper?.setStrokeWidth(mPaint.strokeWidth)
    }

    companion object {
        private var mWhiteboardMultiTouchMethods: WhiteboardMultiTouchMethods? = null
        fun createInstance(
            context: AnkiActivity,
            handleMultiTouch: Boolean,
            whiteboardMultiTouchMethods: WhiteboardMultiTouchMethods?
        ): Whiteboard {
            val whiteboard = OnyxBooxWhiteboard(context, handleMultiTouch, Themes.currentTheme.isNightMode)
            mWhiteboardMultiTouchMethods = whiteboardMultiTouchMethods
            val lp2 = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            whiteboard.layoutParams = lp2
            val fl = context.findViewById<FrameLayout>(R.id.whiteboard)
            fl.addView(whiteboard)
            whiteboard.isEnabled = true
            return whiteboard
        }
    }

    init {
        startOnyxPenHandler()
    }

    private fun startOnyxPenHandler() {
        val callback = object : RawInputCallback() {
            override fun onBeginRawDrawing(p0: Boolean, p1: TouchPoint?) {
            }

            override fun onEndRawDrawing(p0: Boolean, p1: TouchPoint?) {
            }

            override fun onRawDrawingTouchPointMoveReceived(p0: TouchPoint?) {
            }

            override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList?) {
                if (touchPointList == null) return
                executorService.submit { this@OnyxBooxWhiteboard.drawTouchPoints(touchPointList) }
            }

            override fun onBeginRawErasing(p0: Boolean, p1: TouchPoint?) {
            }

            override fun onEndRawErasing(p0: Boolean, p1: TouchPoint?) {
            }

            override fun onRawErasingTouchPointMoveReceived(p0: TouchPoint?) {
            }

            override fun onRawErasingTouchPointListReceived(p0: TouchPointList?) {
                println("onRawErasingTouchPointListReceived")
            }
        }

        val canvasBounds = mCanvas.clipBounds
        val menuBarBounds = Rect(canvasBounds.left, canvasBounds.top, canvasBounds.right, 50)
        onyxTouchHelper = TouchHelper.create(this, callback)
            .setLimitRect(canvasBounds, listOf(menuBarBounds))
            .setStrokeWidth(mPaint.strokeWidth)
            .setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
            .openRawDrawing()
    }

    private fun drawTouchPoints(touchPointList: TouchPointList) {
        val iterator = touchPointList.iterator()

        val firstTouchPoint = iterator.next()
        drawStart(firstTouchPoint.x, firstTouchPoint.y)

        while (iterator.hasNext()) {
            val touchPoint = iterator.next()
            drawAlong(touchPoint.x, touchPoint.y)
        }

        drawFinish()
    }
}

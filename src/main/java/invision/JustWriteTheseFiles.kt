package invision

const val BASE_INVISION_ACTIVITY = """package PACKAGE.invision

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import PACKAGE.R


abstract class BaseInvisionActivity : Activity() {
    var bgResId = 0
    lateinit var wire: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.invision_activity)
        wire=findViewById(R.id.wire)
    }

    override fun onStart() {
        super.onStart()
        val bm = BitmapFactory.decodeResource(resources, bgResId)
        var point = Point()
        windowManager.defaultDisplay.getSize(point)
        val scroll = wire.parent as View
        val sbm = Bitmap.createScaledBitmap(bm, point.x,( 1.0 * point.x / bm.width * bm.height).toInt(), false)
        wire.setImageBitmap(sbm)
        Toast.makeText(this, this.localClassName, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        wire.setImageResource(android.R.color.black)
    }
}"""

fun getBaseActivity(packageName: String) : String {
    return BASE_INVISION_ACTIVITY.replace("PACKAGE", packageName)
}

const val HOTSPOT = """package PACKAGE.invision

import android.view.MotionEvent
import android.view.View

data class Hotspot(val x: Double, val y: Double, val width: Double, val height: Double, val target: Class<*>) {
    fun contains(view: View, touchEvent: MotionEvent): Boolean {
        val xbeg = view.width * x
        val xend = view.width * (x + width)
        val xshould = touchEvent.x >= xbeg && touchEvent.x <= xend
        val ybeg = view.height * y
        val yend = view.height * (y + height)
        val yshould = touchEvent.y >= ybeg && touchEvent.y <= yend
        val should = xshould && yshould
        return touchEvent.x >= view.width * x
                && touchEvent.x <= view.width * (x+width)
                && touchEvent.y >= view.height * y
                && touchEvent.y <= view.height * (y+height)
    }
}"""

fun getHotspot(packageName: String) : String {
    return HOTSPOT.replace("PACKAGE", packageName)
}

const val ACTIVITY_LAYOUT = """<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent">
    <ImageView
        android:id="@+id/wire"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        />
</ScrollView>"""

fun getActivityLayout(packageName: String) : String {
    return ACTIVITY_LAYOUT
}

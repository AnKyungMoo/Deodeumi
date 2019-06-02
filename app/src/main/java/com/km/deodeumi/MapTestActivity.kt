package com.km.deodeumi

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.RelativeLayout
import net.daum.mf.map.api.MapView



class MapTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_test)

        val mapView = MapView(this)
        val mapViewContainer = findViewById<RelativeLayout>(R.id.mapView)
        mapViewContainer.addView(mapView)


    }
}

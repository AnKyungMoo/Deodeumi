package activity

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.alexzaitsev.meternumberpicker.MeterView
import com.km.deodeumi.R



class StrideActivity : AppCompatActivity() {

    private var AVERAGE_FOOTFALL : Int = 75 //성인 평균 보폭
    private lateinit var meterView: MeterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stride)

        val shared : SharedPreferences = getSharedPreferences("step", MODE_PRIVATE)
        AVERAGE_FOOTFALL = shared.getInt("distance", 75)

        meterView = findViewById(R.id.meterView)
        meterView.value = AVERAGE_FOOTFALL

    }


    override fun onPause() {
        super.onPause()
        Log.i("보폭 수::::", meterView.value.toString())
        val pref = getSharedPreferences("step", MODE_PRIVATE)
        val editor = pref.edit()
        editor.putInt("distance", meterView.value)
        editor.apply()
    }
}

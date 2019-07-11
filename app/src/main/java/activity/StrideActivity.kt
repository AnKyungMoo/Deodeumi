package activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.alexzaitsev.meternumberpicker.MeterView
import com.km.deodeumi.R




class StrideActivity : AppCompatActivity() {

    private val AVERAGE_FOOTFALL : Int = 75 //성인 평균 보폭
    private lateinit var meterView: MeterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stride)

        meterView = findViewById(R.id.meterView)
        meterView.value = AVERAGE_FOOTFALL

    }


    override fun onBackPressed() {
        Log.i("보폭 수::::", meterView.value.toString())
        super.onBackPressed()
    }
}

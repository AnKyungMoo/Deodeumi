package activity

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.km.deodeumi.R





class SplashActivity : AppCompatActivity() {

    private lateinit var splash: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        splash = findViewById<RelativeLayout>(R.id.img_splash_background)
        val handler = Handler()
        handler.postDelayed(SplashHandler(), 2800)
    }

    private inner class SplashHandler : Runnable {
        override fun run() {
            val i = Intent(this@SplashActivity, MapActivity::class.java)
            startActivity(i)
            this@SplashActivity.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recycleView(findViewById<ImageView>(R.id.img_splash_background))
        System.gc()
    }

    private fun recycleView(view: View?) {
        if (view != null) {
            val bg = view!!.background
            if (bg != null) {
                bg!!.callback = null
                (bg as BitmapDrawable).bitmap.recycle()
                view!!.setBackgroundDrawable(null)
            }
        }
    }

}



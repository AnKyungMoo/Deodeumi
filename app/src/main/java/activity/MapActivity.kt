package activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.km.deodeumi.R
import resources.APIKey
import com.skt.Tmap.TMapView
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.custom_dialog.view.*

class MapActivity : AppCompatActivity() {

    private lateinit var callBtn: Button
    private lateinit var footCountBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val tMapView = TMapView(this)
        tMapView.setSKTMapApiKey(APIKey.TMAP)

        /* TODO: 현재좌표로 이동을 완료하고 설정하자 true: 나침반 모드 on */
//        tMapView.setCompassMode(true)
        map_layout.addView(tMapView)

        callBtn = findViewById(R.id.btn_call_center)
        footCountBtn = findViewById(R.id.btn_count_foot)

        callBtn.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
            val mAlertDialog = builder.show()
            dialogView.btn_call.setOnClickListener{
                mAlertDialog.dismiss()
                /* TODO: 전화번호를 여러 데이터를 받아오면 상황에 맞춰 사용하자 */
                startActivity(Intent("android.intent.action.CALL", Uri.parse("tel:0220920000")))
            }

        }

        footCountBtn.setOnClickListener{
            val intent = Intent(this, StrideActivity::class.java)
            startActivityForResult(intent, 200)
        }

        btn_search_location.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            startActivity(intent)
        }
    }


}

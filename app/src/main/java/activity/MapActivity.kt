package activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.km.deodeumi.R
import kotlinx.android.synthetic.main.custom_dialog.view.*

class MapActivity : AppCompatActivity() {

    private lateinit var callBtn: Button
    private lateinit var footCountBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        callBtn = findViewById(R.id.btn_call_center)
        footCountBtn = findViewById(R.id.btn_count_foot)

        callBtn.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
            val mAlertDialog = builder.show()
            dialogView.btn_call.setOnClickListener{
                mAlertDialog.dismiss()
            }

        }

        footCountBtn.setOnClickListener{
            val intent = Intent(this, StrideActivity::class.java)
            startActivityForResult(intent, 200)
        }
    }


}

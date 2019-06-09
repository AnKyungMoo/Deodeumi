package adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.km.deodeumi.R
import mapapi.FootfallPaths

class FootfallPathsAdapter(context: Context, paths: ArrayList<FootfallPaths>) : BaseAdapter() {

    val context = context
    val paths = paths


    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val categoryView: View
        categoryView = LayoutInflater.from(context).inflate(R.layout.layout_path_template, null)

        val icon_traffic : Button = categoryView.findViewById(R.id.btn_traffic_icon)
        val txt_usePaths: TextView = categoryView.findViewById(R.id.txt_use_path)
        val txt_useFoot: TextView = categoryView.findViewById(R.id.txt_footfall_count)


        val category = paths[position]
        var image_name: String = category.traffic_icon
        var randMark : String = category.txt_randmark
        var footFall: String = category.txt_footfall

        if (image_name == "bus"){
            icon_traffic.setBackgroundResource(R.drawable.ic_launcher_foreground) //man_01_32는 왜 인식을 못할꽈...
        }
        txt_usePaths.text = randMark
        txt_useFoot.text = footFall

        return categoryView

    }

    override fun getItem(position: Int): Any {
        return paths[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return paths.count()
    }

}
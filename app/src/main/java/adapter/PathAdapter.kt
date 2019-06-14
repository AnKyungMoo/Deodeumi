package adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.km.deodeumi.R
import mapapi.FootfallPaths

class PathAdapter(val context: Context, val pathList: ArrayList<FootfallPaths>) :BaseAdapter(){

    // xml 파일의 View와 데이터를 연결하는 핵심 역할을 하는 메소드
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = LayoutInflater.from(context).inflate(R.layout.layout_path_template, null)

        val traffic = view.findViewById<ImageView>(R.id.btn_traffic_icon)
        val randmark = view.findViewById<TextView>(R.id.txt_use_path)
        val count = view.findViewById<TextView>(R.id.txt_footfall_count)

        val path = pathList[position]
        val resourceId = context.resources.getIdentifier(path.traffic_icon, "drawable", context.packageName)
        traffic.setImageResource(resourceId)
        randmark.text = path.txt_randmark
        count.text = path.txt_footfall

        return view

    }

    //해당 위치의 item을 메소드
    override fun getItem(position: Int): Any {
        return pathList[position]
    }

    //해당 위치의 item id를 반환하는 메소드
    override fun getItemId(position: Int): Long {
        return 0
    }

    //ListView에 속한 item의 전체 수를 반환
    override fun getCount(): Int {
        return pathList.size
    }

}
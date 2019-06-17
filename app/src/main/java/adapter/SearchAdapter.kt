package adapter

import activity.MyLocationActivity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.km.deodeumi.R
import kotlinx.android.synthetic.main.item_search.view.*
import models.KeywordObject

class SearchAdapter(val context: Context) : RecyclerView.Adapter<SearchAdapter.Holder>() {
    private var documentList: ArrayList<KeywordObject.documents> = ArrayList()
    private var keyword: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search, parent, false)

        return Holder(view)
    }

    fun addItem(document: KeywordObject.documents) { //place_name, address_name
        documentList.add(document)
        notifyDataSetChanged()
    }

    fun removeAllItem() {
        documentList.clear()
        notifyDataSetChanged()
    }

    fun setSearchKeyword(keyword: String){
        this.keyword = keyword
    }

    override fun getItemCount(): Int {
        return documentList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(documentList[position], context)

        // item click 할 때 발생하는 이벤트
        holder.itemView.setOnClickListener {
            val intent = Intent(context, MyLocationActivity::class.java)
            intent.putExtra("myLocationString", documentList[position].place_name)
            intent.putExtra("longitude", documentList[position].x)
            intent.putExtra("latitude",documentList[position].y)
            context.startActivity(intent)

            (context as Activity).finish()
        }
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind (document: KeywordObject.documents, context: Context) {
            var index = document.place_name.indexOf(keyword)
            var span = SpannableString(document.place_name)
            if(index >= 0){
                var foregroundColorSpan = ForegroundColorSpan(Color.parseColor("#0b78ad"))
                span.setSpan(foregroundColorSpan, index, index+keyword.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            itemView.text_search_name.text = span
            itemView.text_search_address.text = document.address_name
        }
    }
}
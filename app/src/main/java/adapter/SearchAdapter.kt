package adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.km.deodeumi.R
import kotlinx.android.synthetic.main.item_search.view.*
import models.KeywordObject

class SearchAdapter(val context: Context) : RecyclerView.Adapter<SearchAdapter.Holder>() {
    private var documentList: ArrayList<KeywordObject.documents> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search, parent, false)

        return Holder(view)
    }

    fun addItem(document: KeywordObject.documents) {
        documentList.add(document)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return documentList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(documentList[position], context)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind (document: KeywordObject.documents, context: Context) {
            itemView.text_search_name.text = document.place_name
        }
    }
}
package dev.seriy0904.mangareader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.seriy0904.mangareader.R

class FilesListAdapter(val cl:FileClick) : RecyclerView.Adapter<FilesListAdapter.ViewHolder>() {
    private val model:ArrayList<FilesListModel> = arrayListOf()
    interface FileClick{
        fun onClick(position: Int)
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mangaName: TextView = itemView.findViewById(R.id.mangaFileName)
        val mangaChapter: TextView = itemView.findViewById(R.id.mangaFileChapters)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.files_item, parent, false))
    }

    fun setList(newList:List<FilesListModel>){
        model.clear()
        model.addAll(newList.sortedBy {it.mangaName})
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mangaName.text = model[position].mangaName
        holder.mangaChapter.text = model[position].mangaChapter
        holder.itemView.setOnClickListener {
            cl.onClick(position)
        }
    }

    override fun getItemCount() = model.size
}
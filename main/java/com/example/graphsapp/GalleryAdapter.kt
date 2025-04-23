package com.example.graphsapp

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GalleryAdapter(
    private val graphs: List<Pair<Graph, Bitmap>>,
    private val onGraphClicked: (Graph) -> Unit,
    private val onGraphDeleted: (Int) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GraphViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraphViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_graph, parent, false)
        return GraphViewHolder(view)
    }

    override fun onBindViewHolder(holder: GraphViewHolder, position: Int) {
        val (graph, thumbnail) = graphs[position]
        holder.bind(graph, thumbnail)
        holder.itemView.setOnClickListener {
            onGraphClicked(graph)
        }
    }

    override fun getItemCount(): Int = graphs.size

    fun deleteItem(position: Int) {
        onGraphDeleted(position)
    }

    class GraphViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.graph_info)
        private val thumbnailView: ImageView = itemView.findViewById(R.id.graph_thumbnail)

        fun bind(graph: Graph, thumbnail: Bitmap) {
            textView.text = "Граф с ${graph.vertices.size} вершинами и ${graph.getEdgeCount()} рёбрами"
            thumbnailView.setImageBitmap(thumbnail)
        }
    }
}
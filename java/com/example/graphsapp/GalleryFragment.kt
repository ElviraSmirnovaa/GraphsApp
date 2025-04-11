package com.example.graphsapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryFragment : Fragment() {

    private var navigationListener: OnNavigationListener? = null
    private lateinit var adapter: GalleryAdapter

    interface OnNavigationListener {
        fun navigateToDrawFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNavigationListener) {
            navigationListener = context
        } else {
            throw RuntimeException("$context must implement OnNavigationListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val activity = activity as MainActivity
        adapter = GalleryAdapter(activity.getSavedGraphs(), // Используем метод getSavedGraphs() вместо прямого доступа к полю
            { graph ->
                activity.loadGraph(graph)
                navigationListener?.navigateToDrawFragment()
            },
            { position ->
                activity.deleteGraph(position)
                adapter.notifyItemRemoved(position)
            }
        )

        recyclerView.adapter = adapter

        // Add swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                adapter.deleteItem(position)
            }
        }).attachToRecyclerView(recyclerView)

        return view
    }

    override fun onDetach() {
        super.onDetach()
        navigationListener = null
    }
}
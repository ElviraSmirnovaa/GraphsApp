package com.example.graphsapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class DrawGraphFragment : Fragment(), GraphHighlightListener {

    private lateinit var drawView: DrawView
    private var animationRunning = false
    private val animationHandler = Handler(Looper.getMainLooper())
    private lateinit var clearButton: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var animateButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_draw_graph, container, false)
        drawView = view.findViewById(R.id.draw_view)
        initViews(view)
        setupGraph()
        setupListeners()
        return view
    }

    private fun initViews(view: View) {
        clearButton = view.findViewById(R.id.clear_button)
        saveButton = view.findViewById(R.id.save_to_gallery_button)
        animateButton = view.findViewById(R.id.animate_button)
    }

    private fun setupGraph() {
        (activity as? MainActivity)?.let { activity ->
            if (activity.graph.vertices.isEmpty()) {
                activity.graph = Graph().apply {
                    setAnimationHandler(animationHandler)
                }
                return@let
            }
            drawView.loadGraph(activity.graph)
        }
    }

    private fun setupListeners() {
        drawView.apply {
            onDrawListener = { v1, v2 ->
                (activity as? MainActivity)?.graph?.apply {
                    addEdge(v1, v2)
                    if (v1 < drawView.vertices.size && v2 < drawView.vertices.size) {
                        setVertexPosition(v1, drawView.vertices[v1].x, drawView.vertices[v1].y)
                        setVertexPosition(v2, drawView.vertices[v2].x, drawView.vertices[v2].y)
                    }
                }
            }

            onRemoveVertexListener = { vertex ->
                (activity as? MainActivity)?.graph?.removeVertex(vertex)
            }

            onRemoveEdgeListener = { v1, v2 ->
                (activity as? MainActivity)?.graph?.removeEdge(v1, v2)
            }

            onEdgeClickListener = { edge ->
                showWeightDialog(edge)
            }
        }

        clearButton.setOnClickListener {
            drawView.clear()
            (activity as? MainActivity)?.graph = Graph().apply {
                setAnimationHandler(animationHandler)
            }
        }

        saveButton.setOnClickListener {
            (activity as? MainActivity)?.let { activity ->
                if (activity.graph.vertices.isNotEmpty()) {
                    activity.saveGraph(activity.graph, drawView.createThumbnail())
                    Toast.makeText(context, "Граф сохранён в галерею", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Нечего сохранять - граф пуст", Toast.LENGTH_SHORT).show()
                }
            }
        }

        animateButton.setOnClickListener {
            if (!animationRunning) {
                startAnimation()
            }
        }
    }

    private fun startAnimation() {
        (activity as? MainActivity)?.getCurrentAlgorithmData()?.let { (path, type, coloring) ->
            when (type) {
                "GREEDY_COLORING", "SEQUENTIAL_COLORING" -> {
                    coloring?.let { animateColoring(it) } ?: showNoAnimationData()
                }
                else -> {
                    path?.let { animatePath(it) } ?: showNoAnimationData()
                }
            }
        } ?: showNoAnimationData()
    }

    private fun animatePath(path: List<Int>) {
        animationRunning = true
        drawView.highlightPath(emptyList())

        var currentStep = 0
        val runnable = object : Runnable {
            override fun run() {
                if (currentStep < path.size) {
                    drawView.highlightPath(path.take(currentStep + 1))
                    currentStep++
                    animationHandler.postDelayed(this, 1000)
                } else {
                    animationRunning = false
                }
            }
        }
        animationHandler.post(runnable)
    }

    private fun animateColoring(coloring: Map<Int, Int>) {
        animationRunning = true
        val vertices = coloring.keys.toList()
        drawView.highlightPath(emptyList())
        drawView.resetEdgeColors()

        var currentStep = 0
        val runnable = object : Runnable {
            override fun run() {
                if (currentStep < vertices.size) {
                    val currentColoring = mapOf(vertices[currentStep] to coloring[vertices[currentStep]]!!)
                    drawView.setColoring(currentColoring)
                    drawView.highlightPath(listOf(vertices[currentStep]))
                    currentStep++
                    animationHandler.postDelayed(this, 1000)
                } else {
                    drawView.setColoring(coloring)
                    animationRunning = false
                }
            }
        }
        animationHandler.post(runnable)
    }

    private fun showNoAnimationData() {
        Toast.makeText(context, "Нет данных для анимации. Сначала выполните алгоритм.", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun showWeightDialog(edge: Pair<Int, Int>) {
        val currentWeight = (activity as? MainActivity)?.graph?.weights?.get(edge) ?: 1
        val dialogView = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Введите вес ребра"
            setText(currentWeight.toString())
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Вес ребра")
            .setView(dialogView)
            .setPositiveButton("ОК") { _, _ ->
                val weight = dialogView.text.toString().toIntOrNull() ?: 1
                drawView.setEdgeWeight(edge.first, edge.second, weight)
                (activity as? MainActivity)?.graph?.addEdge(edge.first, edge.second, weight)
                drawView.invalidate()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun highlightPath(path: List<Int>) {
        drawView.highlightPath(path)
        drawView.invalidate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.graph?.setAnimationHandler(animationHandler)
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.graph?.stopAnimation()
        animationHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.graph?.stopAnimation()
        animationRunning = false
    }
}

interface GraphHighlightListener {
    fun highlightPath(path: List<Int>)
}
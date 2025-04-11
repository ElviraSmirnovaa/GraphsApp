package com.example.graphsapp

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity(), GalleryFragment.OnNavigationListener {

    // Текущий активный граф
    var graph = Graph()

    // Переменная для отслеживания последнего выполненного алгоритма
    var lastAlgorithm: String? = null

    fun getCurrentAlgorithmPath(): List<Int>? {
        return graph.getAlgorithmPath()
    }
    fun getCurrentAlgorithmData(): Triple<List<Int>?, String?, Map<Int, Int>?> {
        return Triple(graph.getAlgorithmPath(), lastAlgorithm, graph.getColoring())
    }

    fun saveCurrentAlgorithmData(path: List<Int>?, type: String?, coloring: Map<Int, Int>?) {
        graph.setAlgorithmPath(path ?: emptyList())
        lastAlgorithm = type
        graph.setColoring(coloring ?: emptyMap())
    }



    // Список сохранённых графов с их миниатюрами (private backing field)
    private val _savedGraphs = mutableListOf<Pair<Graph, Bitmap>>()

    // Навигация
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Скрываем ActionBar
        supportActionBar?.hide()

        // Инициализация графа с обработчиком анимаций
        graph = Graph().apply {
            setAnimationHandler(Handler(Looper.getMainLooper()))
        }

        // Инициализация SharedPreferences для сохранения данных
        sharedPreferences = getSharedPreferences("GraphsApp", MODE_PRIVATE)
        loadSavedGraphs()

        // Настройка нижней навигации
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_graph -> {
                    showFragment(GraphFragment())
                    true
                }
                R.id.navigation_draw -> {
                    showFragment(DrawGraphFragment())
                    true
                }
                R.id.navigation_algorithms -> {
                    showFragment(AlgorithmsFragment())
                    true
                }
                R.id.navigation_gallery -> {
                    showFragment(GalleryFragment())
                    true
                }
                else -> false
            }
        }

        // Показываем фрагмент по умолчанию
        showFragment(GraphFragment())
    }


    // Получение неизменяемого списка сохранённых графов
    fun getSavedGraphs(): List<Pair<Graph, Bitmap>> {
        return _savedGraphs.toList()
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun loadSavedGraphs() {
        try {
            val json = sharedPreferences.getString("saved_graphs", null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<SerializableGraph>>() {}.type
                val loadedGraphs: List<SerializableGraph> = gson.fromJson(json, type) ?: emptyList()
                loadedGraphs.forEach { serializableGraph ->
                    try {
                        val graph = Graph.fromSerializable(serializableGraph)
                        val thumbnail = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        _savedGraphs.add(Pair(graph, thumbnail))
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error loading graph: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading saved graphs: ${e.message}")
        }
    }

    fun highlightTSPPath(path: List<Int>) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is GraphHighlightListener) {
            currentFragment.highlightPath(path)
        } else {
            Log.w("MainActivity", "Current fragment doesn't support path highlighting")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("graph_state", gson.toJson(graph.toSerializable()))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getString("graph_state")?.let { json ->
            graph = Graph.fromSerializable(gson.fromJson(json, SerializableGraph::class.java)).apply {
                setAnimationHandler(Handler(Looper.getMainLooper()))
            }
        }
    }

    private fun saveGraphsToPreferences() {
        try {
            val serializableGraphs = _savedGraphs.map { it.first.toSerializable() }
            sharedPreferences.edit()
                .putString("saved_graphs", gson.toJson(serializableGraphs))
                .apply()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving graphs: ${e.message}")
        }
    }

    override fun navigateToDrawFragment() {
        bottomNavigation.selectedItemId = R.id.navigation_draw
    }

    fun saveGraph(graph: Graph, thumbnail: Bitmap) {
        _savedGraphs.add(Pair(graph.copy(), thumbnail))
        saveGraphsToPreferences()
    }

    fun loadGraph(graph: Graph) {
        this.graph = graph.copy().apply {
            setAnimationHandler(Handler(Looper.getMainLooper()))
        }
        showFragment(DrawGraphFragment())
    }

    fun highlightPath(path: List<Int>) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is GraphHighlightListener) {
            currentFragment.highlightPath(path)
        } else {
            Log.w("MainActivity", "Current fragment doesn't support path highlighting")
        }
    }

    fun showGraphVisualization() {
        showFragment(DrawGraphFragment())
        bottomNavigation.selectedItemId = R.id.navigation_draw
    }

    fun deleteGraph(position: Int) {
        if (position in _savedGraphs.indices) {
            _savedGraphs.removeAt(position)
            saveGraphsToPreferences()
        }
    }
}
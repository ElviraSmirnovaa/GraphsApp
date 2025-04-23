package com.example.graphsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class BinaryTreeFragment : Fragment() {

    private val binaryTree = BinaryTree()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_binary_tree, container, false)

        val inputEditText = view.findViewById<EditText>(R.id.input_value)
        val insertButton = view.findViewById<Button>(R.id.insert_button)
        val traverseButton = view.findViewById<Button>(R.id.traverse_button)
        val outputText = view.findViewById<TextView>(R.id.output_text)

        insertButton.setOnClickListener {
            val value = inputEditText.text.toString().toIntOrNull()
            if (value != null) {
                binaryTree.insert(value)
                inputEditText.text.clear()
                outputText.text = "Значение $value добавлено в дерево"
            }
        }

        traverseButton.setOnClickListener {
            val result = binaryTree.inorder()
            outputText.text = "Inorder обход: ${result.joinToString(", ")}"
        }

        return view
    }
}
package com.example.graphsapp

class BinaryTree {
    var root: Node? = null

    class Node(val value: Int) {
        var left: Node? = null
        var right: Node? = null
    }

    fun insert(value: Int) {
        root = insertRec(root, value)
    }

    private fun insertRec(node: Node?, value: Int): Node {
        if (node == null) return Node(value)
        when {
            value < node.value -> node.left = insertRec(node.left, value)
            value > node.value -> node.right = insertRec(node.right, value)
        }
        return node
    }

    fun inorder(): List<Int> {
        val result = mutableListOf<Int>()
        inorderRec(root, result)
        return result
    }

    private fun inorderRec(node: Node?, result: MutableList<Int>) {
        node?.let {
            inorderRec(it.left, result)
            result.add(it.value)
            inorderRec(it.right, result)
        }
    }
}
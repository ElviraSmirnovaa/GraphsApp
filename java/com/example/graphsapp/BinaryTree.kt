package com.example.graphsapp

class BinaryTree {
    var root: Node? = null

    class Node(var value: Int) {
        var left: Node? = null
        var right: Node? = null
    }

    fun insert(value: Int) {
        root = insertRec(root, value)
    }

    private fun insertRec(node: Node?, value: Int): Node {
        if (node == null) {
            return Node(value)
        }

        if (value < node.value) {
            node.left = insertRec(node.left, value)
        } else if (value > node.value) {
            node.right = insertRec(node.right, value)
        }

        return node
    }

    fun inorder(): List<Int> {
        val result = mutableListOf<Int>()
        inorderRec(root, result)
        return result
    }

    private fun inorderRec(node: Node?, result: MutableList<Int>) {
        if (node != null) {
            inorderRec(node.left, result)
            result.add(node.value)
            inorderRec(node.right, result)
        }
    }
}
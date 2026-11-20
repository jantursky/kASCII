package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * One node of an [AsciiTree]. A node with [children] is drawn as a folder, with a trailing slash.
 *
 * @property label Name of the file or folder.
 * @property children Entries inside a folder, empty for a file.
 */
public class AsciiTreeNode(public val label: String, public val children: List<AsciiTreeNode> = emptyList())

/** A file tree drawn with `├──` and `└──` branches. A screen reader gets the nesting as text. */
@Composable
public fun AsciiTree(root: AsciiTreeNode, modifier: Modifier = Modifier) {
    GridSurface(gridOfLines(renderTree(root)), modifier, describeTree(root))
}

internal fun renderTree(root: AsciiTreeNode): List<String> = listOf(root.name()) + branches(root, "")

private fun branches(node: AsciiTreeNode, prefix: String): List<String> =
    node.children.flatMapIndexed { i, child ->
        val last = i == node.children.lastIndex
        listOf(prefix + (if (last) "└── " else "├── ") + child.name()) +
            branches(child, prefix + if (last) "    " else "│   ")
    }

private fun AsciiTreeNode.name(): String = if (children.isEmpty()) label else "$label/"

internal fun describeTree(node: AsciiTreeNode): String =
    if (node.children.isEmpty()) node.label else "${node.label} (${node.children.joinToString { describeTree(it) }})"

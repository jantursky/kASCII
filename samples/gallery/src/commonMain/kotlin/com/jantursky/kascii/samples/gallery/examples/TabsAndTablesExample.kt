package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiAlign
import com.jantursky.kascii.components.AsciiTable
import com.jantursky.kascii.components.AsciiTableColumn
import com.jantursky.kascii.components.AsciiTabs
import com.jantursky.kascii.components.AsciiTree
import com.jantursky.kascii.components.AsciiTreeNode

private val Files =
    AsciiTreeNode(
        "kascii",
        listOf(
            AsciiTreeNode("core", listOf(AsciiTreeNode("Grid.kt"), AsciiTreeNode("Fields.kt"))),
            AsciiTreeNode("compose", listOf(AsciiTreeNode("AsciiViews.kt"))),
            AsciiTreeNode("README.md"),
        ),
    )

// The tabs only report the index, so the caller decides what each one shows. A table sizes its columns to the
// longest cell and aligns each column on its own.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabsAndTablesExample(modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    val columns = listOf(AsciiTableColumn("Module"), AsciiTableColumn("Files", AsciiAlign.End))
    val rows = listOf(listOf("core", "12"), listOf("compose", "9"), listOf("components", "40"))
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiTabs(listOf("Table", "Tree"), tab, { tab = it })
            if (tab == 0) AsciiTable(columns, rows) else AsciiTree(Files)
        }
    }
}

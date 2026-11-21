package com.jantursky.kascii.samples.web

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.jantursky.kascii.samples.gallery.GalleryApp
import com.jantursky.kascii.samples.gallery.PageRequest
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

private fun pageFromHash(): String? =
    window.location.hash
        .removePrefix("#")
        .ifEmpty { null }

/** A query flag such as `?embed=1`: [default] when absent, otherwise true unless it is "0". */
private fun flag(
    name: String,
    default: Boolean = false,
): Boolean {
    val value = Regex("[?&]$name=([^&]*)").find(window.location.search)?.groupValues?.get(1)
    return if (value == null) default else value != "0"
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun main() {
    var opened = false
    ComposeViewport(document.body!!) {
        var request by remember { mutableStateOf<PageRequest?>(null) }
        DisposableEffect(Unit) {
            // The browser's back and forward buttons step through the pages the gallery pushed.
            val listener: (Event) -> Unit = { request = PageRequest(pageFromHash()) }
            window.addEventListener("popstate", listener)
            onDispose { window.removeEventListener("popstate", listener) }
        }
        GalleryApp(
            initialPage = pageFromHash(),
            pageRequest = request,
            initialAnimated = flag("animated", default = true),
            embed = flag("embed"),
            onPageChange = { id ->
                if (window.location.hash.removePrefix("#") != id) {
                    // The first page is the one that opened, so it takes the place of the empty address.
                    if (opened) {
                        window.history.pushState(
                            null,
                            "",
                            "#$id",
                        )
                    } else {
                        window.history.replaceState(null, "", "#$id")
                    }
                }
                opened = true
            },
        )
    }
}

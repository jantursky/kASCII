package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.core.Motion
import kotlinx.coroutines.flow.first

/** A request to open the page [id]. Two requests are different even for the same id, so each one opens it again. */
class PageRequest(
    val id: String?,
)

const val GALLERY_VERSION = "1.1.0"

private val WideBreakpoint = 720.dp
private val ProseWidth = 800.dp
private val GridWidth = 1240.dp

/**
 * The whole gallery: a nav bar, the current page with its source and a footer.
 *
 * @param initialPage The id of the page to open: a showcase page such as `fields`, or a piece such as `donut`.
 * @param pageRequest A page to open, such as the one a browser's back button returns to. Each request opens its
 *   page, also when it names the page that is already shown by an earlier one.
 * @param onPageChange Called with the id of each page that opens, so a host can keep it in a URL.
 * @param onDarkModeChange Called with the theme in use, so a host can match its system bars.
 * @param initialAnimated Starts with animation on, which is the default. Off holds every piece on its still frame.
 * @param embed Shows only the page's stage, with no nav, controls or source, for embedding in another page.
 */
@Composable
fun GalleryApp(
    modifier: Modifier = Modifier,
    initialPage: String? = null,
    pageRequest: PageRequest? = null,
    initialDark: Boolean = isSystemInDarkTheme(),
    onPageChange: (String) -> Unit = {},
    onDarkModeChange: (Boolean) -> Unit = {},
    initialAnimated: Boolean = true,
    embed: Boolean = false,
) {
    val state =
        remember { GalleryState(initialDark, if (initialAnimated) Motion.System else Motion.Reduced, embed) }
    var pageId by remember { mutableStateOf(pageFor(initialPage).id) }
    LaunchedEffect(pageRequest) { if (pageRequest != null) pageId = pageFor(pageRequest.id).id }
    LaunchedEffect(pageId) { onPageChange(pageId) }
    LaunchedEffect(state.dark) { onDarkModeChange(state.dark) }
    GalleryTheme(state.dark) {
        CompositionLocalProvider(LocalNavigate provides { pageId = it }) {
            val page = pageFor(pageId)
            val frame =
                modifier.fillMaxSize().background(Gallery.colors.void).windowInsetsPadding(WindowInsets.safeDrawing)
            when {
                embed && page.fills -> Box(frame) { page.content(state) }
                embed -> Box(frame.verticalScroll(rememberScrollState()).padding(12.dp)) { page.content(state) }
                else -> Shell(state, page, frame)
            }
        }
    }
}

@Composable
private fun Shell(
    state: GalleryState,
    page: Page,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= WideBreakpoint
        CompositionLocalProvider(LocalWide provides wide) {
            Column(Modifier.fillMaxSize()) {
                NavBar(state, page)
                PageView(state, page)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NavBar(
    state: GalleryState,
    page: Page,
) {
    val wide = LocalWide.current
    val navigate = LocalNavigate.current
    val uri = LocalUriHandler.current
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Gallery.colors.surface2)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = if (wide) 48.dp else 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                "kascii",
                Modifier.padding(end = 16.dp),
                style = Gallery.type.title.copy(color = Gallery.colors.strong),
            )
            if (wide) {
                NavLinks.forEach { NavButton(it.title, it.isCurrent(page)) { navigate(it.id) } }
                ExternalLinks(uri)
            } else {
                Pill(if (menuOpen) "Close" else "Menu", { menuOpen = !menuOpen }, tint = Tint.Amber)
            }
            Pill(if (state.dark) "Light" else "Dark", { state.useDark(!state.dark) }, tint = Tint.Red)
        }
        if (!wide && menuOpen) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                NavLinks.forEach {
                    NavButton(it.title, it.isCurrent(page)) {
                        menuOpen = false
                        navigate(it.id)
                    }
                }
                ExternalLinks(uri)
            }
        }
    }
}

@Composable
private fun ExternalLinks(uri: UriHandler) {
    TextLink("Docs", { uri.openUri(DOCS_URL) })
    TextLink("GitHub", { uri.openUri(GITHUB_URL) })
}

private const val DOCS_URL = "https://kascii-docs.vercel.app/"
private const val GITHUB_URL = "https://github.com/jantursky/kASCII/"

@Composable
private fun NavButton(
    title: String,
    current: Boolean,
    onClick: () -> Unit,
) {
    TextLink(title, onClick, Modifier.semantics { selected = current })
}

@Composable
private fun PageView(
    state: GalleryState,
    page: Page,
) {
    val colors = Gallery.colors
    val wide = LocalWide.current
    val navigate = LocalNavigate.current
    val scroll = rememberScrollState()
    LaunchedEffect(page.id) {
        // A list page opens where it was left; any other starts at the top. The page is laid out after this runs, so
        // wait until it is tall enough to scroll that far.
        val target = if (page.wide) state.scrolls[page.id] ?: 0 else 0
        if (target > 0) snapshotFlow { scroll.maxValue }.first { it >= target }
        scroll.scrollTo(target)
        snapshotFlow { scroll.value }.collect { state.scrolls[page.id] = it }
    }
    Box(Modifier.fillMaxSize().verticalScroll(scroll), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = if (page.wide) GridWidth else ProseWidth)
                .fillMaxWidth()
                .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = if (wide) 48.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(64.dp),
        ) {
            if (page.id != "overview") {
                Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (page.group == Group.Pieces) {
                        TextLink("← All pieces", { navigate("pieces") }, Modifier.offset(x = (-8).dp))
                    }
                    BasicText(
                        page.title,
                        Modifier.semantics { heading() },
                        style = Gallery.type.heading.copy(color = colors.strong),
                    )
                    BasicText(page.summary, style = Gallery.type.body.copy(color = colors.muted))
                }
            }
            page.content(state)
            if (page.sources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeading("The code that runs")
                    page.sources.forEach { name -> ExampleSources[name]?.let { CodePanel(it) } }
                }
            }
            Footer()
        }
    }
}

@Composable
private fun Footer() {
    BasicText(SUPPORT_NOTE, style = Gallery.type.caption.copy(color = Gallery.colors.muted))
}

/** What the gallery states about platform support. Anything not run says so. */
private const val SUPPORT_NOTE =
    "kASCII $GALLERY_VERSION. Checked: desktop JVM (screenshot tests) and the web build (browser tests in Chromium, " +
        "Firefox and WebKit). UNVERIFIED: the Android and iOS apps run on a device or emulator, and the Gpu " +
        "renderer on those. PNG: copied on desktop and iOS (iOS UNVERIFIED), saved as a file in the browser, not " +
        "offered on Android."

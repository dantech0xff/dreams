package com.dantech.dreams.ui.feature.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Multi-stack helper that flattens N per-tab back stacks into a single observable [backStack]
 * suitable for `NavDisplay`. Models the canonical Nav3 BottomNav recipe.
 *
 * - Tab switch via [switchTopLevel] re-orders tabs LRU so system back falls back to the
 *   previously-active tab once the current tab is at depth 1.
 * - Drill-down is [add] / [removeLast]. Tap-current-tab is [popToRoot].
 *
 * Saved state survives both config change and process death via [Saver]; encoding handles
 * the closed set of [Route] subtypes used as tab roots and drill-down keys.
 */
class TopLevelBackStack private constructor(
    initialTopLevelKey: Route,
    initialStacks: Map<Route, List<Route>>,
) {

    var topLevelKey: Route by mutableStateOf(initialTopLevelKey)
        private set

    private val topLevelStacks: LinkedHashMap<Route, SnapshotStateList<Route>> =
        LinkedHashMap<Route, SnapshotStateList<Route>>().apply {
            initialStacks.forEach { (root, items) ->
                put(root, mutableStateListOf<Route>().also { it.addAll(items) })
            }
        }

    /** Flattened, observable back stack: concatenation of every tab's stack in LRU order. */
    val backStack: SnapshotStateList<Route> = mutableStateListOf<Route>().also { rebuild(it) }

    private fun rebuild(target: SnapshotStateList<Route>) {
        target.clear()
        topLevelStacks.values.forEach { target.addAll(it) }
    }

    fun switchTopLevel(root: Route) {
        if (topLevelKey == root) return
        if (root in topLevelStacks) {
            // LRU re-order: move chosen tab to end so its top entry is the back-stack tail.
            topLevelStacks.remove(root)?.let { topLevelStacks[root] = it }
        } else {
            // First visit this session: seed a fresh stack rooted at this tab.
            topLevelStacks[root] = mutableStateListOf<Route>().also { it.add(root) }
        }
        topLevelKey = root
        rebuild(backStack)
    }

    fun add(key: Route) {
        topLevelStacks[topLevelKey]?.add(key)
        rebuild(backStack)
    }

    fun removeLast() {
        val current = topLevelStacks[topLevelKey] ?: return
        if (current.size > 1) {
            current.removeAt(current.lastIndex)
            rebuild(backStack)
            return
        }
        // At a tab root: drop this tab and fall back to the previous LRU tab.
        // When only one tab is seeded, this is a no-op so the flattened backStack stays
        // at size 1 — NavDisplay disables its BackHandler and system back exits the activity.
        if (topLevelStacks.size > 1) {
            topLevelStacks.remove(topLevelKey)
            topLevelKey = topLevelStacks.keys.last()
            rebuild(backStack)
        }
    }

    fun popToRoot() {
        val current = topLevelStacks[topLevelKey] ?: return
        if (current.size <= 1) return
        val root = current.first()
        current.clear()
        current.add(root)
        rebuild(backStack)
    }

    internal fun snapshotForSaver(): Pair<Route, Map<Route, List<Route>>> =
        topLevelKey to topLevelStacks.mapValues { it.value.toList() }

    companion object {
        fun create(start: Route): TopLevelBackStack {
            // Seed only the start tab. Other tabs materialise on first switch so a back press
            // from the start tab leaves a single-entry flattened stack and the activity exits.
            val seed = mapOf(start to listOf<Route>(start))
            return TopLevelBackStack(start, seed)
        }

        internal fun restore(
            topLevelKey: Route,
            stacks: Map<Route, List<Route>>,
        ): TopLevelBackStack = TopLevelBackStack(topLevelKey, stacks)
    }
}

private fun encodeRoute(r: Route): String = when (r) {
    is Route.LessonRoot -> "LR"
    is Route.LessonList -> "LL:${r.categoryName}"
    is Route.LessonDetail -> "LD:${r.lessonId}"
    is Route.ShowcaseRoot -> "SR"
    is Route.Showcase -> "SH:${r.lessonId}"
    is Route.SettingsRoot -> "SS"
}

private fun decodeRoute(s: String): Route = when {
    s == "LR" -> Route.LessonRoot
    s == "SR" -> Route.ShowcaseRoot
    s == "SS" -> Route.SettingsRoot
    s.startsWith("LL:") -> Route.LessonList(s.removePrefix("LL:"))
    s.startsWith("LD:") -> Route.LessonDetail(s.removePrefix("LD:"))
    s.startsWith("SH:") -> Route.Showcase(s.removePrefix("SH:"))
    else -> error("Unknown encoded route: $s")
}

private val TopLevelBackStackSaver: Saver<TopLevelBackStack, List<List<String>>> = Saver(
    save = { tbs ->
        val (top, stacks) = tbs.snapshotForSaver()
        buildList {
            add(listOf("TOP", encodeRoute(top)))
            stacks.forEach { (root, items) ->
                add(listOf(encodeRoute(root)) + items.map(::encodeRoute))
            }
        }
    },
    restore = { saved ->
        // On schema drift / unknown encoded routes, return null so rememberSaveable falls
        // back to the initial-value lambda — losing nav state but never crashing.
        runCatching {
            val top = decodeRoute(saved[0][1])
            val stacks = saved.drop(1).associate { row ->
                decodeRoute(row[0]) to row.drop(1).map(::decodeRoute)
            }
            TopLevelBackStack.restore(top, stacks)
        }.getOrNull()
    },
)

@androidx.compose.runtime.Composable
fun rememberTopLevelBackStack(start: Route): TopLevelBackStack =
    rememberSaveable(saver = TopLevelBackStackSaver) { TopLevelBackStack.create(start) }

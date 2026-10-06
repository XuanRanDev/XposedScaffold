package dev.xuanran.xposed.startup

import dev.xuanran.xposed.api.HookConfig

/** Loader-owned configuration channel. It intentionally stays outside the framework Hook bridge. */
fun interface HostConfigProvider {
    /** Returns null when the framework cannot expose module preferences to the host process. */
    fun open(): HookConfig?
}

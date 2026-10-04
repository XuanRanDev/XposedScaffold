package dev.xuanran.xposed.runtime

import dev.xuanran.xposed.api.HookContext
import dev.xuanran.xposed.api.HookItem
import dev.xuanran.xposed.api.HookUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HookRuntimeTest {
    @Test
    fun apiHookAlwaysUsesApiUiType() {
        assertEquals(HookUiType.API, AlwaysOnHook.metadata.uiType)
    }

    @Test
    fun emptyProcessSetMatchesEveryProcess() {
        assertTrue(HookRuntime.matchesProcess(emptySet(), "example.host", "example.host:service"))
    }

    @Test
    fun mainAliasOnlyMatchesMainProcess() {
        assertTrue(HookRuntime.matchesProcess(setOf("main"), "example.host", "example.host"))
        assertFalse(HookRuntime.matchesProcess(setOf("main"), "example.host", "example.host:service"))
    }
}

@HookItem(
    id = "test.always_on",
    path = "Tests",
    title = "Always on",
)
private object AlwaysOnHook : ApiHook() {
    override fun install(context: HookContext) = Unit
}

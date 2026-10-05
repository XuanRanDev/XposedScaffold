package dev.xuanran.xposed.loader.modern

import dev.xuanran.xposed.api.HookCallback
import dev.xuanran.xposed.api.HookParam
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernHookerTest {
    @Test
    fun `intercept dispatches before original and after`() {
        val events = mutableListOf<String>()
        val chain = chain { args ->
            events += "original:${args.single()}"
            "result"
        }
        val hooker = ModernHooker(object : HookCallback {
            override fun before(param: HookParam) {
                events += "before"
                param.args[0] = "changed"
            }

            override fun after(param: HookParam) {
                events += "after:${param.result}"
            }
        })

        assertEquals("result", hooker.intercept(chain))
        assertEquals(listOf("before", "original:changed", "after:result"), events)
    }

    @Test
    fun `before result skips original call`() {
        var proceeded = false
        val hooker = ModernHooker(object : HookCallback {
            override fun before(param: HookParam) {
                param.result = "short-circuit"
            }
        })

        val result = hooker.intercept(chain {
            proceeded = true
            "unexpected"
        })

        assertEquals("short-circuit", result)
        assertTrue(!proceeded)
    }

    @Suppress("UNCHECKED_CAST")
    private fun chain(proceed: (Array<Any?>) -> Any?): XposedInterface.Chain {
        val executable = Any::class.java.getDeclaredMethod("toString")
        return Proxy.newProxyInstance(
            XposedInterface.Chain::class.java.classLoader,
            arrayOf(XposedInterface.Chain::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getExecutable" -> executable
                "getThisObject" -> null
                "getArgs" -> listOf("input")
                "proceed" -> proceed((args?.firstOrNull() as? Array<Any?>) ?: arrayOf("input"))
                else -> error("Unexpected Chain method: ${method.name}")
            }
        } as XposedInterface.Chain
    }
}

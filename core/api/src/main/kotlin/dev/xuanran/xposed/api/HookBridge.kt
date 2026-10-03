package dev.xuanran.xposed.api

import java.lang.reflect.Executable
import android.content.SharedPreferences

/**
 * Xposed 框架能力的最小抽象层。
 *
 * 业务 Hook 只能依赖此接口，不能直接依赖 Legacy Xposed 或 libxposed。这样同一份功能代码可以
 * 同时运行在传统 API 82 与现代 API 101+ 上，框架差异则被限制在 loader 模块内部。
 */
interface HookBridge {
    /** 当前运行时框架的可读名称，例如 LSPosed。 */
    val frameworkName: String
    /** 当前运行时框架版本字符串。 */
    val frameworkVersion: String
    /** 当前运行时提供的 Xposed API 版本。 */
    val apiVersion: Int

    /** Hook 一个方法或构造函数；优先级越大，before 越早、after 越晚。 */
    fun hook(executable: Executable, priority: Int = 50, callback: HookCallback): UnhookHandle
    /** 写入框架日志，业务层不应直接调用某一代框架的日志 API。 */
    fun log(priority: Int, tag: String, message: String, throwable: Throwable? = null)
    /**
     * 获取模块侧可写、宿主侧可读的远程配置。
     * 旧框架可能无法可靠实现，所以允许返回 null 并由 startup 选择后备方案。
     */
    fun remotePreferences(name: String): SharedPreferences? = null
}

/** 可幂等取消一个已经安装的 Hook。 */
fun interface UnhookHandle {
    fun unhook()
}

/** 框架无关的 before/after 回调；默认空实现允许功能只覆盖所需阶段。 */
interface HookCallback {
    fun before(param: HookParam) = Unit
    fun after(param: HookParam) = Unit
}

/**
 * 一次方法调用的可变视图。
 * before 中修改 [args] 可改变实参；设置 [result] 或 [throwable] 可提前结束调用。
 */
interface HookParam {
    val executable: Executable
    val thisObject: Any?
    val args: Array<Any?>
    var result: Any?
    var throwable: Throwable?
    val hasResult: Boolean
}

package dev.xuanran.xposed.api

/** Template identity generated from the root gradle.properties file. */
object ModuleConfig {
    val NAME: String = BuildConfig.MODULE_NAME
    val PACKAGE: String = BuildConfig.MODULE_PACKAGE
    val LOG_TAG: String = BuildConfig.LOG_TAG
    val PREFERENCES_NAME: String = BuildConfig.PREFERENCES_NAME
    val DEX_PREFERENCES_NAME: String = "${BuildConfig.PREFERENCES_NAME}_dex"
}

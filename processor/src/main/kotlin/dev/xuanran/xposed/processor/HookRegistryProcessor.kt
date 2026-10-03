package dev.xuanran.xposed.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

private const val HOOK_ITEM = "dev.xuanran.xposed.api.HookItem"

/** KSP 服务入口，由 META-INF/services 自动发现。 */
class HookRegistryProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        HookRegistryProcessor(environment.codeGenerator, environment.logger)
}

private class HookRegistryProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // KSP 可能多轮调用 process，聚合注册表只能生成一次。
        if (generated) return emptyList()
        val symbols = resolver.getSymbolsWithAnnotation(HOOK_ITEM).toList()
        val deferred = symbols.filterNot { it.validate() }
        if (deferred.isNotEmpty()) return deferred

        val classes = symbols.filterIsInstance<KSClassDeclaration>()
        val ids = mutableSetOf<String>()
        classes.forEach { declaration ->
            // object 能保证进程内单例，也避免生成代码猜测构造函数参数。
            if (declaration.classKind != ClassKind.OBJECT) {
                logger.error("@HookItem must annotate a Kotlin object", declaration)
            }
            val id = declaration.annotations.first { it.annotationType.resolve().declaration.qualifiedName?.asString() == HOOK_ITEM }
                .arguments.first { it.name?.asString() == "id" }.value as String
            if (!ids.add(id)) logger.error("Duplicate HookItem id: $id", declaration)
        }

        val hookFeature = ClassName("dev.xuanran.xposed.api", "HookFeature")
        // 生成直接类型引用而不是字符串反射，重命名和混淆时更安全。
        val entries = CodeBlock.builder().add("return listOf(\n").indent().apply {
            classes.forEach { add("%T,\n", it.toClassName()) }
        }.unindent().add(")\n").build()
        val function = FunSpec.builder("createHooks")
            .returns(LIST.parameterizedBy(hookFeature))
            .addCode(entries)
            .build()

        FileSpec.builder("dev.xuanran.xposed.generated", "GeneratedHookRegistry")
            .addFunction(function)
            .build()
            .writeTo(codeGenerator, Dependencies(true, *classes.mapNotNull { it.containingFile }.toTypedArray()))
        generated = true
        return emptyList()
    }
}

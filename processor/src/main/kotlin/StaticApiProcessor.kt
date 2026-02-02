package dev.tmpod.staticapi

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.writeTo

val FULL_ANNOTATION_NAME = requireNotNull(StaticApi::class.qualifiedName) { "Couldn't get annotation name!" }
val SIMPLE_ANNOTATION_NAME = requireNotNull(StaticApi::class.simpleName) { "Couldn't get annotation name!" }

val INTERNAL_ANNOTATION =
    AnnotationSpec.builder(ClassName("org.jetbrains.annotations", "ApiStatus", "Internal")).build()

val DELEGATE_KDOC =
    """
    Internal mutable delegate. Should be set during initialization.
    
    This property is marked [ApiStatus.Internal] to discourage direct access.
    """.trimIndent()

val KSAnnotation.fqn get() = annotationType.resolve().declaration.qualifiedName?.asString()

class StaticApiProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver) =
        listOf<KSAnnotated>().also {
            resolver.getSymbolsWithAnnotation(FULL_ANNOTATION_NAME)
                .filterIsInstance<KSClassDeclaration>()
                .forEach { d -> d.processInterface() }
        }

    private fun KSClassDeclaration.processInterface() {
        if (classKind != ClassKind.INTERFACE) {
            logger.error("@${SIMPLE_ANNOTATION_NAME} can only be applied to interface declarations", this)
            return
        }

        val name = simpleName.asString()  // interface name
        val annotation = annotations.find { it.fqn == FULL_ANNOTATION_NAME }!!

        // Compute resulting object name
        var objectName = (annotation.arguments.first().value as? String) ?: return
        if (objectName.isBlank()) {
            if (name.startsWith('I')) objectName = name.substring(1)
            else {
                logger.error(
                    "@${SIMPLE_ANNOTATION_NAME} must be applied to an interface named ISomething or be passed a specific name for the generated object.",
                    this,
                )
                return
            }
        }

        // Get all methods from the interface to include in generated object
        val methods = collectInterfaceMethods()
        if (methods.isEmpty()) {
            logger.warn("No methods found in interface $name", this)
        }

        generateObject(objectName, methods)
    }

    private fun KSClassDeclaration.collectInterfaceMethods(): List<KSFunctionDeclaration> {
        // Get all declared functions
        val methods = getAllFunctions()
            .filter { it.parentDeclaration == this }
            .toMutableList()

        // Recursively get methods from super interfaces
        superTypes
            .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
            .filter { it.classKind == ClassKind.INTERFACE }
            .forEach { superInterface ->
                methods.addAll(superInterface.collectInterfaceMethods())
            }

        return methods.distinctBy {
            it.simpleName.asString() + it.parameters.map { p -> p.type.resolve() }
        }
    }

    private fun KSClassDeclaration.generateObject(
        objectName: String,
        methods: List<KSFunctionDeclaration>
    ) {
        FileSpec.builder(packageName.asString(), "${objectName}_IMPL").run {
            addType(
                TypeSpec.objectBuilder(objectName).run {
                    addKdoc(docString ?: "API implementation of [${simpleName.asString()}]")
                    addProperty(createDelegateProperty())
                    methods.forEach { method ->
                        addFunction(method.createForwardingMethod())
                    }
                    build()
                }
            )
            build()
        }.writeTo(codeGenerator, Dependencies(aggregating = true, containingFile!!))
    }

    private fun KSClassDeclaration.createDelegateProperty() =
        PropertySpec.builder("delegate", toClassName()).run {
            mutable(true)
            addModifiers(KModifier.PUBLIC)
            addModifiers(KModifier.LATEINIT)
            addAnnotation(INTERNAL_ANNOTATION)
            addKdoc(DELEGATE_KDOC)
            build()
        }

    private fun KSFunctionDeclaration.createForwardingMethod(): FunSpec {
        val methodName = simpleName.asString()

        return FunSpec.builder(methodName).run {
            docString?.let(::addKdoc)
            addAnnotation(JvmStatic::class)

            // Handle suspend functions
            if (modifiers.contains(KModifier.SUSPEND)) addModifiers(KModifier.SUSPEND)

            // Add type parameters
            val typeParamResolver = typeParameters.toTypeParameterResolver()
            typeParameters.forEach { param ->
                val name = param.name.asString()
                val bounds = param.bounds.map { it.toTypeName(typeParamResolver) }.toList()
                addTypeVariable(
                    if (bounds.isEmpty()) TypeVariableName(name)
                    else TypeVariableName(name, bounds)
                )
            }

            // Add parameters
            val params = this@createForwardingMethod.parameters
            params.forEach { param ->
                ParameterSpec.builder(param.name!!.asString(), param.type.toTypeName(typeParamResolver))
                    .apply { if (param.isVararg) addModifiers(KModifier.VARARG) }
                    .build()
                    .let(::addParameter)
            }

            // Set return type
            val returnType = returnType?.toTypeName(typeParamResolver) ?: UNIT
            returns(returnType)

            // Generate delegation call
            val paramString = params.joinToString(", ") { p ->
                val name = p.name!!.asString()
                if (p.isVararg) "*$name" else name
            }

            if (returnType == UNIT) {
                addStatement("delegate.%N($paramString)", methodName)
            } else {
                addStatement("return delegate.%N($paramString)", methodName)
            }

            build()
        }
    }
}

class StaticApiProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        StaticApiProcessor(environment.codeGenerator, environment.logger)
}

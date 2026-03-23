@file:OptIn(ExperimentalCompilerApi::class)

package dev.tmpod.staticapi.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import dev.tmpod.staticapi.StaticApiProcessorProvider
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

data class CompileResult(
    val exitCode: String,
    val messages: String,
    val outputDirectory: File
)

val TEST_FILE_BASE =
    """
    package test
    import dev.tmpod.staticapi.StaticApi
    """.trimIndent()

/** Sanitizes nullable types to be used in regex matching */
fun String.sanitizeType() = replace("?", "\\?")

/**
 * Base class for KSP processor compilation tests.
 *
 * Provides infrastructure for compiling Kotlin source files through the StaticApiProcessor
 * and asserting on generated code.
 */
abstract class CompilationTestBase {

    /** Compiles the given source files with the StaticApiProcessor. */
    fun compile(source: SourceFile, expectSuccess: Boolean = true): CompileResult {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            inheritClassPath = true
            useKsp2()
            symbolProcessorProviders = mutableListOf(StaticApiProcessorProvider())
        }

        val result = compilation.compile()

        val exitCode = try {
            result.exitCode.toString()
        } catch (_: Exception) {
            "UNKNOWN"
        }

        val messages = try {
            result.messages
        } catch (_: Exception) {
            ""
        }

        val outputDir = try {
            result.outputDirectory
        } catch (_: Exception) {
            File("/tmp/fallback")
        }

        val compileResult = CompileResult(exitCode, messages, outputDir)

        if (expectSuccess) {
            assertThat(compileResult.exitCode)
                .withFailMessage("Compilation failed: ${compileResult.messages}")
                .isEqualTo("OK")
        }

        return compileResult
    }

    /** Creates a SourceFile from Kotlin source code. */
    fun sourceFile(fileName: String, content: String): SourceFile =
        SourceFile.kotlin(fileName, TEST_FILE_BASE + content)

    /** Retrieves the content of a generated file. */
    fun getGeneratedFileContent(result: CompileResult, fileName: String): String {
        val file = findGeneratedFile(result, fileName)
        assertThat(file).withFailMessage("Generated file '$fileName' not found").isNotNull()
        return file!!.readText()
    }

    /** Asserts that a compilation succeeded (exit code OK). */
    fun assertCompilationSucceeds(result: CompileResult) {
        assertThat(result.exitCode)
            .withFailMessage("Compilation failed: ${result.messages}")
            .isEqualTo("OK")
    }

    /** Asserts that a compilation failed and contains the expected error message. */
    fun assertCompilationFails(result: CompileResult, expectedMessage: String) {
        assertThat(result.exitCode)
            .withFailMessage("Compilation should have failed but succeeded")
            .isNotEqualTo("OK")

        assertThat(result.messages)
            .withFailMessage("Expected error message containing: '$expectedMessage'")
            .contains(expectedMessage)
    }

    /**
     * Finds a generated file by name, searching recursively in KSP output directories.
     *
     * KSP generates files in `ksp/sources/kotlin/` subdirectories.
     */
    fun findGeneratedFile(result: CompileResult, fileName: String): File? {
        // Try to find in KSP output directory structure
        val baseDir = result.outputDirectory.parentFile ?: result.outputDirectory
        val kspSourceDir = baseDir.resolve("ksp/sources/kotlin")

        return if (kspSourceDir.exists()) {
            kspSourceDir.walkTopDown().find { it.isFile && it.name == fileName }
        } else {
            // Fallback to searching in output directory
            result.outputDirectory.walkTopDown().find { it.isFile && it.name == fileName }
        }
    }

    /**
     * Compiles the given source files with the StaticApiProcessor and asserts that the compilation was successful,
     * returning the contents of the generated file.
     */
    fun compileSourceAndAssertSuccess(sourceFileName: String, generatedFileName: String, content: String): String {
        val source = sourceFile(sourceFileName, content)

        val result = compile(source)
        assertCompilationSucceeds(result)

        return getGeneratedFileContent(result, generatedFileName)
    }


    /** Asserts that the generated code contains a method with the given signature. */
    fun assertContainsMethod(
        code: String,
        methodName: String,
        returnType: String,
        vararg arguments: Pair<String, String>
    ) {
        val argPattern = arguments.joinToString("") { (n, t) -> "\\s*`?$n`?\\s*:\\s*${t.sanitizeType()}\\s*,?" }
        val pattern = """(?s).*fun\s*`?$methodName`?\(\s*${argPattern}\s*\)\s*:\s*${returnType.sanitizeType()}.*"""
        assertThat(code)
            .withFailMessage("Expected method 'fun ${methodName}(${arguments.joinToString { (n, t) -> "$n: $t" }}): $returnType' not found in:\n$code")
            .matches(pattern)
    }

    /** Asserts that the generated code contains a property with the given name and type. */
    fun assertContainsProperty(code: String, propertyName: String, propertyType: String) {
        val pattern = """(?s).*va[rl]\s+`?$propertyName`?\s*:\s*${propertyType.sanitizeType()}.*"""
        assertThat(code)
            .withFailMessage("Expected property '$propertyName: $propertyType' not found in:\n$code")
            .matches(pattern)
    }

    /** Asserts that the generated code contains the given annotation. */
    fun assertHasAnnotation(code: String, annotationName: String) {
        assertThat(code)
            .withFailMessage("Expected annotation '$annotationName' not found in:\n$code")
            .contains("@$annotationName")
    }

    /** Asserts that the generated code contains KDoc with the given content. */
    fun assertHasKDoc(code: String, expectedKDoc: String) {
        assertThat(code)
            .withFailMessage("Expected KDoc containing '$expectedKDoc' not found in:\n$code")
            .contains(expectedKDoc)
    }
}

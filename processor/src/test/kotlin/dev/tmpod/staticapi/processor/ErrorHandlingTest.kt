package dev.tmpod.staticapi.processor

import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for error handling and validation.
 *
 * These tests verify that the processor properly rejects invalid usage
 * and provides clear error messages.
 */
class ErrorHandlingTest : CompilationTestBase() {

    @Test
    fun testAnnotationOnClass() {
        val source = sourceFile(
            "InvalidClass.kt",
            """
            @StaticApi
            class InvalidClass {
                fun doSomething() = "test"
            }
            """
        )

        val result = compile(source, expectSuccess = false)

        assertCompilationFails(result, "can only be applied to interface declarations")
    }

    @Test
    fun testAnnotationOnObject() {
        val source = sourceFile(
            "InvalidObject.kt",
            """
            @StaticApi
            object InvalidObject {
                fun doSomething() = "test"
            }
            """
        )

        val result = compile(source, expectSuccess = false)

        assertCompilationFails(result, "can only be applied to interface declarations")
    }

    @Test
    fun testMissingObjectNameForNonIPrefixedInterface() {
        val source = sourceFile(
            "IncredibleApi.kt",
            """
            @StaticApi
            interface IncredibleApi {
                fun doSomething(): String
            }
            """
        )

        val result = compile(source, expectSuccess = false)

        assertCompilationFails(
            result,
            "must be applied to an interface named ISomething or be passed a specific name"
        )
    }

    @Test
    fun testEmptyObjectNameOnNonPrefixedInterface() {
        val source = sourceFile(
            "MyApi.kt",
            """
            @StaticApi(objectName = "")
            interface MyApi {
                fun doSomething(): String
            }
            """
        )

        val result = compile(source, expectSuccess = false)

        assertCompilationFails(
            result,
            "must be applied to an interface named ISomething or be passed a specific name"
        )
    }
}

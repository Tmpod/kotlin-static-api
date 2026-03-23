package dev.tmpod.staticapi.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for method modifier preservation.
 *
 * These tests verify that method modifiers like suspend, inline, infix, and
 * operator are correctly preserved in the generated static methods.
 */
class ModifiersTest : CompilationTestBase() {

    @Test
    fun testSuspendModifier() {
        val generated = compileSourceAndAssertSuccess(
            "IAsyncApi.kt", "AsyncApi_IMPL.kt",
            """
            @StaticApi
            interface IAsyncApi {
                suspend fun fetchData(): String
                suspend fun processAsync(data: String): Boolean
            }
            """
        )

        // Verify suspend modifier is preserved
        assertThat(generated).contains("suspend fun fetchData")
        assertThat(generated).contains("suspend fun processAsync")

        // Verify delegation is correct
        assertThat(generated).contains("= delegate.fetchData()")
        assertThat(generated).contains("= delegate.processAsync(data)")
    }

    @Test
    fun testVarargParameter() {
        val generated = compileSourceAndAssertSuccess(
            "IVarargApi.kt", "VarargApi_IMPL.kt",
            """
            @StaticApi
            interface IVarargApi {
                fun format(template: String, vararg args: Any): String
                fun printAll(vararg items: String)
                fun combine(first: String, vararg rest: String): String
            }
            """
        )

        // Verify vararg parameters are preserved
        assertThat(generated).contains("fun format(template: String, vararg args: Any): String")
        assertThat(generated).contains("fun printAll(vararg items: String)")
        assertThat(generated).contains("fun combine(first: String, vararg rest: String): String")

        // Verify varargs are spread in delegation calls
        assertThat(generated).contains("delegate.format(template, *args)")
        assertThat(generated).contains("delegate.printAll(*items)")
        assertThat(generated).contains("delegate.combine(first, *rest)")
    }
}

package dev.tmpod.staticapi.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for generic type parameter preservation.
 *
 * These tests verify that type parameters on methods are correctly
 * preserved in the generated forwarding methods.
 */
class GenericsTest : CompilationTestBase() {

    @Test
    fun testMethodWithSingleTypeParameter() {
        val generated = compileSourceAndAssertSuccess(
            "IGenericApi.kt", "GenericApi_IMPL.kt",
            """
            @StaticApi
            interface IGenericApi {
                fun <T> getValue(key: String): T
                fun <T> process(item: T): Boolean
            }
            """
        )

        // Verify type parameters are preserved in method signatures
        assertThat(generated).contains("fun <T> getValue(key: String): T")
        assertThat(generated).contains("fun <T> process(item: T): Boolean")

        // Verify methods delegate with type parameters intact
        assertThat(generated).contains("delegate.getValue(key)")
        assertThat(generated).contains("delegate.process(item)")
    }

    @Test
    fun testMethodWithMultipleTypeParameters() {
        val generated = compileSourceAndAssertSuccess(
            "IMultiGeneric.kt", "MultiGeneric_IMPL.kt",
            """
            @StaticApi
            interface IMultiGeneric {
                fun <K, V> transform(key: K): V
                fun <A, B, C> combine(first: A, second: B): C
                fun <T> identity(value: T): T
            }
            """
        )

        // Verify multiple type parameters are preserved
        assertThat(generated).contains("fun <K, V> transform(key: K): V")
        assertThat(generated).contains("fun <A, B, C> combine(first: A, second: B): C")
        assertThat(generated).contains("fun <T> identity(`value`: T): T")

        // Verify delegation calls preserve type information implicitly
        assertThat(generated).contains("delegate.transform(key)")
        assertThat(generated).contains("delegate.combine(first, second)")
        assertThat(generated).contains("delegate.identity(value)")
    }
}

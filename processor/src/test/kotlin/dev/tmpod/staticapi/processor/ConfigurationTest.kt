package dev.tmpod.staticapi.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for @StaticApi annotation configuration parameters.
 *
 * These tests verify that the objectName, delegateName, and volatileDelegate
 * parameters work correctly when specified.
 */
class ConfigurationTest : CompilationTestBase() {

    @Test
    fun testCustomObjectName() {
        val generated = compileSourceAndAssertSuccess(
            "MyApi.kt", "CustomApi_IMPL.kt",
            """
            @StaticApi(objectName = "CustomApi")
            interface MyApi {
                fun doSomething(): String
            }
            """
        )

        // Verify object name is the custom one, not the interface name
        assertThat(generated).contains("object CustomApi")
        assertThat(generated).doesNotContain("object MyApi")

        // Verify delegate is still typed with the interface
        assertContainsProperty(generated, "delegate", "MyApi")
    }

    @Test
    fun testCustomDelegateName() {
        val generated = compileSourceAndAssertSuccess(
            "IApi.kt", "Api_IMPL.kt",
            """
            @StaticApi(delegateName = "implementation")
            interface IApi {
                fun execute(): String
                fun process(value: Int): Boolean
            }
            """
        )

        // Verify property name is the custom delegate name
        assertContainsProperty(generated, "implementation", "IApi")
        assertThat(generated).doesNotContain("delegate: IApi")

        // Verify forwarding methods use the custom delegate name
        assertThat(generated).contains("implementation.execute()")
        assertThat(generated).contains("implementation.process(value)")

        // Verify methods call the correctly named property
        assertThat(generated).doesNotContain("delegate.execute()")
        assertThat(generated).doesNotContain("delegate.process(value)")
    }

    @Test
    fun testVolatileDelegateTrue() {
        val generated = compileSourceAndAssertSuccess(
            "IThreadSafeApi.kt", "ThreadSafeApi_IMPL.kt",
            """
            @StaticApi(volatileDelegate = true)
            interface IThreadSafeApi {
                fun getValue(): String
            }
            """
        )

        assertHasAnnotation(generated, "Volatile")
        assertHasAnnotation(generated, "ApiStatus.Internal")
        assertContainsProperty(generated, "delegate", "IThreadSafeApi")
    }
}

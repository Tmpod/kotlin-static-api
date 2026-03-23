package dev.tmpod.staticapi.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for interface inheritance and method collection.
 *
 * These tests verify that the processor correctly collects methods from
 * parent interfaces and includes them in the generated object.
 */
class InheritanceTest : CompilationTestBase() {

    @Test
    fun testSingleLevelInheritance() {
        val generated = compileSourceAndAssertSuccess(
            "IHierarchy.kt", "Child_IMPL.kt",
            """
            interface IBase {
                fun baseMethod(): String
            }

            @StaticApi
            interface IChild : IBase {
                fun childMethod(): String
            }
            """
        )

        // Verify both parent and child methods are included
        assertContainsMethod(generated, "baseMethod", "String")
        assertContainsMethod(generated, "childMethod", "String")

        // Verify both methods delegate correctly
        assertThat(generated).contains("delegate.baseMethod()")
        assertThat(generated).contains("delegate.childMethod()")

        // Verify delegate is typed with the annotated interface
        assertContainsProperty(generated, "delegate", "IChild")
    }

    @Test
    fun testMultiLevelInheritance() {
        val generated = compileSourceAndAssertSuccess(
            "IMultiLevel.kt", "Child_IMPL.kt",
            """
            interface IGrandparent {
                fun grandparentMethod(): String
            }

            interface IParent : IGrandparent {
                fun parentMethod(): String
            }

            @StaticApi
            interface IChild : IParent {
                fun childMethod(): String
            }
            """
        )

        // Verify all methods from hierarchy are included
        assertContainsMethod(generated, "grandparentMethod", "String")
        assertContainsMethod(generated, "parentMethod", "String")
        assertContainsMethod(generated, "childMethod", "String")

        // Verify all methods delegate
        assertThat(generated).contains("delegate.grandparentMethod()")
        assertThat(generated).contains("delegate.parentMethod()")
        assertThat(generated).contains("delegate.childMethod()")
    }

    @Test
    fun testMultipleInterfaceInheritance() {
        val generated = compileSourceAndAssertSuccess(
            "IMultiple.kt", "Combined_IMPL.kt",
            """
            interface IBase1 {
                fun method1(): String
                fun sharedMethod(): Int
            }

            interface IBase2 {
                fun method2(): String
            }

            @StaticApi
            interface ICombined : IBase1, IBase2 {
                fun method3(): String
            }
            """
        )

        // Verify methods from both parents are included
        assertContainsMethod(generated, "method1", "String")
        assertContainsMethod(generated, "method2", "String")
        assertContainsMethod(generated, "sharedMethod", "Int")
        assertContainsMethod(generated, "method3", "String")

        // Verify all delegate correctly
        assertThat(generated).contains("delegate.method1()")
        assertThat(generated).contains("delegate.method2()")
        assertThat(generated).contains("delegate.sharedMethod()")
        assertThat(generated).contains("delegate.method3()")
    }
}

package dev.tmpod.staticapi

/**
 * Marks interfaces that should be exported as static APIs.
 *
 * This will generate the object with [JvmStatic] forwarding methods
 * and an internal mutable delegate that can be replaced if needed.
 * If the interface is named `IFoo`, the object will be named `Foo`.
 * If it isn't, you must provide a name with the [objectName] argument.
 *
 * **Note:** At the moment, this annotation does **not** support properties, only methods.
 *
 * Example:
 * ```
 * @StaticApi
 * interface ISomeApi {
 *   fun foo(): Foo
 *   fun isBar(foo: Foo): Boolean
 * }
 *
 * // Generated object (roughly)
 * object SomeApi {
 *   @ApiStatus.Internal lateinit var delegate: ISomeApi
 *   @JvmStatic fun foo() = delegate.foo()
 *   @JvmStatic fun isBar(foo: Foo) = delegate.isBar(foo)
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class StaticApi(val objectName: String = "")

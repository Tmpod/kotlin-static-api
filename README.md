# Static API generator for Kotlin

A simple KSP annotation processor that generates static objects implementing a given API interface, with swappable implementation delegates.

## Usage

Add the KSP plugin to your Gradle script:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.4"
}
```

And add the annotations and the processor to your dependencies:

```kotlin
dependencies {
    implementation("dev.tmpod:staticapi-annotations:0.1")
    ksp("dev.tmpod:staticapi-processor:0.1")
}
```

Alternatively, you can add these lines to your version catalog (`libs.versions.toml`)

```yaml
[versions]
# ...
ksp = "2.3.4"
static-api = "0.1"

[libraries]
# ...
staticApi-annotations = { module = "dev.tmpod:static-api-annotations", version.ref = "static-api" }
staticApi-processor = { module = "dev.tmpod:static-api-processor", version.ref = "static-api" }

[plugins]
# ...
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Now, declare an interface for your API (and document it), then annotate it with [`@StaticApi`](./annotations/src/main/kotlin/StaticApi.kt).

```kotlin
/**
 * API to work with foo and bar.
 */
interface IFooBar {
    /** Makes a new foo. */
    fun makeFoo(): Foo

    /** Returns true if the given foo is also a bar. */
    fun isBar(foo: Foo): Boolean
}
```

The KSP processor will generate an object roughly equivalent to the following:

```kotlin
/**
 * API to work with foo and bar.
 */
object FooBar {
    @ApiStatus.Internal lateinit var delegate: IFooBar

    /** Makes a new foo. */
    @JvmStatic fun makeFoo() = delegate.makeFoo()

    /** Returns true if the given foo is also a bar. */
    @JvmStatic fun isBar(foo: Foo) = delegate.isBar(foo)
}
```

You can then use it like `FooBar.makeFoo()` in both Java and Kotlin.

## Motivation

This was originally developed for Bukkit/Paper APIs written in Kotlin but provided/consumed by either Kotlin or Java.
In this context, APIs are written on a separate module and then implemented by a plugin which is loaded as a (soft-)dependency to the consumers. The API module includes a class with either

1. A static getter to the implementation instance
2. Static methods that delegate to an underlying implementation instance.

Personally, I dislike having to write `FooApi.getInstance().method()` or `FooApi.INSTANCE.method()`, it just adds visual noise and makes code harder to read and review, which pretty much discards option 1, leaving me with implementing delegating static methods.
Fortunately, in Kotlin, we have class delegation, which helps reduce boilerplate code in a lot of cases. In short, you can make a class or object implement an interface by delegating all method implementations to an existing instance (e.g. top-level variable or class parameter).
Unfortunately, however, while it works with objects, it doesn't work with [`@JvmStatic`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.jvm/-jvm-static) (which is Kotlin's way of making true static methods), meaning you have to write `FooApi.INSTANCE.method()` in Java (see [this SO thread](https://stackoverflow.com/a/50368401)). No can do.

So, without other options, and considering I was curious about exploring Kotlin's metaprogramming more for a while, I ended up writing this small processor with KSP.
The generated object doesn't implement the annotated interface because you cannot override with static methods. Still, the delegate is typed with it and all the methods and their doc comments are correctly ported over, making it quite seamless.


// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
// FULL_JDK
// FILE: box.kt

@java.lang.annotation.Repeatable(As::class)
annotation class A(val value: String)

annotation class As(val value: Array<A>)

@A("O")
@A("")
@A("K")
class Z

fun box(): String {
    val annotations = Z::class.java.annotations.filter { it.annotationClass != Metadata::class }
    val aa = annotations.singleOrNull() ?: return "Fail 1: $annotations"
    if (aa !is As) return "Fail 2: $aa"

    val a = aa.value.asList()
    if (a.size != 3) return "Fail 3: $a"

    val bytype = Z::class.java.getAnnotationsByType(A::class.java)
    if (a.toList() != bytype.toList()) return "Fail 4: ${a.toList()} != ${bytype.toList()}"

    return a.fold("") { acc, it -> acc + it.value }
}

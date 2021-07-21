// FILE: api.kt

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class Marker

@Marker
fun f() {}

// FILE: usage.kt

fun use1() {
    <!EXPERIMENTAL_API_USAGE_ERROR!>f<!>()
}

@Marker
fun use2() {
    f()
}

@OptIn(Marker::class)
fun use3() {
    f()
}

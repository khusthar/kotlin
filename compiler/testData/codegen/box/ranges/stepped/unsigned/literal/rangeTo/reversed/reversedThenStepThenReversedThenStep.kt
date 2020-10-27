// Auto-generated by GenerateSteppedRangesCodegenTestData. Do not edit!
// DONT_TARGET_EXACT_BACKEND: WASM
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val uintList = mutableListOf<UInt>()
    for (i in ((1u..10u).reversed() step 2).reversed() step 3) {
        uintList += i
    }
    assertEquals(listOf(2u, 5u, 8u), uintList)

    val ulongList = mutableListOf<ULong>()
    for (i in ((1uL..10uL).reversed() step 2L).reversed() step 3L) {
        ulongList += i
    }
    assertEquals(listOf(2uL, 5uL, 8uL), ulongList)

    return "OK"
}
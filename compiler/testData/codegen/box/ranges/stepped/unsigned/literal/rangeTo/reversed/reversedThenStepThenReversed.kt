// Auto-generated by GenerateSteppedRangesCodegenTestData. Do not edit!
// DONT_TARGET_EXACT_BACKEND: WASM
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val uintList = mutableListOf<UInt>()
    for (i in ((1u..8u).reversed() step 2).reversed()) {
        uintList += i
    }
    assertEquals(listOf(2u, 4u, 6u, 8u), uintList)

    val ulongList = mutableListOf<ULong>()
    for (i in ((1uL..8uL).reversed() step 2L).reversed()) {
        ulongList += i
    }
    assertEquals(listOf(2uL, 4uL, 6uL, 8uL), ulongList)

    return "OK"
}
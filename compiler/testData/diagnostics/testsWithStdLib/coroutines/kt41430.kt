// ISSUE: KT-41430, KT-47830

class A

fun test_1(list: List<Set<A>>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableSet<A>")!>list.flatMapTo(mutableSetOf()) { it }<!>
}

fun test_2(list: List<Set<A>>) {
    sequence<A> {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableSet<A>")!>list.flatMapTo(mutableSetOf()) { it }<!>
    }
}

fun test_3(list: List<Set<A>>) {
    sequence {
        <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>list.flatMapTo(mutableSetOf()) { it }<!>
        yield(A())
    }
}

fun test_4(list: List<Set<A>>) {
    sequence {
        yield(A())
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableSet<A>"), TYPE_MISMATCH, TYPE_MISMATCH!>list.flatMapTo(mutableSetOf()) { it }<!>
    }
}

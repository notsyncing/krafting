package io.github.notsyncing.krafting.core

val kraftingGlobalInjector = Injector()

inline fun <reified T> di(vararg provides: Any): T? {
    return kraftingGlobalInjector.get(T::class.java, provides = provides as Array<Any>)
}

inline fun <reified T> dip(vararg provides: Any): T? {
    return di(*provides)
}

inline fun <reified T> dis(vararg provides: Any): T {
    val o = kraftingGlobalInjector.get(T::class.java, provides = provides as Array<Any>)

    if (o == null) {
        throw ClassNotFoundException("${T::class.java} cannot be injected, maybe not found?")
    }

    return o
}

inline fun <reified T> dips(vararg provides: Any): T {
    return dis(*provides)
}

inline fun <reified T: I, reified I> diReg() {
    kraftingGlobalInjector.register(T::class.java, I::class.java)
}

inline fun <reified T> diUnreg() {
    kraftingGlobalInjector.unregister(T::class.java)
}

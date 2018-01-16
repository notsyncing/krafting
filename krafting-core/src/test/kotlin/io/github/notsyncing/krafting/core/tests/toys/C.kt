package io.github.notsyncing.krafting.core.tests.toys

import javax.inject.Provider

abstract class C {

}

class C1 : C() {

}

class CProvider : Provider<C> {
    override fun get(): C {
        return C1()
    }
}

class C2(val c: CProvider) {

}


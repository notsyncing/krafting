package io.github.notsyncing.krafting.core.tests.toys

import javax.inject.Inject

class A {
}

class A2(val a: A) {

}

class A3 {
    var a: A? = null

    constructor() {

    }

    @Inject
    constructor(a: A) {
        this.a = a
    }
}

class A4 {
    var called = false

    @Inject
    fun foo() {
        called = true
    }
}

class A5 {
    @Inject
    var a: A? = null
}
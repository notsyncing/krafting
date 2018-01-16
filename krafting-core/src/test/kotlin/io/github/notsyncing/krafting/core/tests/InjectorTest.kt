package io.github.notsyncing.krafting.core.tests

import io.github.notsyncing.krafting.core.Injector
import io.github.notsyncing.krafting.core.tests.toys.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InjectorTest {
    private lateinit var injector: Injector

    @BeforeEach
    fun setUp() {
        injector = Injector()
    }

    @Test
    fun testCraftDefaultConstructor() {
        val a = injector.get<A>()
        assertNotNull(a)
    }

    @Test
    fun testCraftConstructorWithDependencies() {
        val a2 = injector.get<A2>()
        assertNotNull(a2)
        assertNotNull(a2!!.a)
    }

    @Test
    fun testCraftConstructorWithInjectAnnotation() {
        val a3 = injector.get<A3>()
        assertNotNull(a3)
        assertNotNull(a3!!.a)
    }

    @Test
    fun testPublicMethodInjection() {
        val a4 = injector.get<A4>()
        assertTrue(a4!!.called)
    }

    @Test
    fun testPublicFieldInjection() {
        val a5 = injector.get<A5>()
        assertNotNull(a5!!.a)
    }

    @Test
    fun testCraftByInterface() {
        injector.register<IB, B>()
        val b = injector.get<IB>()
        assertNotNull(b)
        assertTrue(b is IB)
        assertTrue(b is B)
    }

    @Test
    fun testCraftByInterfaceInConstructor() {
        injector.register<IB, B>()
        val b2 = injector.get<B2>()
        assertNotNull(b2)
        assertTrue(b2!!.b is B)
    }

    @Test
    fun testCraftByProvider() {
        val c2 = injector.get<C2>()
        assertNotNull(c2)
        assertTrue(c2!!.c.get() is C1)
    }

    @Test
    fun testSingleton() {
        val e1 = injector.get<E>()
        val e2 = injector.get<E>()
        assertEquals(e1, e2)
    }

    @Test
    fun testCircularDetection() {
        assertThrows(ClassCircularityError::class.java) { injector.get<D1>() }
    }
}
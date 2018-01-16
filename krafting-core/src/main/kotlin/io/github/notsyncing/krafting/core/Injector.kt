package io.github.notsyncing.krafting.core

import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

class Injector {
    private val singletons = ConcurrentHashMap<Class<*>, Any>()
    private val mappings = ConcurrentHashMap<Class<*>, Class<*>>()

    private fun constructObject(clazz: Class<*>, provides: Array<Any>,
                                propagateProvides: Boolean, rootClass: Class<*>,
                                pending: MutableSet<Class<*>>): Any? {
        val constructors = clazz.declaredConstructors
        val targetConstructor: Constructor<*>

        val injectableConstructors = constructors.filter { it.isAnnotationPresent(Inject::class.java) }

        if (injectableConstructors.size > 1) {
            throw InstantiationException("$clazz has more than one constructor annotated with ${Inject::class.java}, " +
                    "you can have at most 1 constructor annotated with it!")
        } else if (injectableConstructors.isEmpty()) {
            targetConstructor = constructors[0] as Constructor<*>
        } else {
            targetConstructor = injectableConstructors[0] as Constructor<*>
        }

        if (targetConstructor.parameterCount <= 0) {
            return targetConstructor.newInstance()
        }

        val injectedConstructor = injectExecutable(targetConstructor, receiver = null, provides = provides,
                propagateProvides = propagateProvides, rootClass = rootClass, pending = pending)

        return injectedConstructor.invoke()
    }

    private fun injectFields(o: Any, provides: Array<Any> = emptyArray(), propagateProvides: Boolean = false,
                             rootClass: Class<*>, pending: MutableSet<Class<*>>) {
        for (field in o.javaClass.fields) {
            if (!field.isAnnotationPresent(Inject::class.java)) {
                continue
            }

            val v = if (propagateProvides) {
                _get(field.type, provides = provides, propagateProvides = propagateProvides, rootClass = rootClass,
                        pending = pending)
            } else {
                _get(field.type, propagateProvides = propagateProvides, rootClass = rootClass, pending = pending)
            }

            field.set(o, v)
        }

        for (field in o.javaClass.declaredFields) {
            if (!field.isAnnotationPresent(Inject::class.java)) {
                continue
            }

            if (Modifier.isPublic(field.modifiers)) {
                continue
            }

            val oldAccessible = field.isAccessible
            field.isAccessible = true

            val v = if (propagateProvides) {
                _get(field.type, provides = provides, propagateProvides = propagateProvides, rootClass = rootClass,
                        pending = pending)
            } else {
                _get(field.type, propagateProvides = propagateProvides, rootClass = rootClass, pending = pending)
            }

            field.set(o, v)
            field.isAccessible = oldAccessible
        }
    }

    private fun injectExecutable(executable: Executable, receiver: Any? = null, provides: Array<Any> = emptyArray(),
                                 propagateProvides: Boolean = false, rootClass: Class<*>,
                                 pending: MutableSet<Class<*>>): InjectedExecutable {
        val paramValues = mutableListOf<Any?>()

        p@ for (param in executable.parameters) {
            for (provided in provides) {
                if (param.type.isAssignableFrom(provided.javaClass)) {
                    paramValues.add(provided)
                    continue@p
                }
            }

            val v = if (propagateProvides) {
                _get(param.type, provides = provides, propagateProvides = propagateProvides, rootClass = rootClass,
                        pending = pending)
            } else {
                _get(param.type, propagateProvides = propagateProvides, rootClass = rootClass, pending = pending)
            }

            paramValues.add(v)
        }

        return InjectedExecutable(executable, receiver, paramValues.toTypedArray())
    }

    private fun injectMethods(o: Any, provides: Array<Any> = emptyArray(), propagateProvides: Boolean = false,
                              rootClass: Class<*>, pending: MutableSet<Class<*>>) {
        for (method in o.javaClass.methods) {
            if (!method.isAnnotationPresent(Inject::class.java)) {
                continue
            }

            val injectedMethod = if (propagateProvides) {
                injectExecutable(method, o, provides = provides, propagateProvides = propagateProvides,
                        rootClass = rootClass, pending = pending)
            } else {
                injectExecutable(method, o, propagateProvides = propagateProvides, rootClass = rootClass, pending = pending)
            }

            injectedMethod.invoke()
        }

        for (method in o.javaClass.declaredMethods) {
            if (!method.isAnnotationPresent(Inject::class.java)) {
                continue
            }

            if (Modifier.isPublic(method.modifiers)) {
                continue
            }

            val oldAccessible = method.isAccessible
            method.isAccessible = true

            val injectedMethod = if (propagateProvides) {
                injectExecutable(method, o, provides = provides, propagateProvides = propagateProvides,
                        rootClass = rootClass, pending = pending)
            } else {
                injectExecutable(method, o, propagateProvides = propagateProvides, rootClass = rootClass,
                        pending = pending)
            }

            injectedMethod.invoke()
            method.isAccessible = oldAccessible
        }
    }

    private fun _get(clazz: Class<*>, provides: Array<Any> = emptyArray(), propagateProvides: Boolean = false,
                     rootClass: Class<*>, pending: MutableSet<Class<*>>): Any? {
        val realClass = if (mappings.containsKey(clazz)) {
            mappings[clazz]!!
        } else {
            clazz
        }

        if (singletons.containsKey(realClass)) {
            return singletons[realClass]
        }

        if (pending.contains(realClass)) {
            throw ClassCircularityError("Circular dependency detected when crafting $rootClass: on $clazz " +
                    "($realClass)")
        }

        pending.add(realClass)

        val o = constructObject(realClass, provides, propagateProvides, rootClass, pending)

        if (o == null) {
            pending.remove(realClass)
            return null
        }

        if (o.javaClass.isAnnotationPresent(Singleton::class.java)) {
            singletons[realClass] = o
        }

        injectFields(o, provides = provides, propagateProvides = propagateProvides, rootClass = rootClass,
                pending = pending)

        injectMethods(o, provides = provides, propagateProvides = propagateProvides, rootClass = rootClass,
                pending = pending)

        pending.remove(realClass)

        return o
    }

    fun <T> get(clazz: Class<T>, provides: List<Any> = emptyList(), propagateProvides: Boolean = false): T? {
        return _get(clazz, provides.toTypedArray(), propagateProvides, clazz, mutableSetOf()) as T?
    }

    fun <T> get(clazz: Class<T>, provides: Array<Any> = emptyArray(), propagateProvides: Boolean = false): T? {
        return _get(clazz, provides, propagateProvides, clazz, mutableSetOf()) as T?
    }

    inline fun <reified T> get(): T? {
        return get(T::class.java, provides = emptyArray())
    }

    fun <W, I: W> register(wantClass: Class<W>, implementClass: Class<I>) {
        mappings[wantClass] = implementClass
    }

    inline fun <reified W, reified I: W> register() {
        register(W::class.java, I::class.java)
    }

    fun register(obj: Any) {
        singletons[obj.javaClass] = obj
    }

    fun <W> register(wantClass: Class<W>, obj: Any) {
        singletons[wantClass] = obj
        mappings[wantClass] = obj.javaClass
    }

    inline fun <reified W> registerSingletonAs(obj: Any) {
        register(W::class.java, obj)
    }

    fun unregister(wantClass: Class<*>) {
        mappings.remove(wantClass)
    }

    fun unregister(wantClass: Class<*>, implementClass: Class<*>) {
        mappings.remove(wantClass, implementClass)
    }

    fun unregister(obj: Any) {
        mappings.remove(obj.javaClass)
        singletons.remove(obj.javaClass)
    }

    fun unregister(wantClass: Class<*>, obj: Any) {
        mappings.remove(wantClass, obj.javaClass)
        singletons.remove(wantClass)
    }
}
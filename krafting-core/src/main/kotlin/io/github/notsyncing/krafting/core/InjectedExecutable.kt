package io.github.notsyncing.krafting.core

import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method

class InjectedExecutable(private val executable: Executable,
                         private val receiver: Any?,
                         private val params: Array<Any?>) {
    fun invoke(): Any? {
        return when (executable) {
            is Method ->
                executable.invoke(receiver, *params)

            is Constructor<*> ->
                executable.newInstance(*params)

            else ->
                throw UnsupportedOperationException("Unsupported executable $executable with receiver $receiver, " +
                        "parameters $params")
        }
    }
}
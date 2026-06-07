package com.qinhuai.corelib.reflection

import java.lang.reflect.Field
import java.lang.reflect.Method

object ReflectionBridge {
    
    fun isClassAvailable(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}

class ReflectionCache(private val className: String) {
    private var clazz: Class<*>? = null
    private val methods = mutableMapOf<String, Method?>()
    private val fields = mutableMapOf<String, Field?>()
    
    fun getClazz(): Class<*>? {
        if (clazz != null) return clazz
        return try {
            clazz = Class.forName(className)
            clazz
        } catch (e: ClassNotFoundException) {
            null
        }
    }
    
    fun getMethod(name: String, vararg paramTypes: Class<*>): Method? {
        val key = buildKey(name, paramTypes)
        return methods.getOrPut(key) {
            getClazz()?.getMethod(name, *paramTypes)?.apply { isAccessible = true }
        }
    }
    
    fun getDeclaredMethod(name: String, vararg paramTypes: Class<*>): Method? {
        val key = buildKey(name, paramTypes)
        return methods.getOrPut(key) {
            getClazz()?.getDeclaredMethod(name, *paramTypes)?.apply { isAccessible = true }
        }
    }
    
    fun findMethodByName(name: String): Method? {
        return methods.getOrPut("name_$name") {
            getClazz()?.declaredMethods?.firstOrNull { it.name == name }?.apply { isAccessible = true }
        }
    }
    
    fun getField(name: String): Field? {
        return fields.getOrPut(name) {
            getClazz()?.getField(name)?.apply { isAccessible = true }
        }
    }
    
    fun getDeclaredField(name: String): Field? {
        return fields.getOrPut(name) {
            getClazz()?.getDeclaredField(name)?.apply { isAccessible = true }
        }
    }
    
    fun findFieldInHierarchy(name: String): Field? {
        var current: Class<*>? = getClazz()
        while (current != null) {
            try {
                val field = current.getDeclaredField(name)
                field.isAccessible = true
                return field
            } catch (e: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }
    
    fun invoke(method: Method, instance: Any?, vararg args: Any?): Any? {
        return try {
            method.invoke(instance, *args)
        } catch (e: Exception) {
            null
        }
    }
    
    fun invokeStatic(method: Method, vararg args: Any?): Any? {
        return invoke(method, null, *args)
    }
    
    fun get(field: Field, instance: Any?): Any? {
        return try {
            field.get(instance)
        } catch (e: Exception) {
            null
        }
    }
    
    fun set(field: Field, instance: Any?, value: Any?) {
        try {
            field.set(instance, value)
        } catch (e: Exception) {
        }
    }
    
    fun <T> safeCall(block: () -> T): T? {
        return try {
            block()
        } catch (e: Throwable) {
            null
        }
    }
    
    private fun buildKey(name: String, paramTypes: Array<out Class<*>>): String {
        return "$name:${paramTypes.joinToString(",") { it.name }}"
    }
}

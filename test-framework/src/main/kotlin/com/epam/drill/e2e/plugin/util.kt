/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.e2e.plugin

import com.epam.drill.e2e.*
import com.epam.drill.plugin.api.processing.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.*
import org.apache.bcel.classfile.*
import org.apache.bcel.generic.*
import java.util.jar.*

@Suppress("UNCHECKED_CAST")
fun MemoryClassLoader.clazz(
    suffix: String,
    entries: Set<JarEntry>,
    jarFile: JarFile
): Class<AgentPart<*>> = entries.asSequence().filter {
    it.name.endsWith(".class") && !it.name.contains("module-info")
}.map { jarEntry ->
    jarFile.getInputStream(jarEntry).use { inStream ->
        ClassParser(inStream, "").parse()
    }
}.run {
    first { it.superclassName == AgentPart::class.qualifiedName }.also { pluginClass ->
        val singletons = filter { c ->
            !c.isSynthetic && !c.isNested && c.packageName == pluginClass.packageName &&
                c.fields.any { it.isStatic && it.name == "INSTANCE" }
        }
        val toBeRenamed = listOf(pluginClass) + singletons
        val paths = toBeRenamed.map { it.className.replace('.', '/') }
        println("MemoryClassLoader, classes to be renamed: $paths")
        forEach { javaClass ->
            val classGen = lazy(LazyThreadSafetyMode.NONE) { ClassGen(javaClass) }
            toBeRenamed.firstOrNull { javaClass.className.startsWith(it.className) }?.let {
                classGen.value.className = javaClass.className.replace(it.className, "${it.className}$suffix")
            }
            javaClass.constantPool.constantPool.forEachIndexed { index, constant ->
                if (constant is ConstantUtf8 && paths.any { it in constant.bytes }) {
                    val modified = paths.fold(constant.bytes) { str, path ->
                        str.replace(path, "$path$suffix")
                    }
                    classGen.value.constantPool.setConstant(index, ConstantUtf8(modified))
                }
            }
            val defClass = classGen.takeIf(Lazy<*>::isInitialized)?.run {
                value.apply(ClassGen::update).javaClass
            } ?: javaClass
            addDefinition(defClass.className, defClass.bytes)
        }
    }.let { "${it.className}$suffix" }
}.let { loadClass(it) as Class<AgentPart<*>> }

class OutsSock(private val mainChannel: SendChannel<Frame>, private val withDebug: Boolean = false) :
    SendChannel<Frame> by mainChannel {
    override suspend fun send(element: Frame) {
        if (withDebug && element is Frame.Text) {
            println("AGENT OUT: ${element.readText()}")
        }
        mainChannel.send(element)
    }
}

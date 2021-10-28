/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.admin.endpoints

import com.epam.drill.admin.agent.*
import com.epam.drill.admin.plugins.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.end.*
import com.epam.kodux.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*
import java.io.*
import java.lang.reflect.*

class Agent(info: AgentInfo) {
    private val _info = atomic(info)

    private val _instanceMap = atomic(
        persistentHashMapOf<String, PersistentMap<String, AdminPluginPart<*>>>()
    )

    var info: AgentInfo
        get() = _info.value
        set(value) = _info.update { value }

    val plugins get() = _instanceMap.value.values

    fun update(
        updater: (AgentInfo) -> AgentInfo,
    ): AgentInfo = _info.updateAndGet(updater)

    operator fun get(
        buildVersion: String,
        pluginId: String
    ): AdminPluginPart<*>? = _instanceMap.value[    buildVersion]?.get(pluginId)

    fun get(
        buildVersion: String,
        pluginId: String,
        updater: Agent.() -> AdminPluginPart<*>,
    ): AdminPluginPart<*> = get(buildVersion, pluginId) ?: _instanceMap.updateAndGet { instances ->
        val current = instances[buildVersion] ?: persistentMapOf()
        instances.put(buildVersion, current.takeIf { pluginId in it } ?: current.put(pluginId, updater()))
    }.getValue(buildVersion).getValue(pluginId)

    fun close() {
        plugins.forEach { plugin ->
            runCatching { (plugin as? Closeable)?.close() }
        }
    }
}

internal fun Plugin.createInstance(
    buildVersion: String,
    agentInfo: AgentInfo,
    data: AdminData,
    sender: Sender,
    store: StoreClient,
): AdminPluginPart<*> {
    @Suppress("UNCHECKED_CAST")
    val constructor = pluginClass.constructors.run {
        first() as Constructor<out AdminPluginPart<*>>
    }
    val classToArg: (Class<*>) -> Any = {
        when (it) {
            String::class.java -> pluginBean.id
            CommonAgentInfo::class.java -> agentInfo.toCommonInfo(buildVersion)
            AdminData::class.java -> data
            Sender::class.java -> sender
            StoreClient::class.java -> store
            else -> error("${pluginClass.name}: unsupported constructor parameter type $it.")
        }
    }
    val args: Array<Any> = constructor.parameterTypes.map(classToArg).toTypedArray()
    return constructor.newInstance(*args)
}

internal suspend fun Agent.applyPackagesChanges(buildVersion: String) {
    for (pluginId in info.plugins) {
        this[buildVersion, pluginId]?.applyPackagesChanges()
    }
}

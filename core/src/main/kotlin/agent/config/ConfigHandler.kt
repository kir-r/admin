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
package com.epam.drill.admin.agent.config

import com.epam.drill.admin.agent.*
import com.epam.drill.admin.endpoints.*
import com.epam.drill.admin.endpoints.agent.*
import com.epam.drill.admin.store.*
import com.epam.drill.api.*
import com.epam.drill.api.dto.*
import com.epam.drill.common.*
import org.kodein.di.*

class ConfigHandler(override val di: DI) : DIAware {
    private val buildManager by instance<BuildManager>()

    suspend fun store(agentId: String, parameters: Map<String, AgentParameter>) {
        adminStore.store(StoredAgentConfig(agentId, parameters))
    }

    suspend fun load(agentId: String) = adminStore.findById<StoredAgentConfig>(agentId)?.params

    suspend fun updateAgent(agentId: String, parameters: Map<String, String>) {
        buildManager.agentSessions(agentId).applyEach {
            updateParameters(parameters)
        }
    }
}

suspend fun AgentWsSession.updateParameters(params: Map<String, String>) =
    sendToTopic<Communication.Agent.UpdateParametersEvent, UpdateInfo>(UpdateInfo(params)).await()

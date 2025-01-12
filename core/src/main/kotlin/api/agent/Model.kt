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
package com.epam.drill.admin.api.agent

import com.epam.drill.admin.api.plugin.*
import kotlinx.serialization.*

enum class AgentType(val notation: String) {
    JAVA("Java"),
    DOTNET(".NET"),
    NODEJS("Node.js")
}

enum class AgentStatus {
    NOT_REGISTERED,
    PREREGISTERED,
    REGISTERING,
    REGISTERED,
}

enum class BuildStatus {
    ONLINE,
    OFFLINE,
    BUSY,
}

@Serializable
data class SystemSettingsDto(
    val packages: List<String> = emptyList(),
    val sessionIdHeaderName: String = "",
    val targetHost: String = "",
)

@Serializable
data class AgentInfoDto(
    val id: String,
    val group: String,
    val name: String,
    val description: String = "",
    val environment: String = "",
    val agentStatus: AgentStatus = AgentStatus.NOT_REGISTERED,
    val adminUrl: String = "",
    val activePluginsCount: Int = 0,
    val agentType: String,
    val plugins: Set<PluginDto> = emptySet(),
)

@Serializable
data class AgentBuildInfoDto(
    val buildVersion: String,
    val buildStatus: BuildStatus,
    val ipAddress: String = "",
    val agentVersion: String,
    val systemSettings: SystemSettingsDto = SystemSettingsDto(),
    val instanceIds: Set<String> = emptySet(),
)

//TODO remove after EPMDJ-8323
@Serializable
data class AgentBuildDto(
    val agentId: String,
    val build: AgentBuildInfoDto,
)

@Serializable
data class AgentCreationDto(
    val id: String,
    val agentType: AgentType,
    val name: String,
    val group: String = "",
    val environment: String = "",
    val description: String = "",
    val systemSettings: SystemSettingsDto = SystemSettingsDto(),
    val plugins: Set<String> = emptySet(),
)

@Serializable
data class AgentRegistrationDto(
    val name: String,
    val description: String = "",
    val environment: String = "",
    val systemSettings: SystemSettingsDto = SystemSettingsDto(),
    val plugins: List<String> = emptyList(),
)

@Serializable
data class AgentUpdateDto(
    val name: String,
    val description: String = "",
    val environment: String = "",
)

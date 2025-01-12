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
package com.epam.drill.admin.agent

import com.epam.drill.admin.api.agent.*
import com.epam.drill.admin.endpoints.*
import com.epam.dsm.*
import com.epam.dsm.serializer.*
import kotlinx.serialization.*

typealias CommonAgentConfig = com.epam.drill.common.AgentConfig
typealias CommonAgentInfo = com.epam.drill.common.AgentInfo
typealias PackagesPrefixes = com.epam.drill.common.PackagesPrefixes
typealias ByteClass = com.epam.drill.common.ByteClass
typealias BuildInfo = com.epam.drill.common.BuildInfo

@Serializable
data class AgentInfo(
    @Id val id: String,
    val name: String,
    val groupId: String = "",
    val agentStatus: AgentStatus,
    val environment: String = "",
    val description: String,
    val agentType: AgentType,
    val adminUrl: String = "",
    val build: AgentBuildInfo,
    val plugins: Set<String> = emptySet(),
) {
    override fun equals(other: Any?): Boolean = other is AgentInfo && id == other.id

    override fun hashCode(): Int = id.hashCode()
}

@Serializable
data class AgentBuildInfo(
    val version: String,
    val agentVersion: String = "",
    val ipAddress: String = "",
)

@Serializable
data class AgentBuildKey(
    val agentId: String,
    val buildVersion: String,
)

@Serializable
internal class PreparedAgentData(
    @Id val id: String,
    val dto: AgentCreationDto,
)

@Serializable
internal data class AgentDataSummary(
    @Id val agentId: String,
    val settings: SystemSettingsDto,
)

@Serializable
internal class StoredCodeData(
    @Id val id: AgentBuildKey,
    val data: List<ByteArray>,
)

@Serializable
internal data class Metadata(
    val countClass: Int = 0,
    val classBytesSizeKb: Int = 0,
) {
    companion object {
        val emptyMetadata = Metadata()
    }
}

@Serializable
internal class AgentMetadata(
    @Id val id: AgentBuildKey,
    val data: Metadata = Metadata.emptyMetadata,
)

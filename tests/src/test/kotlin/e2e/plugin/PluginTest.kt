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
package com.epam.drill.admin.e2e.plugin

import com.epam.drill.admin.api.agent.*
import com.epam.drill.admin.api.group.*
import com.epam.drill.admin.common.serialization.*
import com.epam.drill.admin.endpoints.*
import com.epam.drill.builds.*
import com.epam.drill.e2e.*
import io.kotlintest.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import mu.*
import kotlin.test.*

class PluginTest : E2EPluginTest() {
    private val logger = KotlinLogging.logger {}

    @Test
    fun `reconnect - same build`() {
        createSimpleAppWithPlugin<PTestStream> {
            connectAgent<Build1> { _, build ->
                logger.info { build }
                pluginAction("myActionForAllAgents") { status, content ->
                    logger.info { content }
                    status shouldBe HttpStatusCode.OK
                }.join()
            }.reconnect<Build1> { plugUi, build ->
                logger.info { "Reconnected agentId=${plugUi.info.agentId}, buildVersion=${build.version}" }
            }
        }
    }

    @Test
    fun `reconnect - new build`() {
        createSimpleAppWithPlugin<PTestStream> {
            connectAgent<Build1> { plugUi, _ ->
                plugUi.processedData.receive()
                pluginAction("myActionForAllAgents") { status, content ->
                    logger.info { content }
                    status shouldBe HttpStatusCode.OK
                }.join()
            }.reconnect<Build2> { plugUi, build ->
                plugUi.processedData.receive()
                logger.info { "Reconnected agentId=${plugUi.info.agentId}, buildVersion=${build.version}" }
            }
        }
    }

    @Test
    fun `test e2e plugin API for group`() {
        val group = "myGroup"
        logger.info { "starting tests..." }
        createSimpleAppWithPlugin<PTestStream> {
            connectAgent<Build1>(group) { _, _ ->
                logger.info { "hi ag1" }
            }
            connectAgent<Build1>(group) { _, _ ->
                logger.info { "hi ag2" }
            }
            connectAgent<Build1>(group) { _, _ ->
                logger.info { "hi ag3" }
            }
            logger.info { "finish connected..." }
            uiWatcher { channel ->
                logger.info { "waiting..." }
                waitForMultipleAgents(channel)
                logger.info { "after waiting..." }
                val statusResponse = StatusMessageResponse(
                    code = 200,
                    message = "act"
                )
                val statusResponses: List<WithStatusCode> =
                    listOf(statusResponse, statusResponse, statusResponse)
                delay(50)
                pluginAction("myActionForAllAgents", group) { status, content ->
                    logger.info { "2" }
                    status shouldBe HttpStatusCode.OK
                    content shouldBe (ListSerializer(WithStatusCode.serializer()) stringify statusResponses)
                }
                logger.info { "3" }
            }
        }

    }

    private suspend fun waitForMultipleAgents(channel: Channel<GroupedAgentsDto>, instanceCount: Int = 3) {
        while (true) {
            val message = channel.receive()
            val groupedAgents = message.grouped.flatMap { it.agents }
            if (groupedAgents.all { it.activePluginsCount == 1 && it.agentStatus == AgentStatus.REGISTERED } && groupedAgents.size == instanceCount) {
                break
            }
        }
    }
}

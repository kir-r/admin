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
package com.epam.drill.admin.e2e

import com.epam.drill.admin.api.agent.*
import com.epam.drill.e2e.*
import io.kotlintest.*
import io.ktor.http.*
import java.util.concurrent.*
import kotlin.test.*
import kotlin.time.*


class AgentRegistrationTest : E2ETest() {

    private val agentId = "registerAgent"

    @Test
    fun `agent should be registered`() {
        createSimpleAppWithUIConnection(uiStreamDebug = true, agentStreamDebug = true, timeout = 120.0.toDuration(TimeUnit.SECONDS) ) {
            connectAgent(AgentWrap(agentId, "0.1.0")) { ui, agent ->
                ui.getAgent()?.agentStatus shouldBe AgentStatus.NOT_REGISTERED
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
                register(agentId) { status, _ ->
                    status shouldBe HttpStatusCode.OK
                }
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERING
                ui.getBuild()?.buildStatus shouldBe BuildStatus.BUSY
                agent.`get-set-packages-prefixes`()
                agent.`get-load-classes-datas`()
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERED
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
            }
        }
    }

    @Test
    fun `agent must register without a description`() {
        createSimpleAppWithUIConnection {
            connectAgent(AgentWrap(agentId, "0.1.0")) { ui, agent ->
                ui.getAgent()?.agentStatus shouldBe AgentStatus.NOT_REGISTERED
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
                register(
                    agentId,
                    payload = AgentRegistrationDto(
                        name = "without description",
                        systemSettings = SystemSettingsDto(
                            packages = listOf("testPrefix")
                        ),
                        plugins = emptyList()
                    )
                ) { status, _ ->
                    status shouldBe HttpStatusCode.OK
                }
                ui.getBuild()?.buildStatus shouldBe BuildStatus.BUSY
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERING
                agent.`get-set-packages-prefixes`()
                agent.`get-load-classes-datas`()
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERED
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
            }
        }
    }

}

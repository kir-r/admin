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
import kotlin.time.*
import io.ktor.http.*
import kotlin.test.*
import kotlin.time.seconds as secs

class BuildsTest : E2ETest() {

    private val agentId = "buildRenamingAgent"

    @Test
    fun `can add new builds`() {
        createSimpleAppWithUIConnection(
            agentStreamDebug = false,
            uiStreamDebug = false,
            timeout = Duration.seconds(15)
        ) {
            val aw = AgentWrap(agentId)
            connectAgent(aw) { ui, agent ->
                ui.getAgent()?.agentStatus shouldBe AgentStatus.NOT_REGISTERED
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
                register(aw.id) { status, _ ->
                    status shouldBe HttpStatusCode.OK
                }
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERING
                agent.`get-set-packages-prefixes`()
                agent.`get-load-classes-datas`("DrillExtension1.class")
                ui.getBuild()?.buildStatus shouldBe BuildStatus.BUSY
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERED
                ui.getBuildSummary()?.size shouldBe 1
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE

            }.reconnect(aw.copy(buildVersion = "0.1.2")) { ui, agent ->
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERED
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
                agent.`get-set-packages-prefixes`()
                agent.`get-load-classes-datas`("DrillExtension2.class")
                ui.getBuild()?.buildStatus shouldBe BuildStatus.BUSY
                ui.getBuildSummary()?.size shouldBe 2
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
            }.reconnect(aw.copy(buildVersion = "0.1.3")) { ui, agent ->
                ui.getAgent()?.agentStatus shouldBe AgentStatus.REGISTERED
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
                agent.`get-set-packages-prefixes`()
                agent.`get-load-classes-datas`()
                ui.getBuild()?.buildStatus shouldBe BuildStatus.BUSY
                ui.getBuildSummary()?.size shouldBe 3
                ui.getBuild()?.buildStatus shouldBe BuildStatus.ONLINE
            }
        }

    }

}


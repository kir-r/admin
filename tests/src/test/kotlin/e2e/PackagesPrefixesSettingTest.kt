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
import org.junit.jupiter.api.*
import java.util.concurrent.*
import kotlin.time.*

class PackagesPrefixesSettingTest : E2ETest() {

    private val agentId = "ag02"

    @Test
    fun `Packages prefixes changing Test`() {
        createSimpleAppWithUIConnection {
            connectAgent(AgentWrap(agentId)) { ui, agent ->
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

                changePackages(
                    agentId = agentId,
                    payload = SystemSettingsDto(
                        listOf("newTestPrefix"),
                        ""
                    )
                ) { status, _ ->
                    status shouldBe HttpStatusCode.OK
                }

                ui.getBuild()?.buildStatus shouldBe BuildStatus.BUSY
                agent.`get-set-packages-prefixes`()
                agent.`get-load-classes-datas`()

                ui.getBuild()?.run {
                    buildStatus shouldBe BuildStatus.ONLINE
                    systemSettings.packages.first() shouldBe "newTestPrefix"
                }
            }
        }
    }
}

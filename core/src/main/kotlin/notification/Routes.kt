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
package com.epam.drill.admin.notification

import de.nielsfalk.ktor.swagger.*
import de.nielsfalk.ktor.swagger.version.shared.*
import io.ktor.locations.*


@Group("Notification Endpoints")
@Location("/api/notifications")
@Ignore(properties = ["parent"])
object ApiNotifications {
    // TODO EPMDJ-8438 param doesn't display in swagger
    @Group("Notification Endpoints")
    @Location("/{id}")
    data class Notification(val id: String) {
        @Group("Notification Endpoints")
        @Location("/read")
        data class Read(val parent: Notification)
    }

    @Group("Notification Endpoints")
    @Location("/read")
    data class Read(val parent: ApiNotifications)
}

@Location("/notifications")
object WsNotifications

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
package com.epam.drill.admin.jwt.user.source

import com.epam.drill.admin.jwt.user.*
import io.ktor.auth.*

class UserSourceImpl : UserSource {

    val testUser = User(1, System.getenv("DRILL_USERNAME") ?: "guest", System.getenv("DRILL_PASSWORD") ?: "", "admin")

    override fun findUserById(id: Int): User? = users[id]

    override fun findUserByCredentials(
        credential: UserPasswordCredential,
    ): User? = users.values.find { it.name == credential.name && it.password == credential.password }

    private val users = listOf(testUser).associateBy(User::id)


}

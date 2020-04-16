package com.epam.drill.admin.notification

import com.epam.drill.admin.admindata.*
import com.epam.drill.admin.endpoints.*
import com.epam.drill.admin.endpoints.agent.*
import com.epam.drill.admin.plugin.*
import com.epam.drill.admin.plugins.*
import com.epam.drill.common.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import mu.*
import org.kodein.di.*
import org.kodein.di.generic.*
import java.util.*

internal val logger = KotlinLogging.logger {}

class NotificationManager(override val kodein: Kodein) : KodeinAware {
    private val topicResolver by instance<TopicResolver>()
    private val plugins by instance<Plugins>()
    private val agentManager by instance<AgentManager>()
    private val _notifications = atomic(Notifications())

    val notifications
        get() = _notifications.value

    fun save(notification: Notification) {
        val notificationId = notification.id
        logger.info {
            "New notification with $notificationId associated with agent ${notification.agentId}." +
                " Message: ${notification.message}"
        }
        _notifications.update { it + notification }
    }

    fun read(id: String): Boolean = id in _notifications.updateAndGet { notifications ->
        notifications[id]?.run { copy(read = true) }?.let(notifications::replace) ?: notifications
    }

    fun delete(id: String): Boolean = id in _notifications.updateAndGet { it.minus(id) }

    suspend fun handleNewBuildNotification(agentInfo: AgentInfo) {
        val buildManager = agentManager.adminData(agentInfo.id).buildManager
        val previousBuildVersion = buildManager[agentInfo.buildVersion]?.parentVersion
        if (!previousBuildVersion.isNullOrEmpty() && previousBuildVersion != agentInfo.buildVersion) {
            saveNewBuildNotification(agentInfo, buildManager, previousBuildVersion)
        }
    }

    private suspend fun saveNewBuildNotification(
        agentInfo: AgentInfo,
        buildManager: AgentBuildManager,
        previousBuildVersion: String
    ) {
        save(
            Notification(
                id = UUID.randomUUID().toString(),
                agentId = agentInfo.id,
                createdAt = System.currentTimeMillis(),
                type = NotificationType.BUILD,
                message = createNewBuildMessage(buildManager, previousBuildVersion, agentInfo)
            )
        )
        topicResolver.sendToAllSubscribed(WsNotifications)
    }

    private suspend fun createNewBuildMessage(
        buildManager: AgentBuildManager,
        previousBuildVersion: String,
        agentInfo: AgentInfo
    ): NewBuildArrivedMessage {
        val methodChanges = buildManager[agentInfo.buildVersion]?.methodChanges ?: MethodChanges()
        val buildDiff = BuildDiff(
            methodChanges.map[DiffType.MODIFIED_BODY]?.count() ?: 0,
            methodChanges.map[DiffType.MODIFIED_DESC]?.count() ?: 0,
            methodChanges.map[DiffType.MODIFIED_NAME]?.count() ?: 0,
            methodChanges.map[DiffType.NEW]?.count() ?: 0,
            methodChanges.map[DiffType.DELETED]?.count() ?: 0
        )

        return NewBuildArrivedMessage(
            agentInfo.buildVersion,
            previousBuildVersion,
            buildDiff,
            pluginsRecommendations(agentInfo)
        )
    }

    private suspend fun pluginsRecommendations(
        agentInfo: AgentInfo
    ): List<String> {
        return agentManager.full(agentInfo.id)?.let { agentEntry ->
            val connectedPlugins = plugins.filter {
                agentInfo.plugins.map { pluginMetadata -> pluginMetadata.id }.contains(it.key)
            }

            //TODO rewrite this double parsing
            val results = connectedPlugins.map { (_, plugin: Plugin) ->
                val pluginInstance = agentManager.ensurePluginInstance(agentEntry, plugin)
                pluginInstance.getPluginData(type = "recommendations") as? String
            }.filterNotNull()
            results.mapNotNull { result ->
                try {
                    String.serializer().list parse result
                } catch (exception: JsonDecodingException) {
                    logger.error(exception) { "Parsing result '$result' finished with exception: " }
                    null
                }
            }.flatten()
        } ?: emptyList()
    }
}

class Notifications(
    private val asc: PersistentMap<String, Notification> = persistentMapOf(),
    private val desc: PersistentMap<String, Notification> = persistentMapOf()
) {

    val valuesDesc get() = desc.values

    operator fun get(id: String) = asc[id]

    operator fun contains(id: String): Boolean = id in asc

    fun replace(notification: Notification): Notifications = if (notification.id in asc) {
        Notifications(
            asc = asc.put(notification.id, notification),
            desc = desc.put(notification.id, notification)
        )
    } else this

    operator fun plus(notification: Notification): Notifications = if (notification.id !in asc) {
        Notifications(
            asc = asc.put(notification.id, notification),
            desc = persistentMapOf(notification.id to notification) + desc
        )
    } else this

    operator fun minus(id: String): Notifications = if (id in asc) {
        Notifications(
            asc = asc - id,
            desc = desc - id
        )
    } else this
}

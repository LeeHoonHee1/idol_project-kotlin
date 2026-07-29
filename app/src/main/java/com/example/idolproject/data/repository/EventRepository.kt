package com.example.idolproject.data.repository

import com.example.idolproject.Drawer.Event.EventItem
import com.example.idolproject.Drawer.Event.EventStatus
import com.example.idolproject.data.local.dao.EventDao
import com.example.idolproject.data.mapper.toEventEntity
import com.example.idolproject.data.mapper.toEventItem
import com.example.idolproject.data.remote.api.EventApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val eventApiService: EventApiService
) {

    fun observeEvents(): Flow<List<EventItem>> {
        return eventDao.observeEvents()
            .map { entities ->
                entities.map { entity ->
                    entity.toEventItem()
                }
            }
    }

    suspend fun refreshEvents(): EventRefreshResult {
        return runCatching {
            val eventEntities = eventApiService.getEvents().map { dto ->
                dto.toEventEntity()
            }

            eventDao.upsertEvents(eventEntities)

            EventRefreshResult.Success
        }.getOrElse {
            val fallbackEvents = sampleEvents().map { event ->
                event.toEventEntity()
            }

            eventDao.upsertEvents(fallbackEvents)

            EventRefreshResult.Fallback
        }
    }

    private fun sampleEvents(): List<EventItem> {
        return listOf(
            EventItem(
                id = "attendance_2026_06",
                title = "6월 출석 이벤트",
                description = "7일 연속 출석하면 보너스 EXP를 받을 수 있어요.",
                startDate = "2026.06.01",
                endDate = "2026.06.30",
                reward = "EXP +50",
                status = EventStatus.ACTIVE
            ),
            EventItem(
                id = "favorite_group_support",
                title = "최애 그룹 응원 이벤트",
                description = "이번 주 가장 많은 응원을 받은 그룹이 랭킹 배너에 노출돼요.",
                startDate = "2026.06.10",
                endDate = "2026.06.17",
                reward = "응원 뱃지",
                status = EventStatus.ACTIVE
            ),
            EventItem(
                id = "fanart_event_2026_07",
                title = "팬아트 게시글 이벤트",
                description = "커뮤니티에 팬아트 게시글을 올리고 이벤트에 참여해보세요.",
                startDate = "2026.07.01",
                endDate = "2026.07.15",
                reward = "팬아트 칭호",
                status = EventStatus.UPCOMING
            )
        )
    }
}

sealed interface EventRefreshResult {
    data object Success : EventRefreshResult
    data object Fallback : EventRefreshResult
}
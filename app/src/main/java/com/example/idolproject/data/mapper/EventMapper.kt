package com.example.idolproject.data.mapper

import com.example.idolproject.Drawer.Event.EventItem
import com.example.idolproject.Drawer.Event.EventStatus
import com.example.idolproject.data.local.entity.EventEntity
import com.example.idolproject.data.remote.dto.EventDto

fun EventEntity.toEventItem(): EventItem {
    return EventItem(
        id = id,
        title = title,
        description = description,
        startDate = startDate,
        endDate = endDate,
        reward = reward,
        status = runCatching {
            EventStatus.valueOf(status)
        }.getOrDefault(EventStatus.FINISHED)
    )
}

fun EventItem.toEventEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        description = description,
        startDate = startDate,
        endDate = endDate,
        reward = reward,
        status = status.name
    )
}

fun EventDto.toEventEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        description = description,
        startDate = startDate,
        endDate = endDate,
        reward = reward,
        status = status
    )
}
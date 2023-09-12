package ru.practicum.participationRequest.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateRequest {

    Set<Integer> requestIds;

    String status;  //[ CONFIRMED, REJECTED ]
}

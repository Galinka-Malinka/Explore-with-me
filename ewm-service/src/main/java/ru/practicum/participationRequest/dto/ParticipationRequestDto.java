package ru.practicum.participationRequest.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequestDto {

    Integer id;

    @NotNull
    Integer requester;

    @NotNull
    Integer event;

    String created;

    String status;
}


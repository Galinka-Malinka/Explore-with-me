package ru.practicum.compilation.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.model.EventForCompilation;
import ru.practicum.compilation.model.EventForCompilationPK;
import ru.practicum.event.dto.EventShortDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompilationMapper {

    public static Compilation toCompilation(NewCompilationDto newCompilationDto) {
        boolean pinned = newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false;

        return Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(pinned)
                .build();
    }

    public static List<EventForCompilation> toEventForCompilationList(Integer compId, NewCompilationDto newCompilationDto) {
        List<EventForCompilation> result = new ArrayList<>();

        for (Integer eventId : newCompilationDto.getEvents()) {
            result.add(EventForCompilation.builder()
                    .eventForCompilationPK(
                            EventForCompilationPK.builder()
                                    .compilationId(compId)
                                    .eventId(eventId)
                                    .build()
                    ).build());
        }
        return result;
    }

    public static CompilationDto toCompilationDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(new HashSet<>())
                .build();
    }

    public static CompilationDto toCompilationDto(Compilation compilation, Set<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(events)
                .build();
    }
}

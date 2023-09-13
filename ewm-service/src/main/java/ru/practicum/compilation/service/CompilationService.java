package ru.practicum.compilation.service;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto create(NewCompilationDto newCompilationDto);

    CompilationDto update(Integer compId, UpdateCompilationRequest updateCompilationRequest);

    void delete(Integer compId);

    CompilationDto getById(Integer compId);

    List<CompilationDto> get(Boolean pinned, Integer from, Integer size);
}

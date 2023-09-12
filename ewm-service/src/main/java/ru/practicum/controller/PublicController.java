package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PublicController {

    private final EventService eventService;

    private final CategoryService categoryService;

    @GetMapping("/events")
    public List<EventShortDto> getEvents(@RequestParam(value = "text", required = false) String text,
                                         @RequestParam(value = "categories", required = false,
                                                 defaultValue = "0") Integer[] categories,
                                         @RequestParam(value = "paid", required = false) Boolean paid,
                                         @RequestParam(value = "rangeStart", required = false) String rangeStart,
                                         @RequestParam(value = "rangeEnd", required = false) String rangeEnd,
                                         @RequestParam(value = "onlyAvailable", required = false,
                                                 defaultValue = "false") Boolean onlyAvailable,
                                         @RequestParam(value = "sort", required = false,
                                                 defaultValue = "EVENT_DATE") String sort,
                                         @RequestParam(value = "from", required = false,
                                                 defaultValue = "0") Integer from,
                                         @RequestParam(value = "size", required = false,
                                                 defaultValue = "10") Integer size) {

        return eventService.getPublicly(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/events/{eventId}")
    public EventFullDto geyEventById(@PathVariable Integer eventId) {
        return eventService.getPubliclyById(eventId);
    }

    @GetMapping("/categories")
    public List<CategoryDto> getCategories(@RequestParam(value = "from", required = false, defaultValue = "0")
                                           Integer from,
                                           @RequestParam(value = "size", required = false, defaultValue = "10")
                                           Integer size) {
        return categoryService.get(from, size);
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto getCategoryById(@PathVariable Integer catId) {
        return categoryService.getById(catId);
    }
}

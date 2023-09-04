package ru.practicum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.controller.StatsController;
import ru.practicum.service.StatsService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatsController.class)
public class StatsServiceControllerTest {

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    StatsService statsService;

    @Autowired
    private MockMvc mvc;

    private final EndpointHit endpointHit = EndpointHit.builder()
            .id(1)
            .app("ewm-main-service")
            .uri("/events/1")
            .ip("192.163.0.1")
            .timestamp("2022-09-06 11:00:23")
            .build();

    private final ViewStats viewStats1 = ViewStats.builder()
            .app("ewm-main-service")
            .uri("/events/1")
            .hits(6)
            .build();

    private final ViewStats viewStats2 = ViewStats.builder()
            .app("ewm-main-service")
            .uri("/events")
            .hits(10)
            .build();


    @Test
    void shouldCreateEndpointHit() throws Exception {
        when(statsService.create(any(EndpointHit.class))).thenReturn(endpointHit);

        mvc.perform(post("/hit").content(objectMapper.writeValueAsString(endpointHit))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(endpointHit.getId()), Integer.class))
                .andExpect(jsonPath("$.app", is(endpointHit.getApp()), String.class))
                .andExpect(jsonPath("$.uri", is(endpointHit.getUri()), String.class))
                .andExpect(jsonPath("$.ip", is(endpointHit.getIp()), String.class))
                .andExpect(jsonPath("$.timestamp", is(endpointHit.getTimestamp()), String.class));
    }

    @Test
    void shouldGetStats() throws Exception {
        List<ViewStats> viewStatsList = new ArrayList<>();
        viewStatsList.add(viewStats1);
        viewStatsList.add(viewStats2);

        when(statsService.get(anyString(), anyString(), any(), anyBoolean())).thenReturn(viewStatsList);

        mvc.perform(get("/stats")
                        .param("start", "2022-09-06 11:00:23")
                        .param("end", "2023-08-06 11:00:23")
                        .param("uris", "/events/1, /events")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].app", is(viewStats1.getApp()), String.class))
                .andExpect(jsonPath("$[0].uri", is(viewStats1.getUri()), String.class))
                .andExpect(jsonPath("$[0].hits", is(viewStats1.getHits()), Integer.class))
                .andExpect(jsonPath("$[1].app", is(viewStats2.getApp()), String.class))
                .andExpect(jsonPath("$[1].uri", is(viewStats2.getUri()), String.class))
                .andExpect(jsonPath("$[1].hits", is(viewStats2.getHits()), Integer.class));
    }
}

package ru.practicum.compilation;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class EventForCompilationPK implements Serializable {
    private Integer compilation_id;

    private Integer event_id;
}

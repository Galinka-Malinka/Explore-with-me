package ru.practicum.compilation.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "events_for_compilation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventForCompilation {

    @EmbeddedId
    private EventForCompilationPK eventForCompilationPK;

}

package ru.practicum.compilation.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Event;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "compilations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "title", nullable = false, length = 50)
    String title;

    @Column(name = "pinned", nullable = false)
    boolean pinned;

//     @ManyToMany
//        @JoinTable(name = "compilations_for_events",
//                joinColumns = @JoinColumn(name = "compilation_id", referencedColumnName = "id"),
//                inverseJoinColumns = @JoinColumn(name = "event_id", referencedColumnName = "id"))
//        private Collection<Event> events;
}

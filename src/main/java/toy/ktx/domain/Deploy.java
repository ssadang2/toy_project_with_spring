package toy.ktx.domain;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "deploy")
public class Deploy {

    @Id @GeneratedValue
    private Long id;

    @Column(name = "departure_time")
    @NotBlank
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    @NotBlank
    private LocalDateTime arrivalTime;

    @Column(name = "departure_place")
    @NotBlank
    private String departurePlace;

    @Column(name = "arrival_place")
    @NotBlank
    private String arrivalPlace;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "deploy")
    private Reservation reservation;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "deploy")
    private List<Train> trains;
}

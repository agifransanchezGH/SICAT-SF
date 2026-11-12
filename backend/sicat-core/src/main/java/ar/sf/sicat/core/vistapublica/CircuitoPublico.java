package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_circuito_publico}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_circuito_publico")
@Getter
@NoArgsConstructor
public class CircuitoPublico {

    @Id
    @Column(name = "id_oferta")
    private Long idOferta;

    @Column(name = "duracion_min")
    private Integer duracionMin;

    @Column(name = "lugar_partida")
    private String lugarPartida;

    @Column(name = "lat_partida")
    private Double latPartida;

    @Column(name = "lng_partida")
    private Double lngPartida;

    /** Trazado como GeoJSON (LineString). */
    @Column(name = "trazado_geojson")
    private String trazadoGeojson;
}

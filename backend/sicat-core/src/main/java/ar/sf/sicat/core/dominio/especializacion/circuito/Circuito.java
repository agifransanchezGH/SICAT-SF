package ar.sf.sicat.core.dominio.especializacion.circuito;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.LineString;

import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Extensión 1 a 1 de {@link Oferta} para circuitos turísticos. */
@Entity
@Table(schema = "sicat", name = "circuito")
@Getter
@Setter
@NoArgsConstructor
public class Circuito {

    @Id
    @Column(name = "id_oferta")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferta")
    private Oferta oferta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lugar_partida")
    private Lugar lugarPartida;

    @Column(name = "duracion_min")
    private Integer duracionMin;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(columnDefinition = "geography(LineString,4326)")
    private LineString trazado;
}

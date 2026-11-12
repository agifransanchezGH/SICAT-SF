package ar.sf.sicat.core.dominio.lugar;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ubicación geográfica. Jerárquica: un lugar puede estar dentro de otro ({@code lugarPadre}). */
@Entity
@Table(schema = "sicat", name = "lugar")
@Getter
@Setter
@NoArgsConstructor
public class Lugar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lugar")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lugar_padre")
    private Lugar lugarPadre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_lugar")
    private TipoLugar tipoLugar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_localidad")
    private Localidad localidad;

    @Column(nullable = false)
    private String nombre;

    private String calle;

    private Integer altura;

    @Column(precision = 6, scale = 1)
    private BigDecimal km;

    @Column(name = "calle_cruce")
    private String calleCruce;

    @Column(length = 100)
    private String complemento;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(columnDefinition = "geography(Point,4326)")
    private Point punto;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(columnDefinition = "geography(Geometry,4326)")
    private Geometry geometria;

    @Column(name = "estado_geo")
    private String estadoGeo;
}

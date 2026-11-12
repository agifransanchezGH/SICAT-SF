package ar.sf.sicat.core.seguridad;

import java.time.OffsetDateTime;

import ar.sf.sicat.core.dominio.oferente.Oferente;
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

@Entity
@Table(schema = "seguridad", name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    /** Etapa 2: usuario con rol OFERENTE asociado a su oferente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_oferente")
    private Oferente oferente;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hash_password", nullable = false)
    private String hashPassword;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "ultimo_acceso", columnDefinition = "timestamptz")
    private OffsetDateTime ultimoAcceso;
}

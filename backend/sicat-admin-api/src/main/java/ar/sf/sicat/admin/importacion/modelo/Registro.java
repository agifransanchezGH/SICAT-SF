package ar.sf.sicat.admin.importacion.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un establecimiento interpretado desde una o más filas de una planilla, listo para validar y guardar.
 * Es mutable a propósito: los intérpretes lo van completando.
 */
public class Registro {

    public record Contacto(String tipo, String valor, String uso) {
    }

    public record CaracteristicaValor(String grupo, String nombre, Integer cantidad) {
    }

    public record Referente(String nombre, String telefono, String correo) {
    }

    public record Alojamiento(Short estrellas, Integer habitaciones, Integer plazas, Integer habAccesibles,
                              Integer plazasAccesibles, Integer personalPermanente, Integer personalNoPermanente) {
    }

    public record Salon(String nombre, Integer auditorio, Integer banquete, Integer coctel, java.math.BigDecimal m2) {
    }

    public final String hoja;
    public final int fila;
    public String nombre;
    public String direccion;
    public String categoria;
    public String subcategoria;
    public String descripcion;
    public String estadoFuncionamiento;
    public String acceso;
    public String visitaGuiada;
    public Short anioInauguracion;
    public LocalDate fechaRelevamiento;
    public Alojamiento alojamiento;
    public final List<Contacto> contactos = new ArrayList<>();
    public final List<CaracteristicaValor> caracteristicas = new ArrayList<>();
    public final List<Referente> referentes = new ArrayList<>();
    public final List<Salon> salones = new ArrayList<>();
    public final List<String> errores = new ArrayList<>();
    public final List<String> advertencias = new ArrayList<>();

    public Registro(String hoja, int fila) {
        this.hoja = hoja;
        this.fila = fila;
    }

    public void agregarDescripcion(String etiqueta, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        String parte = etiqueta == null ? texto.trim() : etiqueta + ": " + texto.trim();
        descripcion = descripcion == null ? parte : descripcion + "\n\n" + parte;
    }

    public void agregarCaracteristica(String grupo, String nombre, Integer cantidad) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        String n = nombre.length() > 200 ? nombre.substring(0, 199) + "…" : nombre;
        boolean repetida = caracteristicas.stream().anyMatch(c -> c.grupo().equalsIgnoreCase(grupo)
                && ar.sf.sicat.admin.importacion.lectura.Texto.clave(c.nombre())
                        .equals(ar.sf.sicat.admin.importacion.lectura.Texto.clave(n)));
        if (!repetida) {
            caracteristicas.add(new CaracteristicaValor(grupo, n, cantidad));
        }
    }
}

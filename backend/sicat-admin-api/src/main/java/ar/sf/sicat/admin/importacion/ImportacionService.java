package ar.sf.sicat.admin.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ar.sf.sicat.admin.importacion.dto.ResultadoImportacion;
import ar.sf.sicat.admin.importacion.dto.ResultadoImportacion.Estado;
import ar.sf.sicat.admin.importacion.lectura.LectorExcel;
import ar.sf.sicat.admin.importacion.lectura.LectorExcel.Hoja;
import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.admin.importacion.modelo.Perfil;
import ar.sf.sicat.admin.importacion.modelo.Registro;
import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.clasificacion.CategoriaRepository;
import ar.sf.sicat.core.dominio.contacto.CanalContacto;
import ar.sf.sicat.core.dominio.contacto.CanalContactoRepository;
import ar.sf.sicat.core.dominio.contacto.TipoCanal;
import ar.sf.sicat.core.dominio.contacto.TipoCanalRepository;
import ar.sf.sicat.core.dominio.especializacion.alojamiento.Alojamiento;
import ar.sf.sicat.core.dominio.especializacion.alojamiento.AlojamientoRepository;
import ar.sf.sicat.core.dominio.especializacion.salon.Salon;
import ar.sf.sicat.core.dominio.especializacion.salon.SalonRepository;
import ar.sf.sicat.core.dominio.especializacion.visitaguiada.VisitaGuiada;
import ar.sf.sicat.core.dominio.especializacion.visitaguiada.VisitaGuiadaRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristica;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristicaRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.establecimiento.Referente;
import ar.sf.sicat.core.dominio.establecimiento.ReferenteRepository;
import ar.sf.sicat.core.dominio.lugar.Localidad;
import ar.sf.sicat.core.dominio.lugar.LocalidadRepository;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import ar.sf.sicat.core.dominio.oferente.Oferente;
import ar.sf.sicat.core.dominio.oferente.OferenteRepository;
import ar.sf.sicat.core.dominio.oferente.TipoOferente;
import ar.sf.sicat.core.dominio.oferente.TipoOferenteRepository;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;

/**
 * Importación desde las planillas Excel de la Dirección de Turismo (ADR 0010).
 * {@link #validar} simula sin guardar; {@link #ejecutar} guarda los registros nuevos y sin errores,
 * todo en una transacción. Lo creado queda en BORRADOR, con un oferente propio y la dirección pendiente
 * de geocodificar. La auditoría lo registra con origen MIGRACION.
 */
@Service
public class ImportacionService {

    public static final String LOCALIDAD_POR_DEFECTO = "Santa Fe";
    public static final String PROVINCIA_POR_DEFECTO = "Santa Fe";
    public static final String TIPO_OFERENTE_IMPORTADO = "Sin clasificar";
    public static final String GEO_PENDIENTE = "PENDIENTE";
    private static final Pattern VISITA_CON_RESERVA = Pattern.compile(
            "(?i).*(turno|reserva|solicit|acuerdo|consult|previa|coordinar).*", Pattern.DOTALL);

    private final EstablecimientoRepository establecimientos;
    private final OferenteRepository oferentes;
    private final TipoOferenteRepository tiposOferente;
    private final LugarRepository lugares;
    private final LocalidadRepository localidades;
    private final CategoriaRepository categorias;
    private final CaracteristicaRepository caracteristicas;
    private final EstablecimientoCaracteristicaRepository asignaciones;
    private final OfertaRepository ofertas;
    private final TipoCanalRepository tiposCanal;
    private final CanalContactoRepository canales;
    private final ReferenteRepository referentes;
    private final AlojamientoRepository alojamientos;
    private final SalonRepository salones;
    private final VisitaGuiadaRepository visitas;

    public ImportacionService(EstablecimientoRepository establecimientos, OferenteRepository oferentes,
                              TipoOferenteRepository tiposOferente, LugarRepository lugares,
                              LocalidadRepository localidades, CategoriaRepository categorias,
                              CaracteristicaRepository caracteristicas,
                              EstablecimientoCaracteristicaRepository asignaciones, OfertaRepository ofertas,
                              TipoCanalRepository tiposCanal, CanalContactoRepository canales,
                              ReferenteRepository referentes, AlojamientoRepository alojamientos,
                              SalonRepository salones, VisitaGuiadaRepository visitas) {
        this.establecimientos = establecimientos;
        this.oferentes = oferentes;
        this.tiposOferente = tiposOferente;
        this.lugares = lugares;
        this.localidades = localidades;
        this.categorias = categorias;
        this.caracteristicas = caracteristicas;
        this.asignaciones = asignaciones;
        this.ofertas = ofertas;
        this.tiposCanal = tiposCanal;
        this.canales = canales;
        this.referentes = referentes;
        this.alojamientos = alojamientos;
        this.salones = salones;
        this.visitas = visitas;
    }

    @Transactional(readOnly = true)
    public ResultadoImportacion validar(Perfil perfil, MultipartFile archivo) {
        return procesar(perfil, archivo, false);
    }

    @Transactional
    public ResultadoImportacion ejecutar(Perfil perfil, MultipartFile archivo) {
        return procesar(perfil, archivo, true);
    }

    private ResultadoImportacion procesar(Perfil perfil, MultipartFile archivo, boolean guardar) {
        List<Hoja> hojas = leer(archivo);
        List<String> ignoradas = new ArrayList<>();
        List<Registro> registros = new ArrayList<>();
        for (Hoja hoja : hojas) {
            Optional<Perfil.HojaConfig> config = perfil.configDe(hoja.nombre());
            if (config.isEmpty() || hoja.filas().isEmpty()) {
                ignoradas.add(hoja.nombre());
                continue;
            }
            registros.addAll(Interprete.interpretar(hoja, config.get()));
        }
        if (registros.isEmpty()) {
            throw new DatoInvalidoException("El archivo no tiene filas para el perfil '" + perfil.ruta()
                    + "' (se espera " + perfil.archivoEsperado() + ")");
        }

        Duplicados indice = new Duplicados(establecimientos.findAll());
        Set<String> enArchivo = new HashSet<>();
        Escritura escritura = guardar ? new Escritura() : null;
        List<ResultadoImportacion.Item> items = new ArrayList<>();
        int nuevos = 0;
        int dupBase = 0;
        int dupArchivo = 0;
        int errores = 0;
        int creados = 0;
        for (Registro r : registros) {
            Estado estado;
            Long existente = null;
            Long creado = null;
            String clave = Texto.clave(r.nombre) + "|" + Texto.clave(r.direccion);
            if (!r.errores.isEmpty()) {
                estado = Estado.ERROR;
                errores++;
            } else if (!enArchivo.add(clave)) {
                estado = Estado.DUPLICADO_EN_ARCHIVO;
                dupArchivo++;
            } else {
                Optional<Long> dup = indice.buscar(r.nombre, r.direccion);
                if (dup.isPresent()) {
                    estado = Estado.DUPLICADO_EN_BASE;
                    existente = dup.get() > 0 ? dup.get() : null;
                    dupBase++;
                    // el mismo establecimiento en otra planilla: se le suma la categoría en vez de perderla
                    if (existente != null) {
                        String categoria = r.subcategoria != null ? r.subcategoria : r.categoria;
                        if (escritura != null) {
                            if (escritura.agregarCategoria(existente, r.categoria, r.subcategoria)) {
                                r.advertencias.add("Ya existía: se le agregó la categoría '" + categoria + "'");
                            }
                        } else {
                            r.advertencias.add("Ya existe: al ejecutar se le agrega la categoría '" + categoria
                                    + "' si no la tiene");
                        }
                    }
                } else {
                    estado = Estado.NUEVO;
                    nuevos++;
                    if (escritura != null) {
                        creado = escritura.guardar(r);
                        creados++;
                    }
                }
            }
            items.add(new ResultadoImportacion.Item(r.hoja, r.fila, r.nombre,
                    r.subcategoria != null ? r.categoria + " > " + r.subcategoria : r.categoria, r.direccion, estado,
                    existente, creado, r.contactos.size(), r.caracteristicas.size(),
                    r.errores.isEmpty() ? null : r.errores, r.advertencias.isEmpty() ? null : r.advertencias));
        }
        if (escritura != null) {
            establecimientos.flush();
        }
        return new ResultadoImportacion(perfil.ruta(), guardar, registros.size(), nuevos, dupBase, dupArchivo, errores,
                creados, ignoradas, items);
    }

    private static List<Hoja> leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DatoInvalidoException("Falta el archivo .xlsx");
        }
        try (InputStream in = archivo.getInputStream()) {
            return LectorExcel.leer(in);
        } catch (IOException e) {
            throw new DatoInvalidoException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    /** Guarda registros reutilizando catálogos ya existentes (y creándolos si faltan). Vive una sola importación. */
    private final class Escritura {

        private final Map<String, Categoria> categoriasPorClave = new HashMap<>();
        private final Map<String, Caracteristica> caracteristicasPorClave = new HashMap<>();
        private final Map<String, TipoCanal> canalesPorClave = new HashMap<>();
        private final TipoOferente tipoOferente;
        private final Localidad localidad;

        Escritura() {
            for (Categoria c : categorias.findAll()) {
                categoriasPorClave.putIfAbsent(claveCategoria(c.getCategoriaPadre(), c.getNombre()), c);
            }
            for (Caracteristica c : caracteristicas.findAll()) {
                caracteristicasPorClave.putIfAbsent(Texto.clave(c.getGrupo()) + "|" + Texto.clave(c.getNombre()), c);
            }
            for (TipoCanal t : tiposCanal.findAll()) {
                canalesPorClave.putIfAbsent(Texto.clave(t.getNombre()), t);
            }
            tipoOferente = tiposOferente.findByNombreIgnoreCase(TIPO_OFERENTE_IMPORTADO).orElseGet(() -> {
                TipoOferente t = new TipoOferente();
                t.setNombre(TIPO_OFERENTE_IMPORTADO);
                t.setActivo(true);
                return tiposOferente.save(t);
            });
            localidad = localidades.findByNombreIgnoreCaseAndProvinciaIgnoreCase(LOCALIDAD_POR_DEFECTO, PROVINCIA_POR_DEFECTO)
                    .orElseGet(() -> {
                        Localidad l = new Localidad();
                        l.setNombre(LOCALIDAD_POR_DEFECTO);
                        l.setProvincia(PROVINCIA_POR_DEFECTO);
                        l.setActivo(true);
                        return localidades.save(l);
                    });
        }

        Long guardar(Registro r) {
            Oferente oferente = new Oferente();
            oferente.setNombre(Texto.recortar(r.nombre, 300));
            oferente.setTipoOferente(tipoOferente);
            oferente.setActivo(true);
            oferentes.save(oferente);

            Lugar lugar = null;
            if (r.direccion != null) {
                lugar = new Lugar();
                lugar.setNombre(Texto.recortar(r.direccion.replace('\n', ' '), 300));
                String[] partes = Texto.calleYAltura(r.direccion).orElse(null);
                if (partes != null) {
                    lugar.setCalle(Texto.recortar(partes[0], 200));
                    lugar.setAltura(Integer.valueOf(partes[1]));
                }
                lugar.setLocalidad(localidad);
                lugar.setEstadoGeo(GEO_PENDIENTE);
                lugares.save(lugar);
            }

            Establecimiento e = new Establecimiento();
            e.setOferente(oferente);
            e.setLugar(lugar);
            e.setNombre(Texto.recortar(r.nombre, 300));
            e.setDescripcion(Texto.recortar(r.descripcion, 5000));
            e.setAnioInauguracion(r.anioInauguracion);
            e.setEstadoFuncionamiento(Texto.recortar(r.estadoFuncionamiento, 100));
            e.setEstadoPublicacion(EstadoPublicacion.BORRADOR);
            e.setDestacado(false);
            e.setActivo(true);
            establecimientos.save(e);

            Oferta oferta = new Oferta();
            oferta.setEstablecimiento(e);
            oferta.setCategoria(categoria(r.categoria, r.subcategoria));
            oferta.setAcceso(Texto.recortar(r.acceso, 1000));
            oferta.setFechaRelevamiento(r.fechaRelevamiento);
            oferta.setActivo(true);
            ofertas.save(oferta);

            if (r.alojamiento != null) {
                Alojamiento a = new Alojamiento();
                a.setOferta(oferta);
                Short estrellas = r.alojamiento.estrellas();
                a.setEstrellas(estrellas != null && estrellas >= 1 && estrellas <= 5 ? estrellas : null);
                a.setCantHabitaciones(r.alojamiento.habitaciones());
                a.setTotalPlazas(r.alojamiento.plazas());
                a.setHabAccesibles(r.alojamiento.habAccesibles());
                a.setPlazasAccesibles(r.alojamiento.plazasAccesibles());
                a.setPersonalPermanente(r.alojamiento.personalPermanente());
                a.setPersonalNoPermanente(r.alojamiento.personalNoPermanente());
                alojamientos.save(a);
            }
            for (Registro.Salon s : r.salones) {
                Salon salon = new Salon();
                salon.setOferta(oferta);
                salon.setNombre(Texto.recortar(s.nombre(), 200));
                salon.setCapAuditorio(s.auditorio());
                salon.setCapBanquete(s.banquete());
                salon.setCapCoctel(s.coctel());
                salon.setM2(s.m2());
                salon.setActivo(true);
                salones.save(salon);
            }
            if (r.visitaGuiada != null) {
                VisitaGuiada v = new VisitaGuiada();
                v.setOferta(oferta);
                v.setModalidad(r.visitaGuiada);
                v.setRequiereReserva(VISITA_CON_RESERVA.matcher(r.visitaGuiada).matches());
                v.setHabilitada(true);
                visitas.save(v);
            }

            short orden = 0;
            for (Registro.Contacto k : r.contactos) {
                CanalContacto c = new CanalContacto();
                c.setEstablecimiento(e);
                c.setTipoCanal(tipoCanal(k.tipo()));
                c.setValor(k.valor());
                c.setUso(k.uso());
                c.setEsPublico(true);
                c.setOrden(++orden);
                canales.save(c);
            }
            Set<Long> asignadas = new HashSet<>();
            for (Registro.CaracteristicaValor cv : r.caracteristicas) {
                Caracteristica c = caracteristica(cv.grupo(), cv.nombre());
                if (asignadas.add(c.getId())) {
                    EstablecimientoCaracteristica a = new EstablecimientoCaracteristica();
                    a.setEstablecimiento(e);
                    a.setCaracteristica(c);
                    a.setCantidad(Boolean.TRUE.equals(c.getAdmiteCantidad()) ? cv.cantidad() : null);
                    asignaciones.save(a);
                }
            }
            for (Registro.Referente ref : r.referentes) {
                Referente referente = new Referente();
                referente.setEstablecimiento(e);
                referente.setNombre(Texto.recortar(ref.nombre(), 200));
                referente.setTelefono(Texto.recortar(ref.telefono(), 100));
                referente.setCorreo(Texto.recortar(ref.correo(), 200));
                referente.setActivo(true);
                referentes.save(referente);
            }
            return e.getId();
        }

        /**
         * Agrega la categoría como adicional a la primera oferta activa del establecimiento si ninguna de sus
         * ofertas la tiene. Devuelve true si la agregó.
         */
        boolean agregarCategoria(Long idEstablecimiento, String raiz, String sub) {
            Categoria categoria = categoria(raiz, sub);
            List<Oferta> existentes = ofertas.findByEstablecimientoIdOrderById(idEstablecimiento);
            boolean yaLaTiene = existentes.stream().anyMatch(o -> o.getCategoria().getId().equals(categoria.getId())
                    || o.getCategoriasAdicionales().stream().anyMatch(c -> c.getId().equals(categoria.getId())));
            Optional<Oferta> destino = existentes.stream().filter(o -> Boolean.TRUE.equals(o.getActivo())).findFirst();
            if (yaLaTiene || destino.isEmpty()) {
                return false;
            }
            destino.get().getCategoriasAdicionales().add(categoria);
            return true;
        }

        private Categoria categoria(String raiz, String sub) {
            Categoria padre = buscarOCrearCategoria(null, raiz);
            return sub == null ? padre : buscarOCrearCategoria(padre, sub);
        }

        private Categoria buscarOCrearCategoria(Categoria padre, String nombre) {
            return categoriasPorClave.computeIfAbsent(claveCategoria(padre, nombre), k -> {
                Categoria c = new Categoria();
                c.setNombre(Texto.recortar(nombre, 200));
                c.setCategoriaPadre(padre);
                c.setActivo(true);
                return categorias.save(c);
            });
        }

        private Caracteristica caracteristica(String grupo, String nombre) {
            return caracteristicasPorClave.computeIfAbsent(Texto.clave(grupo) + "|" + Texto.clave(nombre), k -> {
                Caracteristica c = new Caracteristica();
                c.setGrupo(grupo);
                c.setNombre(nombre);
                c.setAdmiteCantidad(false);
                c.setActivo(true);
                return caracteristicas.save(c);
            });
        }

        private TipoCanal tipoCanal(String nombre) {
            return canalesPorClave.computeIfAbsent(Texto.clave(nombre), k -> {
                TipoCanal t = new TipoCanal();
                t.setNombre(nombre);
                t.setActivo(true);
                return tiposCanal.save(t);
            });
        }

        private static String claveCategoria(Categoria padre, String nombre) {
            return (padre == null ? "" : padre.getId() + "/") + Texto.clave(nombre);
        }
    }
}

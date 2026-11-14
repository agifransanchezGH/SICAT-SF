package ar.sf.sicat.admin.importacion.lectura;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import ar.sf.sicat.core.comun.DatoInvalidoException;

/**
 * Lee un .xlsx: por hoja, la primera fila no vacía es el encabezado y el resto son filas de datos.
 * Las claves de cada fila son los encabezados normalizados ({@link Texto#encabezado}).
 */
public final class LectorExcel {

    public record Fila(int numero, Map<String, String> valores) {

        /** Valor de la primera columna cuyo encabezado normalizado empieza con alguno de los prefijos. */
        public String valor(String... prefijos) {
            for (String p : prefijos) {
                String clave = Texto.encabezado(p);
                for (Map.Entry<String, String> e : valores.entrySet()) {
                    if (e.getKey().equals(clave) || e.getKey().startsWith(clave)) {
                        String v = Texto.limpio(e.getValue());
                        if (v != null) {
                            return v;
                        }
                    }
                }
            }
            return null;
        }
    }

    public record Hoja(String nombre, List<String> encabezados, List<Fila> filas) {
    }

    private LectorExcel() {
    }

    public static List<Hoja> leer(InputStream entrada) {
        try (Workbook libro = WorkbookFactory.create(entrada)) {
            List<Hoja> hojas = new ArrayList<>();
            for (Sheet hoja : libro) {
                hojas.add(leerHoja(hoja));
            }
            return hojas;
        } catch (IOException | RuntimeException e) {
            throw new DatoInvalidoException("No se pudo leer el archivo como planilla .xlsx: " + e.getMessage());
        }
    }

    private static Hoja leerHoja(Sheet hoja) {
        List<String> encabezados = new ArrayList<>();
        List<Fila> filas = new ArrayList<>();
        int columnas = 0;
        for (Row fila : hoja) {
            if (encabezados.isEmpty()) {
                if (estaVacia(fila)) {
                    continue;
                }
                columnas = fila.getLastCellNum();
                for (int c = 0; c < columnas; c++) {
                    String crudo = valor(fila.getCell(c));
                    encabezados.add(crudo == null ? "" : crudo.trim());
                }
                continue;
            }
            if (estaVacia(fila)) {
                continue;
            }
            Map<String, String> valores = new LinkedHashMap<>();
            for (int c = 0; c < columnas; c++) {
                String clave = Texto.encabezado(encabezados.get(c));
                if (clave.isEmpty()) {
                    clave = "COLUMNA " + (c + 1);
                }
                while (valores.containsKey(clave)) {
                    clave = clave + " BIS";
                }
                valores.put(clave, valor(fila.getCell(c)));
            }
            filas.add(new Fila(fila.getRowNum() + 1, valores));
        }
        return new Hoja(hoja.getSheetName().trim(), encabezados, filas);
    }

    private static boolean estaVacia(Row fila) {
        for (Cell celda : fila) {
            if (Texto.limpio(valor(celda)) != null) {
                return false;
            }
        }
        return true;
    }

    /** Texto de la celda. Fechas en ISO (yyyy-MM-dd) y números sin notación científica ni ".0". */
    static String valor(Cell celda) {
        if (celda == null) {
            return null;
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
        return switch (tipo) {
            case STRING -> celda.getStringCellValue();
            case BOOLEAN -> celda.getBooleanCellValue() ? "Sí" : "No";
            case NUMERIC -> DateUtil.isCellDateFormatted(celda)
                    ? celda.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    : BigDecimal.valueOf(celda.getNumericCellValue()).stripTrailingZeros().toPlainString();
            default -> null;
        };
    }
}

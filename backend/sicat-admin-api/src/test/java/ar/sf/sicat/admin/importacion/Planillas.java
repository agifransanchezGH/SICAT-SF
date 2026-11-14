package ar.sf.sicat.admin.importacion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;

/** Arma planillas .xlsx de prueba en memoria. Los textos que parecen números se guardan como números. */
public final class Planillas {

    private final Map<String, String[][]> hojas = new LinkedHashMap<>();

    public static Planillas nueva() {
        return new Planillas();
    }

    public Planillas hoja(String nombre, String[]... filas) {
        hojas.put(nombre, filas);
        return this;
    }

    public MockMultipartFile archivo() throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Map.Entry<String, String[][]> h : hojas.entrySet()) {
                Sheet hoja = libro.createSheet(h.getKey());
                for (int i = 0; i < h.getValue().length; i++) {
                    Row fila = hoja.createRow(i);
                    String[] valores = h.getValue()[i];
                    for (int c = 0; c < valores.length; c++) {
                        String v = valores[c];
                        if (v == null) {
                            continue;
                        }
                        if (i > 0 && v.matches("\\d+(\\.\\d+)?")) {
                            fila.createCell(c).setCellValue(Double.parseDouble(v));
                        } else {
                            fila.createCell(c).setCellValue(v);
                        }
                    }
                }
            }
            libro.write(out);
            return new MockMultipartFile("archivo", "planilla.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    public static String[] f(String... valores) {
        return valores;
    }
}

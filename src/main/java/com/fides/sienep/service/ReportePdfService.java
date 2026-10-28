package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Genera el PDF del reporte como un documento formal sobre fondo blanco. De la identidad del frontend toma la
 * tipografía Onest (licencia OFL, incrustada desde resources; si los archivos faltaran cae a Helvetica) y la paleta
 * de grises, con el celeste de marca como único acento.
 */
@Service
@RequiredArgsConstructor
public class ReportePdfService {

    private static final DateTimeFormatter FORMATTER_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATTER_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Paleta (globals.css del frontend) ─────────────────────
    private static final BaseColor COLOR_ACENTO = new BaseColor(14, 165, 233);     // sky-500
    private static final BaseColor COLOR_TEXTO = new BaseColor(38, 38, 38);        // neutral-800
    private static final BaseColor COLOR_TEXTO_SUAVE = new BaseColor(113, 113, 122); // zinc-500
    private static final BaseColor COLOR_LINEA_FUERTE = new BaseColor(63, 63, 70); // zinc-700
    private static final BaseColor COLOR_BORDE = new BaseColor(214, 211, 209);     // stone-300
    private static final BaseColor COLOR_CABECERA = new BaseColor(245, 245, 244);  // stone-100

    // ── Fuentes ───────────────────────────────────────────────
    // Instancias estáticas (400 y 700) de la variable Onest-latin.woff2 del frontend, que iText no puede leer
    private static final BaseFont BASE_REGULAR = cargarFuente("/reportes/fuentes/Onest-Regular.ttf", BaseFont.HELVETICA);
    private static final BaseFont BASE_NEGRITA = cargarFuente("/reportes/fuentes/Onest-Bold.ttf", BaseFont.HELVETICA_BOLD);

    private static final Font FUENTE_TITULO = fuente(BASE_NEGRITA, 20, COLOR_TEXTO);
    private static final Font FUENTE_SUBTITULO = fuente(BASE_REGULAR, 10, COLOR_TEXTO_SUAVE);
    private static final Font FUENTE_SECCION = fuente(BASE_NEGRITA, 11, COLOR_TEXTO);
    private static final Font FUENTE_CODIGO = fuente(BASE_REGULAR, 9, COLOR_TEXTO_SUAVE);
    private static final Font FUENTE_NORMAL = fuente(BASE_REGULAR, 9, COLOR_TEXTO);
    private static final Font FUENTE_NEGRITA = fuente(BASE_NEGRITA, 9, COLOR_TEXTO);
    private static final Font FUENTE_PEQUEÑA = fuente(BASE_REGULAR, 7, COLOR_TEXTO_SUAVE);
    private static final Font FUENTE_CABECERA_TABLA = fuente(BASE_NEGRITA, 8, COLOR_TEXTO);
    private static final Font FUENTE_CELDA_TABLA = fuente(BASE_REGULAR, 8, COLOR_TEXTO);

    private static final float MARGEN_LATERAL = 50;

    private static Font fuente(BaseFont base, float tamanio, BaseColor color) {
        return new Font(base, tamanio, Font.NORMAL, color);
    }

    private static BaseFont cargarFuente(String recurso, String respaldo) {
        try (InputStream in = ReportePdfService.class.getResourceAsStream(recurso)) {
            if (in != null) {
                String nombre = recurso.substring(recurso.lastIndexOf('/') + 1);
                return BaseFont.createFont(nombre, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
            }
        } catch (DocumentException | IOException e) {
            // Se usa la fuente de respaldo más abajo
        }
        try {
            return BaseFont.createFont(respaldo, BaseFont.WINANSI, false);
        } catch (DocumentException | IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    public byte[] generarPdf(ReporteResponseDTO reporte) {
        try {
            String periodo = textoPeriodo(reporte);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document documento = new Document(PageSize.A4, MARGEN_LATERAL, MARGEN_LATERAL, 64, 60);
            PdfWriter writer = PdfWriter.getInstance(documento, baos);
            writer.setPageEvent(new MarcoDePagina(periodo));
            documento.open();

            agregarEncabezado(documento, periodo);
            agregarMetadata(documento, reporte);
            agregarResumen(documento, reporte);
            agregarIndicadores(documento, reporte);
            agregarPieDeDocumento(documento, reporte);

            documento.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el PDF del reporte", e);
        }
    }

    // El Data Warehouse agrega por mes, así que el período siempre son meses completos
    private String textoPeriodo(ReporteResponseDTO reporte) {
        if (reporte.getDesde() == null || reporte.getHasta() == null) {
            return "Período: sin datos";
        }
        return "Período: " + reporte.getDesde().format(FORMATTER_FECHA) + " al " +
                reporte.getHasta().format(FORMATTER_FECHA) + " (meses completos)";
    }

    // Valores que pueden venir nulos de las vistas: se muestra un guion en vez de la palabra null
    private String texto(Object valor) {
        return valor != null ? String.valueOf(valor) : "-";
    }

    private String porcentaje(BigDecimal valor) {
        return valor != null ? valor + " %" : "-";
    }

    // ── Secciones del documento ───────────────────────────────

    private void agregarEncabezado(Document doc, String periodo) throws Exception {
        Paragraph titulo = new Paragraph("Reporte de seguimiento", FUENTE_TITULO);
        titulo.setSpacingAfter(2);
        doc.add(titulo);

        Paragraph subtitulo = new Paragraph("Instancias e incidencias · " + periodo, FUENTE_SUBTITULO);
        subtitulo.setSpacingAfter(6);
        doc.add(subtitulo);

        // Único trazo celeste del documento
        doc.add(linea(1.5f, COLOR_ACENTO));
    }

    private void agregarMetadata(Document doc, ReporteResponseDTO reporte) throws Exception {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1, 3});
        tabla.setSpacingBefore(8);

        agregarFilaMetadata(tabla, "Generado por", reporte.getGeneradoPor());
        agregarFilaMetadata(tabla, "Fecha de generación", reporte.getFechaGeneracion().format(FORMATTER_DATETIME));
        agregarFilaMetadata(tabla, "Datos actualizados al", reporte.getDatosAl() != null
                ? reporte.getDatosAl().format(FORMATTER_DATETIME) : "sin carga registrada");

        doc.add(tabla);
    }

    private void agregarResumen(Document doc, ReporteResponseDTO reporte) throws Exception {
        agregarTituloSeccion(doc, null, "Resumen del período");

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1, 1});
        agregarCabecera(tabla, new String[]{"Concepto", "Total"});
        agregarFila(tabla, new String[]{"Instancias agendadas", String.valueOf(reporte.getTotalInstancias())});
        agregarFila(tabla, new String[]{"Incidencias ocurridas", String.valueOf(reporte.getTotalIncidencias())});
        doc.add(tabla);
    }

    private void agregarIndicadores(Document doc, ReporteResponseDTO reporte) throws Exception {
        agregarTablaIndicador(doc, "IND-01 e IND-02", "Instancias agendadas e incidencias ocurridas por mes",
                new String[]{"Mes", "Instancias agendadas", "Incidencias ocurridas"},
                new float[]{1.5f, 1.5f, 1.5f},
                reporte.getEvolucionMensual(),
                f -> new String[]{f.getAnioMes(), String.valueOf(f.getInstanciasAgendadas()), String.valueOf(f.getIncidenciasOcurridas())});

        agregarTablaIndicador(doc, "IND-03", "Instancias por categoría",
                new String[]{"Mes", "Categoría", "Instancias", "% del mes"},
                new float[]{1f, 2.5f, 1f, 1f},
                reporte.getInstanciasPorCategoria(),
                f -> new String[]{f.getAnioMes(), f.getCategoria(), String.valueOf(f.getInstancias()), porcentaje(f.getPorcentajeDelMes())});

        agregarTablaIndicador(doc, "IND-04", "Instancias por canal",
                new String[]{"Mes", "Canal", "Modalidad", "Instancias", "% del mes"},
                new float[]{1f, 1.8f, 1.5f, 1f, 1f},
                reporte.getInstanciasPorCanal(),
                f -> new String[]{f.getAnioMes(), f.getCanal(), f.getModalidad(), String.valueOf(f.getInstancias()), porcentaje(f.getPorcentajeDelMes())});

        agregarTablaIndicador(doc, "IND-05", "Incidencias por lugar",
                new String[]{"Mes", "Tipo de espacio", "Lugar", "Incidencias"},
                new float[]{1f, 1.8f, 2f, 1f},
                reporte.getIncidenciasPorLugar(),
                f -> new String[]{f.getAnioMes(), f.getTipoEspacio(), f.getLugar(), String.valueOf(f.getIncidencias())});

        agregarTablaIndicador(doc, "IND-06", "Incidencias por día de la semana y franja horaria",
                new String[]{"Mes", "Día", "Franja", "Incidencias"},
                new float[]{1f, 1.5f, 1.5f, 1f},
                reporte.getIncidenciasPorDiaFranja(),
                f -> new String[]{f.getAnioMes(), f.getDiaSemana(), f.getFranjaHoraria(), String.valueOf(f.getIncidencias())});

        agregarTablaIndicador(doc, "IND-07", "Involucrados por incidencia",
                new String[]{"Mes", "Incidencias", "Involucrados", "Con más de uno", "Promedio", "% con más de uno"},
                new float[]{1f, 1f, 1f, 1f, 1f, 1.2f},
                reporte.getIncidenciasInvolucrados(),
                f -> new String[]{f.getAnioMes(), String.valueOf(f.getIncidencias()), String.valueOf(f.getTotalInvolucrados()),
                        String.valueOf(f.getIncidenciasConMasDeUno()), texto(f.getPromedioInvolucrados()),
                        porcentaje(f.getPorcentajeConMasDeUno())});
    }

    private <T> void agregarTablaIndicador(Document doc, String codigo, String titulo, String[] cabeceras, float[] anchos,
                                           List<T> filas, Function<T, String[]> aCeldas) throws Exception {
        agregarTituloSeccion(doc, codigo, titulo);

        List<T> datos = filas != null ? filas : new ArrayList<>();
        if (datos.isEmpty()) {
            doc.add(new Paragraph("Sin datos en el período.", FUENTE_NORMAL));
            return;
        }

        PdfPTable tabla = new PdfPTable(cabeceras.length);
        tabla.setWidthPercentage(100);
        tabla.setWidths(anchos);
        // La cabecera se repite si la tabla continúa en otra página
        tabla.setHeaderRows(1);

        agregarCabecera(tabla, cabeceras);
        for (T fila : datos) {
            agregarFila(tabla, aCeldas.apply(fila));
        }

        doc.add(tabla);
    }

    private void agregarPieDeDocumento(Document doc, ReporteResponseDTO reporte) throws Exception {
        Paragraph pie = new Paragraph(
                "Documento generado por SIENEP el " +
                        reporte.getFechaGeneracion().format(FORMATTER_DATETIME) +
                        ". La información oficial y fidedigna debe consultarse directamente en el sistema.",
                FUENTE_PEQUEÑA);
        pie.setSpacingBefore(18);
        doc.add(pie);
    }

    // ── Helpers de construcción ───────────────────────────────

    private Paragraph linea(float grosor, BaseColor color) {
        Paragraph linea = new Paragraph(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(
                grosor, 100, color, Element.ALIGN_CENTER, -2)));
        linea.setSpacingBefore(2);
        return linea;
    }

    // Título en negrita con el código del indicador en gris y una línea fina debajo
    private void agregarTituloSeccion(Document doc, String codigo, String titulo) throws Exception {
        Phrase texto = new Phrase();
        if (codigo != null) {
            texto.add(new Chunk(codigo + "   ", FUENTE_CODIGO));
        }
        texto.add(new Chunk(titulo, FUENTE_SECCION));

        PdfPCell celda = new PdfPCell(texto);
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderWidthBottom(0.5f);
        celda.setBorderColorBottom(COLOR_BORDE);
        celda.setPaddingLeft(0);
        celda.setPaddingBottom(4);

        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(18);
        tabla.setSpacingAfter(6);
        tabla.addCell(celda);
        doc.add(tabla);
    }

    private void agregarFilaMetadata(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, FUENTE_NEGRITA));
        celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
        celdaEtiqueta.setPaddingLeft(0);
        celdaEtiqueta.setPaddingTop(2);
        celdaEtiqueta.setPaddingBottom(2);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "-", FUENTE_NORMAL));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        celdaValor.setPaddingTop(2);
        celdaValor.setPaddingBottom(2);
        tabla.addCell(celdaValor);
    }

    // Cabecera clara con una línea oscura arriba y abajo, como las tablas de un informe
    private void agregarCabecera(PdfPTable tabla, String[] cabeceras) {
        for (int i = 0; i < cabeceras.length; i++) {
            PdfPCell celda = new PdfPCell(new Phrase(cabeceras[i], FUENTE_CABECERA_TABLA));
            celda.setBackgroundColor(COLOR_CABECERA);
            celda.setHorizontalAlignment(Element.ALIGN_LEFT);
            celda.setPadding(5);
            celda.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
            celda.setBorderWidthTop(0.75f);
            celda.setBorderWidthBottom(0.75f);
            celda.setBorderColorTop(COLOR_LINEA_FUERTE);
            celda.setBorderColorBottom(COLOR_LINEA_FUERTE);
            tabla.addCell(celda);
        }
    }

    // Solo líneas horizontales finas entre filas
    private void agregarFila(PdfPTable tabla, String[] textos) {
        for (int i = 0; i < textos.length; i++) {
            PdfPCell celda = new PdfPCell(new Phrase(textos[i] != null ? textos[i] : "-", FUENTE_CELDA_TABLA));
            celda.setHorizontalAlignment(Element.ALIGN_LEFT);
            celda.setPadding(4);
            celda.setBorder(Rectangle.BOTTOM);
            celda.setBorderWidthBottom(0.5f);
            celda.setBorderColorBottom(COLOR_BORDE);
            tabla.addCell(celda);
        }
    }

    // ── Dibujo de bajo nivel ──────────────────────────────────

    // Encabezado corrido (desde la segunda página) y pie con numeración
    private static class MarcoDePagina extends PdfPageEventHelper {
        private static final float ANCHO_TOTAL_PAGINAS = 18;

        private final String periodo;
        private PdfTemplate totalPaginas;

        MarcoDePagina(String periodo) {
            this.periodo = periodo;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPaginas = writer.getDirectContent().createTemplate(ANCHO_TOTAL_PAGINAS, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float ancho = document.getPageSize().getWidth();
            float alto = document.getPageSize().getHeight();
            float izquierda = MARGEN_LATERAL;
            float derecha = ancho - MARGEN_LATERAL;

            // En la primera página el título del documento hace de encabezado
            if (writer.getPageNumber() > 1) {
                texto(cb, BASE_NEGRITA, 8, COLOR_TEXTO, "Reporte de seguimiento", izquierda, alto - 36, Element.ALIGN_LEFT);
                texto(cb, BASE_REGULAR, 8, COLOR_TEXTO_SUAVE, periodo, derecha, alto - 36, Element.ALIGN_RIGHT);
                linea(cb, izquierda, derecha, alto - 44);
            }

            linea(cb, izquierda, derecha, 46);
            texto(cb, BASE_REGULAR, 7, COLOR_TEXTO_SUAVE,
                    "SIENEP · Sistema Integral de Estudiantes con Necesidades Educativas Personalizadas",
                    izquierda, 34, Element.ALIGN_LEFT);
            texto(cb, BASE_REGULAR, 8, COLOR_TEXTO_SUAVE, "Página " + writer.getPageNumber() + " de ",
                    derecha - ANCHO_TOTAL_PAGINAS, 34, Element.ALIGN_RIGHT);
            cb.addTemplate(totalPaginas, derecha - ANCHO_TOTAL_PAGINAS, 34);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPaginas.beginText();
            totalPaginas.setFontAndSize(BASE_REGULAR, 8);
            totalPaginas.setColorFill(COLOR_TEXTO_SUAVE);
            totalPaginas.setTextMatrix(0, 0);
            // iText ya cerró la última página al llegar acá, por eso no se resta 1
            totalPaginas.showText(String.valueOf(writer.getPageNumber()));
            totalPaginas.endText();
        }

        private void linea(PdfContentByte cb, float desde, float hasta, float y) {
            cb.saveState();
            cb.setLineWidth(0.5f);
            cb.setColorStroke(COLOR_BORDE);
            cb.moveTo(desde, y);
            cb.lineTo(hasta, y);
            cb.stroke();
            cb.restoreState();
        }

        private void texto(PdfContentByte cb, BaseFont fuente, float tamanio, BaseColor color, String texto,
                           float x, float y, int alineacion) {
            cb.beginText();
            cb.setFontAndSize(fuente, tamanio);
            cb.setColorFill(color);
            cb.showTextAligned(alineacion, texto, x, y, 0);
            cb.endText();
        }
    }
}

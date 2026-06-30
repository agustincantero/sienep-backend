package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteEstudianteDTO;
import com.fides.sienep.dto.ReporteResponseDTO;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReportePdfService {

    private static final DateTimeFormatter FORMATTER_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATTER_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Fuentes ───────────────────────────────────────────────
    private static final Font FUENTE_TITULO = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
    private static final Font FUENTE_SUBTITULO = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 11, BaseColor.DARK_GRAY);
    private static final Font FUENTE_NORMAL = FontFactory.getFont(
            FontFactory.HELVETICA, 9, BaseColor.BLACK);
    private static final Font FUENTE_NEGRITA = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 9, BaseColor.BLACK);
    private static final Font FUENTE_PEQUEÑA = FontFactory.getFont(
            FontFactory.HELVETICA, 7, BaseColor.GRAY);
    private static final Font FUENTE_ADVERTENCIA = FontFactory.getFont(
            FontFactory.HELVETICA_OBLIQUE, 8, new BaseColor(180, 90, 0));
    private static final Font FUENTE_CABECERA_TABLA = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
    private static final Font FUENTE_CELDA_TABLA = FontFactory.getFont(
            FontFactory.HELVETICA, 8, BaseColor.BLACK);

    private static final BaseColor COLOR_CABECERA = new BaseColor(52, 73, 94);
    private static final BaseColor COLOR_FILA_PAR = new BaseColor(240, 243, 246);

    public byte[] generarPdf(ReporteResponseDTO reporte) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document documento = new Document(PageSize.A4, 40, 40, 60, 60);
            PdfWriter.getInstance(documento, baos);
            documento.open();

            agregarEncabezado(documento, reporte);
            agregarMetadata(documento, reporte);
            agregarMetricasGlobales(documento, reporte);
            agregarTablaDetalle(documento, reporte);
            agregarPieDeDocumento(documento, reporte);

            documento.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el PDF del reporte", e);
        }
    }

    // ── Secciones del documento ───────────────────────────────

    private void agregarEncabezado(Document doc, ReporteResponseDTO reporte) throws Exception {
        Paragraph titulo = new Paragraph("SIENEP — Reporte de Seguimiento Estudiantil", FUENTE_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(4);
        doc.add(titulo);

        String periodo = "Período: " +
                reporte.getDesde().format(FORMATTER_FECHA) + " al " +
                reporte.getHasta().format(FORMATTER_FECHA);
        Paragraph subtitulo = new Paragraph(periodo, FUENTE_SUBTITULO);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(8);
        doc.add(subtitulo);

        agregarLinea(doc);
    }

    private void agregarMetadata(Document doc, ReporteResponseDTO reporte) throws Exception {
        Paragraph seccion = new Paragraph("Información del documento", FUENTE_SUBTITULO);
        seccion.setSpacingBefore(10);
        seccion.setSpacingAfter(6);
        doc.add(seccion);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1, 2});

        agregarFilaMetadata(tabla, "Generado por:", reporte.getGeneradoPor());
        agregarFilaMetadata(tabla, "Fecha de generación:",
                reporte.getFechaGeneracion().format(FORMATTER_DATETIME));
        agregarFilaMetadata(tabla, "Filtros aplicados:", reporte.getFiltrosAplicados());
        agregarFilaMetadata(tabla, "Total de estudiantes:",
                String.valueOf(reporte.getTotalEstudiantes()));

        doc.add(tabla);

        if (reporte.getAdvertencia() != null) {
            Paragraph advertencia = new Paragraph("⚠ " + reporte.getAdvertencia(), FUENTE_ADVERTENCIA);
            advertencia.setSpacingBefore(6);
            advertencia.setSpacingAfter(4);
            doc.add(advertencia);
        }

        agregarLinea(doc);
    }

    private void agregarMetricasGlobales(Document doc, ReporteResponseDTO reporte) throws Exception {
        Paragraph seccion = new Paragraph("Métricas globales del período", FUENTE_SUBTITULO);
        seccion.setSpacingBefore(10);
        seccion.setSpacingAfter(6);
        doc.add(seccion);

        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);

        agregarCeldaMetrica(tabla, "Instancias", String.valueOf(reporte.getTotalInstancias()));
        agregarCeldaMetrica(tabla, "Incidencias", String.valueOf(reporte.getTotalIncidencias()));
        agregarCeldaMetrica(tabla, "Recordatorios", String.valueOf(reporte.getTotalRecordatorios()));
        agregarCeldaMetrica(tabla, "Recordatorios HOY", String.valueOf(reporte.getRecordatoriosHoy()));
        agregarCeldaMetrica(tabla, "Recordatorios VENCIDOS", String.valueOf(reporte.getRecordatoriosVencidos()));
        agregarCeldaMetrica(tabla, "Recordatorios FUTUROS", String.valueOf(reporte.getRecordatoriosFuturos()));

        doc.add(tabla);
        agregarLinea(doc);
    }

    private void agregarTablaDetalle(Document doc, ReporteResponseDTO reporte) throws Exception {
        if (reporte.getDetallePorEstudiante() == null || reporte.getDetallePorEstudiante().isEmpty()) {
            Paragraph vacio = new Paragraph("No hay estudiantes para mostrar con los filtros aplicados.", FUENTE_NORMAL);
            vacio.setSpacingBefore(10);
            doc.add(vacio);
            return;
        }

        Paragraph seccion = new Paragraph("Detalle por estudiante", FUENTE_SUBTITULO);
        seccion.setSpacingBefore(10);
        seccion.setSpacingAfter(6);
        doc.add(seccion);

        PdfPTable tabla = new PdfPTable(6);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{0.5f, 2.5f, 1.5f, 1f, 1f, 1f});

        // Cabecera
        String[] cabeceras = {"#", "Nombre completo", "Documento", "Instancias", "Incidencias", "Recordatorios"};
        for (String cabecera : cabeceras) {
            PdfPCell celda = new PdfPCell(new Phrase(cabecera, FUENTE_CABECERA_TABLA));
            celda.setBackgroundColor(COLOR_CABECERA);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setPadding(5);
            celda.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(celda);
        }

        // Filas
        int numero = 1;
        for (ReporteEstudianteDTO estudiante : reporte.getDetallePorEstudiante()) {
            BaseColor colorFila = (numero % 2 == 0) ? COLOR_FILA_PAR : BaseColor.WHITE;

            agregarCeldaTabla(tabla, String.valueOf(numero), colorFila, Element.ALIGN_CENTER);
            agregarCeldaTabla(tabla, estudiante.getNombreCompleto(), colorFila, Element.ALIGN_LEFT);
            agregarCeldaTabla(tabla, estudiante.getDocumento(), colorFila, Element.ALIGN_CENTER);
            agregarCeldaTabla(tabla, String.valueOf(estudiante.getCantidadInstancias()), colorFila, Element.ALIGN_CENTER);
            agregarCeldaTabla(tabla, String.valueOf(estudiante.getCantidadIncidencias()), colorFila, Element.ALIGN_CENTER);
            agregarCeldaTabla(tabla, String.valueOf(estudiante.getCantidadRecordatorios()), colorFila, Element.ALIGN_CENTER);

            numero++;
        }

        doc.add(tabla);
    }

    private void agregarPieDeDocumento(Document doc, ReporteResponseDTO reporte) throws Exception {
        agregarLinea(doc);

        Paragraph pie = new Paragraph(
                "Documento generado por SIENEP el " +
                        reporte.getFechaGeneracion().format(FORMATTER_DATETIME) +
                        ". La información oficial y fidedigna debe consultarse directamente en el sistema.",
                FUENTE_PEQUEÑA);
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(6);
        doc.add(pie);
    }

    // ── Helpers de construcción ───────────────────────────────

    private void agregarLinea(Document doc) throws Exception {
        Paragraph linea = new Paragraph(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(
                0.5f, 100, BaseColor.LIGHT_GRAY, Element.ALIGN_CENTER, -2)));
        linea.setSpacingBefore(4);
        linea.setSpacingAfter(4);
        doc.add(linea);
    }

    private void agregarFilaMetadata(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, FUENTE_NEGRITA));
        celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
        celdaEtiqueta.setPadding(3);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "-", FUENTE_NORMAL));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        celdaValor.setPadding(3);
        tabla.addCell(celdaValor);
    }

    private void agregarCeldaMetrica(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(BaseColor.LIGHT_GRAY);
        celda.setPadding(8);

        Paragraph p = new Paragraph();
        p.add(new Chunk(valor + "\n", FUENTE_TITULO));
        p.add(new Chunk(etiqueta, FUENTE_PEQUEÑA));
        p.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(p);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }

    private void agregarCeldaTabla(PdfPTable tabla, String texto, BaseColor colorFondo, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto != null ? texto : "-", FUENTE_CELDA_TABLA));
        celda.setBackgroundColor(colorFondo);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(4);
        celda.setBorderColor(BaseColor.LIGHT_GRAY);
        tabla.addCell(celda);
    }
}
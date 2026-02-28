package com.nexus.data.repository

import android.content.Context
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentExtractor @Inject constructor(private val context: Context) {

    fun extractText(file: File): String = try {
        when (file.extension.lowercase()) {
            "pdf"        -> extractPdf(file)
            "docx"       -> extractDocx(file)
            "doc"        -> extractDoc(file)
            "xlsx", "xls", "csv" -> extractExcel(file)
            "pptx"       -> extractPptx(file)
            "txt", "md", "log"   -> file.readText(Charsets.UTF_8)
            "json"       -> file.readText(Charsets.UTF_8)
            else         -> ""
        }
    } catch (e: Exception) {
        ""
    }

    private fun extractPdf(file: File): String {
        PDDocument.load(file).use { doc ->
            return PDFTextStripper().getText(doc)
        }
    }

    private fun extractDocx(file: File): String {
        XWPFDocument(file.inputStream()).use { doc ->
            return doc.paragraphs.joinToString("\n") { it.text }
        }
    }

    private fun extractDoc(file: File): String {
        // Basic DOC support via POI HWPF
        return try {
            val hwpf = org.apache.poi.hwpf.HWPFDocument(file.inputStream())
            hwpf.range.text()
        } catch (e: Exception) { "" }
    }

    private fun extractExcel(file: File): String {
        if (file.extension.lowercase() == "csv") return file.readText()
        WorkbookFactory.create(file.inputStream()).use { wb ->
            val sb = StringBuilder()
            for (i in 0 until wb.numberOfSheets) {
                val sheet = wb.getSheetAt(i)
                sb.appendLine("SHEET: ${sheet.sheetName}")
                sheet.forEach { row ->
                    val line = row.joinToString("\t") { cell ->
                        cell.toString()
                    }
                    sb.appendLine(line)
                }
            }
            return sb.toString()
        }
    }

    private fun extractPptx(file: File): String {
        XMLSlideShow(file.inputStream()).use { ppt ->
            return ppt.slides.joinToString("\n---\n") { slide ->
                slide.shapes.joinToString("\n") { shape ->
                    if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape)
                        shape.text else ""
                }
            }
        }
    }

    val supportedExtensions = setOf(
        "pdf", "docx", "doc", "xlsx", "xls", "csv",
        "pptx", "txt", "md", "log", "json"
    )
}

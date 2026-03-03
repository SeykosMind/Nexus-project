package com.nexus.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class DocumentExtractor @Inject constructor(private val context: Context) {

    private val ocrRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Extracción síncrona para archivos normales
    fun extractText(file: File): String = try {
        when (file.extension.lowercase()) {
            "pdf"               -> extractPdf(file)
            "docx"             -> extractDocx(file)
            "doc"              -> extractDoc(file)
            "xlsx", "xls"      -> extractExcel(file)
            "csv"              -> file.readText()
            "pptx"             -> extractPptx(file)
            "txt", "md", "log" -> file.readText(Charsets.UTF_8)
            "json"             -> file.readText(Charsets.UTF_8)
            else               -> ""
        }
    } catch (e: Exception) { "" }

    // Extracción con soporte OCR para imágenes (suspend)
    suspend fun extractTextSuspend(file: File): String = try {
        when (file.extension.lowercase()) {
            "jpg", "jpeg", "png", "webp", "bmp" -> extractOcr(file)
            else -> extractText(file)
        }
    } catch (e: Exception) { "" }

    // OCR con Google MLKit — 100% on-device, sin internet
    private suspend fun extractOcr(file: File): String =
        suspendCancellableCoroutine { cont ->
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: run { cont.resume(""); return@suspendCancellableCoroutine }
                val image = InputImage.fromBitmap(bitmap, 0)
                ocrRecognizer.process(image)
                    .addOnSuccessListener { result -> cont.resume(result.text) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

    private fun extractPdf(file: File): String = try {
        val reader = com.itextpdf.kernel.pdf.PdfReader(file)
        val doc = com.itextpdf.kernel.pdf.PdfDocument(reader)
        val sb = StringBuilder()
        for (i in 1..doc.numberOfPages) {
            sb.append(
                com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
                    .getTextFromPage(doc.getPage(i))
            )
            sb.append("\n")
        }
        doc.close()
        sb.toString()
    } catch (e: Exception) { "" }

    private fun extractDocx(file: File): String {
        XWPFDocument(file.inputStream()).use { doc ->
            return doc.paragraphs.joinToString("\n") { it.text }
        }
    }

    private fun extractDoc(file: File): String = try {
        org.apache.poi.hwpf.HWPFDocument(file.inputStream()).range.text()
    } catch (e: Exception) { "" }

    private fun extractExcel(file: File): String {
        WorkbookFactory.create(file.inputStream()).use { wb ->
            val sb = StringBuilder()
            for (i in 0 until wb.numberOfSheets) {
                val sheet = wb.getSheetAt(i)
                sb.appendLine("SHEET: ${sheet.sheetName}")
                sheet.forEach { row ->
                    sb.appendLine(row.joinToString("\t") { cell -> cell.toString() })
                }
            }
            return sb.toString()
        }
    }

    private fun extractPptx(file: File): String {
        XMLSlideShow(file.inputStream()).use { ppt ->
            return ppt.slides.joinToString("\n---\n") { slide ->
                slide.shapes.joinToString("\n") { shape ->
                    if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) shape.text else ""
                }
            }
        }
    }

    val supportedExtensions = setOf(
        "pdf", "docx", "doc", "xlsx", "xls", "csv",
        "pptx", "txt", "md", "log", "json"
    )

    val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

    val allSupportedExtensions = supportedExtensions + imageExtensions
}

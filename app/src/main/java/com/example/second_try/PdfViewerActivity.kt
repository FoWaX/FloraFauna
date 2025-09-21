package com.example.second_try

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.github.barteksc.pdfviewer.PDFView

class PdfViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем имя PDF-файла из Intent
        val pdfName = intent.getStringExtra("pdf_name") ?: return
        val pdfResId = resources.getIdentifier(pdfName, "raw", packageName)

        // Создаем корневой FrameLayout
        val rootLayout = FrameLayout(this)

        // Создаем PDFView
        val pdfView = PDFView(this, null).apply {
            fromStream(resources.openRawResource(pdfResId))
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .load()
        }

        // Создаем кнопку "Назад"
        val backButton = Button(this).apply {
            text = "← Назад"
            textSize = 16f
            setPadding(32, 16, 32, 16)
            setBackgroundColor(0xAAFFFFFF.toInt()) // полупрозрачный белый фон
            setOnClickListener { finish() }
        }

        // Параметры для размещения кнопки в левом верхнем углу
        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            topMargin = 40
            marginStart = 24
        }

        // Добавляем в layout сначала PDFView, потом кнопку
        rootLayout.addView(pdfView)
        rootLayout.addView(backButton, buttonParams)

        // Устанавливаем layout как контент
        setContentView(rootLayout)
    }
}

package com.nepdire.customorinteradapterexample

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.print.pdf.PrintedPdfDocument
import android.view.View
import android.widget.Button
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val printButton = findViewById<Button>(R.id.button)
        printButton.setOnClickListener {
            printDocument(it)
        }
    }

    fun printDocument(View: View){
        var printManager = this.getSystemService(Context.PRINT_SERVICE) as PrintManager
        var jobName = this.getString(R.string.app_name) + " Document"

        printManager.print(jobName, MyPrintDocumentAdapter(this),null)



    }

    public class MyPrintDocumentAdapter(var context: Context): PrintDocumentAdapter() {

        var pageHeight: Int? = null
        var pageWidth: Int? = null
        var myPdfDocument: PdfDocument? = PdfDocument()
        var totalPages = 4


        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            myPdfDocument = PrintedPdfDocument(this.context, newAttributes!!)
            pageHeight = newAttributes.mediaSize?.heightMils!!/1000*72
            pageWidth = newAttributes.mediaSize?.widthMils!!/1000*72

            if (cancellationSignal!!.isCanceled()){
                callback!!.onLayoutCancelled()
                return
            }

            if (totalPages>0){
                var builder = PrintDocumentInfo.Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(totalPages)

                var info = builder.build()
                callback!!.onLayoutFinished(info, true)
            } else {
                callback!!.onLayoutFailed("Page count is zero")
            }
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            for ( i in 1..totalPages) {
                if (pageInRange(pages, i)) {
                    var newPage =
                        PdfDocument.PageInfo.Builder(pageWidth!!, pageHeight!!, i).create()
                    var page = myPdfDocument!!.startPage(newPage)

                    if (cancellationSignal!!.isCanceled) {
                        callback!!.onWriteCancelled()
                        myPdfDocument!!.close()
                        myPdfDocument = null
                        return
                    }

                    drawPage(page, i)
                    myPdfDocument!!.finishPage(page)
                }
            }

            try {
                myPdfDocument!!.writeTo(FileOutputStream(destination!!.fileDescriptor))
            } catch (e:IOException){
                callback!!.onWriteFailed(e.toString())
                return
            } finally {
                myPdfDocument!!.close()
                myPdfDocument = null
            }

            callback!!.onWriteFinished(pages)
        }

        private fun pageInRange(pages: Array<out PageRange>?, page: Int): Boolean {
            for (pageRange in pages!!){
                if ((page >= pageRange.start) && (page <=pageRange.end)){
                    return true
                }


            }
            return false

        }

        private fun drawPage(page: PdfDocument.Page?, pageNumber: Int) {
            var pageNumber = pageNumber
            var canvas = page!!.canvas
            pageNumber++

            var titleBBaseLine = 72f
            var leftMargin = 54f
            var paint = Paint()
            paint.setColor(Color.BLACK)
            paint.textSize = 40f
            canvas.drawText("Test Print Document Page" + pageNumber, leftMargin,titleBBaseLine,paint)
            paint.textSize = 14f
            canvas.drawText("This is somw test conent to verify that custom document printing works",
            leftMargin,
            titleBBaseLine,
            paint)

            if (pageNumber%2 == 0){
                paint.setColor(Color.RED)
            } else {
                paint.setColor(Color.GREEN)
            }

            var pageInfo = page.info

            canvas.drawCircle(pageInfo.pageWidth/2f,
            pageInfo.pageHeight/2f,
            150f,
            paint)

        }

    }
}

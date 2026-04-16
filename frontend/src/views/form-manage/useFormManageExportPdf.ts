import { snapdom } from '@zumer/snapdom'
import { ElMessage } from 'element-plus'
import { jsPDF } from 'jspdf'
import { nextTick } from 'vue'

const PDF_CAPTURE_SCALE = 2
const PDF_A4_WIDTH_MM = 210
const PDF_A4_HEIGHT_MM = 297

const createPdfPageCanvas = (sourceCanvas: HTMLCanvasElement, startY: number, sliceHeight: number) => {
  const pageCanvas = document.createElement('canvas')
  pageCanvas.width = sourceCanvas.width
  pageCanvas.height = sliceHeight

  const context = pageCanvas.getContext('2d')
  if (!context) {
    throw new Error('创建 PDF 分页画布失败')
  }

  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, pageCanvas.width, pageCanvas.height)
  context.drawImage(sourceCanvas, 0, startY, sourceCanvas.width, sliceHeight, 0, 0, sourceCanvas.width, sliceHeight)

  return pageCanvas
}

export const useFormManageExportPdf = () => {
  const handleExportPdf = async (element: HTMLElement | null | undefined, formName?: string | null) => {
    const previewTitle = `${formName || '表单'}.pdf`

    if (!element) {
      return
    }

    try {
      await nextTick()

      const exportCanvas = await snapdom.toCanvas(element, {
        backgroundColor: '#ffffff',
        embedFonts: true,
        scale: PDF_CAPTURE_SCALE,
      })
      const pdf = new jsPDF({
        compress: true,
        format: 'a4',
        orientation: 'portrait',
        unit: 'mm',
      })

      pdf.setDocumentProperties({
        author: 'Formly',
        creator: 'Formly',
        subject: previewTitle,
        title: previewTitle,
      })

      const renderedWidth = PDF_A4_WIDTH_MM
      const pagePixelHeight = Math.max(1, Math.floor((PDF_A4_HEIGHT_MM * exportCanvas.width) / PDF_A4_WIDTH_MM))

      let offsetY = 0
      let isFirstPage = true

      while (offsetY < exportCanvas.height) {
        const currentSliceHeight = Math.min(pagePixelHeight, exportCanvas.height - offsetY)
        const pageCanvas = createPdfPageCanvas(exportCanvas, offsetY, currentSliceHeight)
        const renderedHeight = (currentSliceHeight * renderedWidth) / exportCanvas.width

        if (!isFirstPage) {
          pdf.addPage()
        }

        pdf.addImage(pageCanvas.toDataURL('image/png'), 'PNG', 0, 0, renderedWidth, renderedHeight)

        offsetY += currentSliceHeight
        isFirstPage = false
      }

      const pdfBlob = pdf.output('blob')
      const pdfUrl = URL.createObjectURL(pdfBlob)
      const previewWindow = window.open('', '_blank')

      if (!previewWindow) {
        URL.revokeObjectURL(pdfUrl)
        throw new Error('浏览器拦截了 PDF 预览窗口')
      }

      const escapedPreviewTitle = previewTitle
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')

      previewWindow.document.open()
      previewWindow.document.write(`<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${escapedPreviewTitle}</title>
    <style>
      html, body {
        margin: 0;
        height: 100%;
        background: #f8fafc;
      }

      iframe {
        border: 0;
        width: 100%;
        height: 100%;
        display: block;
      }
    </style>
  </head>
  <body>
    <iframe src="${pdfUrl}" title="${escapedPreviewTitle}"></iframe>
  </body>
</html>`)
      previewWindow.document.close()
      previewWindow.addEventListener(
        'beforeunload',
        () => {
          URL.revokeObjectURL(pdfUrl)
        },
        { once: true },
      )

      window.setTimeout(() => {
        URL.revokeObjectURL(pdfUrl)
      }, 60_000)

      ElMessage.success('PDF 已在新窗口打开')
    } catch (error) {
      console.error(error)
      ElMessage.error('PDF 导出失败，请稍后重试')
    }
  }

  return {
    handleExportPdf,
  }
}

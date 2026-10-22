package org.example.pdf;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.PDFStreamEngine;
import org.apache.pdfbox.util.ResourceLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CizgiOkuyucu extends PDFStreamEngine {

    public static class Cizgi {
        public float x1, y1, x2, y2;
        public Cizgi(float x1, float y1, float x2, float y2) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        }
    }

    public final List<Cizgi> cizgiler = new ArrayList<Cizgi>();
    private float sayfaYuksekligi;
    private float curX, curY;

    public CizgiOkuyucu() throws IOException {
        super(ResourceLoader.loadProperties(
                "org/apache/pdfbox/resources/PDFTextStripper.properties", true));
    }

    public void sayfayiIsle(PDPage page) throws IOException {
        cizgiler.clear();
        sayfaYuksekligi = page.findMediaBox().getHeight();
        if (page.getContents() != null) {
            processStream(page, page.findResources(), page.getContents().getStream());
        }
    }

    @Override
    protected void processOperator(PDFOperator operator, List<COSBase> arguments) throws IOException {
        String op = operator.getOperation();
        if ("m".equals(op)) {
            curX = f(arguments, 0); curY = f(arguments, 1);
        } else if ("l".equals(op)) {
            float x = f(arguments, 0), y = f(arguments, 1);
            ekle(curX, curY, x, y); curX = x; curY = y;
        } else if ("re".equals(op)) {
            float x = f(arguments, 0), y = f(arguments, 1), w = f(arguments, 2), h = f(arguments, 3);
            ekle(x, y, x + w, y);
            ekle(x + w, y, x + w, y + h);
            ekle(x + w, y + h, x, y + h);
            ekle(x, y + h, x, y);
        }
        super.processOperator(operator, arguments);
    }

    private void ekle(float ux1, float uy1, float ux2, float uy2) {
        float[] p1 = donustur(ux1, uy1);
        float[] p2 = donustur(ux2, uy2);
        cizgiler.add(new Cizgi(p1[0], p1[1], p2[0], p2[1]));
    }

    private float[] donustur(float x, float y) {
        Matrix m = getGraphicsState().getCurrentTransformationMatrix();
        float px = x * m.getValue(0, 0) + y * m.getValue(1, 0) + m.getValue(2, 0);
        float py = x * m.getValue(0, 1) + y * m.getValue(1, 1) + m.getValue(2, 1);
        return new float[]{px, sayfaYuksekligi - py};
    }

    private static float f(List<COSBase> a, int i) {
        return ((COSNumber) a.get(i)).floatValue();
    }
}
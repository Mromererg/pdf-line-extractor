package org.example.pdf;

import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KelimeOkuyucu extends PDFTextStripper {

    public static class Parca {
        public int sayfa, x, sag, y;
        public String metin;
        public Parca(int sayfa, int x, int sag, int y, String metin) {
            this.sayfa = sayfa; this.x = x; this.sag = sag; this.y = y; this.metin = metin;
        }
    }

    public final List<Parca> parcalar = new ArrayList<Parca>();

    public KelimeOkuyucu() throws IOException { super(); }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        int n = textPositions.size();
        int i = 0;
        while (i < n) {
            while (i < n && bosMu(textPositions.get(i))) i++;
            if (i >= n) break;

            int bas = i;
            StringBuilder sb = new StringBuilder();
            TextPosition onceki = null;
            while (i < n) {
                TextPosition tp = textPositions.get(i);
                if (bosMu(tp)) break;
                if (onceki != null) {
                    float bosluk = tp.getX() - (onceki.getX() + onceki.getWidth());
                    float bosGen = onceki.getWidthOfSpace();
                    if (bosGen <= 0) bosGen = onceki.getWidth();
                    if (bosluk > bosGen * 0.5f) break;
                }
                sb.append(tp.getCharacter());
                onceki = tp;
                i++;
            }

            String kelime = sb.toString().trim();
            if (!kelime.isEmpty()) {
                TextPosition ilk = textPositions.get(bas);
                TextPosition son = textPositions.get(i - 1);
                int xsol = Math.round(ilk.getX());
                int xsag = Math.round(son.getX() + son.getWidth());
                int y = Math.round(ilk.getY());
                parcalar.add(new Parca(getCurrentPageNo(), xsol, xsag, y, kelime));
            }
        }
    }

    private static boolean bosMu(TextPosition tp) {
        String c = tp.getCharacter();
        return c == null || c.trim().length() == 0;
    }
}
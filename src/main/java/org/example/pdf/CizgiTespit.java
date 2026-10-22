package org.example.pdf;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CizgiTespit {

    public final List<float[]> yatay = new ArrayList<float[]>();  // {y, x1, x2}
    public final List<float[]> dikey = new ArrayList<float[]>();  // {x, y1, y2}

    // esik: gri eşiği (altı = koyu), minUz: min çizgi uzunluğu (piksel)
    public void bul(BufferedImage img, int esik, int minUzYatay, int minUzDikey) {
        yatay.clear(); dikey.clear();
        int W = img.getWidth(), H = img.getHeight();
        boolean[][] koyu = new boolean[H][W];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y);
                int gri = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                koyu[y][x] = gri < esik;
            }

        List<float[]> hamY = new ArrayList<float[]>();
        for (int y = 0; y < H; y++) {
            int bas = -1;
            for (int x = 0; x < W; x++) {
                if (koyu[y][x]) { if (bas < 0) bas = x; }
                else { if (bas >= 0 && x - bas >= minUzYatay) hamY.add(new float[]{y, bas, x - 1}); bas = -1; }
            }
            if (bas >= 0 && W - bas >= minUzYatay) hamY.add(new float[]{y, bas, W - 1});
        }

        List<float[]> hamD = new ArrayList<float[]>();
        for (int x = 0; x < W; x++) {
            int bas = -1;
            for (int y = 0; y < H; y++) {
                if (koyu[y][x]) { if (bas < 0) bas = y; }
                else { if (bas >= 0 && y - bas >= minUzDikey) hamD.add(new float[]{x, bas, y - 1}); bas = -1; }
            }
            if (bas >= 0 && H - bas >= minUzDikey) hamD.add(new float[]{x, bas, H - 1});
        }

        yatay.addAll(birlestir(hamY));
        dikey.addAll(birlestir(hamD));
    }

    // yakın/örtüşen çizgileri birleştir
    private static List<float[]> birlestir(List<float[]> ham) {
        Collections.sort(ham, new Comparator<float[]>() {
            public int compare(float[] a, float[] b) { return Float.compare(a[0], b[0]); }
        });
        List<float[]> sonuc = new ArrayList<float[]>();
        for (float[] c : ham) {
            boolean birlesti = false;
            for (float[] s : sonuc)
                if (Math.abs(s[0] - c[0]) <= 3 && c[1] <= s[2] + 3 && c[2] >= s[1] - 3) {
                    s[1] = Math.min(s[1], c[1]); s[2] = Math.max(s[2], c[2]);
                    birlesti = true; break;
                }
            if (!birlesti) sonuc.add(new float[]{c[0], c[1], c[2]});
        }
        return sonuc;
    }
}
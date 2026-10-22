package org.example.pdf;

import org.example.pdf.KelimeOkuyucu.Parca;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class OcrOkuyucu {

    // Tesseract'ın tam yolu (kendi kurulumuna göre)
// Tesseract komutu: önce -Dtesseract, sonra TESSERACT_PATH env, yoksa PATH'teki "tesseract"
    static final String TESSERACT = cozTesseract();

    private static String cozTesseract() {
        String p = System.getProperty("tesseract");
        if (p == null) p = System.getenv("TESSERACT_PATH");
        return (p != null && !p.isEmpty()) ? p : "tesseract";
    }    // gri tonlama + kontrast germe (mavi/soluk yazıyı OCR'a hazırlar)
    public static java.awt.image.BufferedImage onIsle(java.awt.image.BufferedImage img) {
        int W = img.getWidth(), H = img.getHeight();
        int[] gri = new int[W * H];
        int[] hist = new int[256];
        for (int y = 0, i = 0; y < H; y++)
            for (int x = 0; x < W; x++, i++) {
                int rgb = img.getRGB(x, y);
                int g = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                gri[i] = g; hist[g]++;
            }
        int kes = (W * H) / 100, birikim = 0, gmin = 0, gmax = 255;      // %1 kesme
        for (int g = 0; g < 256; g++) { birikim += hist[g]; if (birikim > kes) { gmin = g; break; } }
        birikim = 0;
        for (int g = 255; g >= 0; g--) { birikim += hist[g]; if (birikim > kes) { gmax = g; break; } }
        if (gmax <= gmin) { gmin = 0; gmax = 255; }
        java.awt.image.BufferedImage sonuc =
                new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0, i = 0; y < H; y++)
            for (int x = 0; x < W; x++, i++) {
                int g = (gri[i] - gmin) * 255 / (gmax - gmin);
                if (g < 0) g = 0; if (g > 255) g = 255;
                sonuc.setRGB(x, y, (g << 16) | (g << 8) | g);
            }
        return sonuc;
    }
    // Bir sayfa resmini Tesseract ile OCR'layıp Parca listesi döndürür
    public static List<Parca> oku(BufferedImage img, int sayfaNo) throws IOException, InterruptedException {
        File png = File.createTempFile("ocr_", ".png");
        String taban = png.getAbsolutePath().substring(0, png.getAbsolutePath().length() - 4); // ".png" at
        ImageIO.write(img, "png", png);

        // tesseract <png> <taban> -l tur tsv   ->  <taban>.tsv üretir
        ProcessBuilder pb = new ProcessBuilder(
                TESSERACT, png.getAbsolutePath(), taban, "-l", "tur", "tsv");
        pb.redirectErrorStream(true);
        Process pr = pb.start();
        BufferedReader cik = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        while (cik.readLine() != null) { /* çıktıyı tüket */ }
        pr.waitFor();

        List<Parca> parcalar = new ArrayList<Parca>();
        File tsv = new File(taban + ".tsv");
        BufferedReader tr = new BufferedReader(new InputStreamReader(new FileInputStream(tsv), "UTF-8"));
        String satir; boolean ilk = true;
        while ((satir = tr.readLine()) != null) {
            if (ilk) { ilk = false; continue; }                 // başlık satırı
            String[] a = satir.split("\t", -1);
            if (a.length < 12 || !a[0].equals("5")) continue;   // sadece kelime seviyesi
            String metin = a[11].trim();
            if (metin.isEmpty()) continue;
            if (sayiF(a[10]) < 15) continue;   // sadece çok düşük güvenliyi at                   // düşük güvenli kelimeleri at
            int left = sayiI(a[6]), top = sayiI(a[7]), w = sayiI(a[8]);
            parcalar.add(new Parca(sayfaNo, left, left + w, top, metin));
        }
        tr.close();
        png.delete(); tsv.delete();
        return parcalar;
    }

    private static int sayiI(String s) { try { return (int) Float.parseFloat(s); } catch (Exception e) { return 0; } }
    private static float sayiF(String s) { try { return Float.parseFloat(s); } catch (Exception e) { return 0; } }
}
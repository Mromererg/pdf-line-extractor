package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.example.model.Tablo;
import org.example.pdf.CizgiOkuyucu;
import org.example.pdf.CizgiTespit;
import org.example.pdf.KelimeOkuyucu;
import org.example.pdf.KelimeOkuyucu.Parca;
import org.example.pdf.OcrOkuyucu;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Main {

    static final int SAYFA_OFSET = 0;

    static class Bolge {
        List<Integer> sutunX;
        List<Integer> satirY;
        List<float[]> yatay;
    }

    static String baslikBul(Bolge b, List<Parca> parcalar) {
        int ustY = b.satirY.get(0);
        int xl = b.sutunX.get(0), xr = b.sutunX.get(b.sutunX.size() - 1);
        List<Parca> ad = new ArrayList<Parca>();
        for (Parca p : parcalar)
            if (p.y < ustY && p.y >= ustY - 90 && p.sag > xl && p.x < xr) ad.add(p);
        if (ad.isEmpty()) return "";
        Collections.sort(ad, new Comparator<Parca>() {
            public int compare(Parca a, Parca c) { return a.y != c.y ? a.y - c.y : a.x - c.x; }
        });
        List<List<Parca>> satirlar = new ArrayList<List<Parca>>();
        for (Parca p : ad) {
            if (!satirlar.isEmpty() && Math.abs(p.y - satirlar.get(satirlar.size() - 1).get(0).y) <= 6)
                satirlar.get(satirlar.size() - 1).add(p);
            else { List<Parca> y = new ArrayList<Parca>(); y.add(p); satirlar.add(y); }
        }
        List<List<Parca>> secili = new ArrayList<List<Parca>>();
        for (int i = satirlar.size() - 1; i >= 0; i--) {
            if (!secili.isEmpty()) {
                int altY = secili.get(secili.size() - 1).get(0).y;
                if (altY - satirlar.get(i).get(0).y > 22) break;
            }
            secili.add(satirlar.get(i));
        }
        Collections.reverse(secili);
        StringBuilder sb = new StringBuilder();
        for (List<Parca> satir : secili) {
            Collections.sort(satir, new Comparator<Parca>() {
                public int compare(Parca a, Parca c) { return a.x - c.x; }
            });
            for (Parca p : satir) { if (sb.length() > 0) sb.append(" "); sb.append(p.metin); }
        }
        return sb.toString().trim();
    }

    static List<float[]> yataylar(List<CizgiOkuyucu.Cizgi> cizgiler) {
        List<float[]> ham = new ArrayList<float[]>();
        for (CizgiOkuyucu.Cizgi c : cizgiler)
            if (Math.abs(c.y1 - c.y2) <= 2) {
                float x1 = Math.min(c.x1, c.x2), x2 = Math.max(c.x1, c.x2);
                if (x2 - x1 < 5) continue;
                ham.add(new float[]{(c.y1 + c.y2) / 2f, x1, x2});
            }
        return birlestir(ham);
    }

    static List<float[]> dikeyler(List<CizgiOkuyucu.Cizgi> cizgiler) {
        List<float[]> ham = new ArrayList<float[]>();
        for (CizgiOkuyucu.Cizgi c : cizgiler)
            if (Math.abs(c.x1 - c.x2) <= 2) {
                float y1 = Math.min(c.y1, c.y2), y2 = Math.max(c.y1, c.y2);
                if (y2 - y1 < 5) continue;
                ham.add(new float[]{(c.x1 + c.x2) / 2f, y1, y2});
            }
        return birlestir(ham);
    }

    static List<float[]> birlestir(List<float[]> ham) {
        Collections.sort(ham, new Comparator<float[]>() {
            public int compare(float[] a, float[] b) { return Float.compare(a[0], b[0]); }
        });
        List<float[]> sonuc = new ArrayList<float[]>();
        for (float[] c : ham) {
            boolean birlesti = false;
            for (float[] s : sonuc)
                if (Math.abs(s[0] - c[0]) <= 2 && c[1] <= s[2] + 2 && c[2] >= s[1] - 2) {
                    s[1] = Math.min(s[1], c[1]); s[2] = Math.max(s[2], c[2]);
                    birlesti = true; break;
                }
            if (!birlesti) sonuc.add(new float[]{c[0], c[1], c[2]});
        }
        return sonuc;
    }

    static List<Bolge> tablolariBul(List<float[]> yl, List<float[]> dl) {
        List<float[]> dik = new ArrayList<float[]>(dl);
        List<float[]> yat = new ArrayList<float[]>(yl);
        if (yat.isEmpty()) return new ArrayList<Bolge>();
        Collections.sort(yat, new Comparator<float[]>() {
            public int compare(float[] a, float[] b) { return Float.compare(a[0], b[0]); }
        });
        float minY = yat.get(0)[0], maxY = yat.get(yat.size() - 1)[0];
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (float[] d : dik) {
            if (d[1] < minY) minY = d[1];
            if (d[2] > maxY) maxY = d[2];
            if (d[0] < minX) minX = d[0];
            if (d[0] > maxX) maxX = d[0];
        }
        float sayfaBoyu = maxY - minY;
        List<Bolge> bolgeler = new ArrayList<Bolge>();
        int i = 0, n = yat.size();
        while (i < n) {
            int j = i;
            while (j + 1 < n) {
                float bosluk = yat.get(j + 1)[0] - yat.get(j)[0];
                if (bosluk <= 25) { j++; continue; }
                if (sutunKopru(dik, yat.get(j)[0], yat.get(j + 1)[0], sayfaBoyu, minX, maxX)) { j++; continue; }
                break;
            }
            float kLo = yat.get(i)[0], kHi = yat.get(j)[0];
            java.util.TreeSet<Integer> satir = new java.util.TreeSet<Integer>();
            List<float[]> kYatay = new ArrayList<float[]>();
            for (int t = i; t <= j; t++) { satir.add(Math.round(yat.get(t)[0])); kYatay.add(yat.get(t)); }
            java.util.TreeSet<Integer> sutun = new java.util.TreeSet<Integer>();
            for (float[] d : dik) if (d[1] <= kHi + 5 && d[2] >= kLo - 5) sutun.add(Math.round(d[0]));
            i = j + 1;
            if (sutun.size() < 2 || satir.size() < 2) continue;
            Bolge b = new Bolge();
            b.sutunX = new ArrayList<Integer>(sutun);
            b.satirY = new ArrayList<Integer>(satir);
            b.yatay = kYatay;
            bolgeler.add(b);
        }
        return bolgeler;
    }

    static boolean sutunKopru(List<float[]> dik, float y1, float y2, float sayfaBoyu, float minX, float maxX) {
        for (float[] d : dik) {
            boolean tamBoy = (d[2] - d[1]) > sayfaBoyu * 0.8f;
            boolean kenar = Math.abs(d[0] - minX) <= 3 || Math.abs(d[0] - maxX) <= 3;
            if (tamBoy && kenar) continue;
            if (d[1] <= y1 + 3 && d[2] >= y2 - 3) return true;
        }
        return false;
    }

    static int bandBul(List<Integer> sinir, int v) {
        for (int i = 0; i < sinir.size() - 1; i++)
            if (v >= sinir.get(i) - 2 && v < sinir.get(i + 1)) return i;
        return -1;
    }

    static List<List<String>> hucreleriDoldur(Bolge b, List<Parca> parcalar) {
        int sut = b.sutunX.size() - 1, sat = b.satirY.size() - 1;
        String[][] g = new String[sat][sut];
        for (Parca p : parcalar) {
            int cx = (p.x + p.sag) / 2, cy = p.y;
            int r = bandBul(b.satirY, cy);
            int c = bandBul(b.sutunX, cx);
            if (r < 0 || c < 0) continue;
            g[r][c] = (g[r][c] == null ? "" : g[r][c] + " ") + p.metin;
        }
        List<List<String>> satirlar = new ArrayList<List<String>>();
        for (int r = 0; r < sat; r++) {
            List<String> satir = new ArrayList<String>();
            for (int c = 0; c < sut; c++) satir.add(g[r][c] == null ? "" : g[r][c]);
            satirlar.add(satir);
        }
        return satirlar;
    }

    static void birlesikDoldur(Bolge b, List<List<String>> satirlar) {
        int sat = satirlar.size(), sut = b.sutunX.size() - 1;
        for (int c = 0; c < sut; c++) {
            int xl = b.sutunX.get(c), xr = b.sutunX.get(c + 1);
            int r = 0;
            while (r < sat) {
                int son = r;
                while (son + 1 < sat && !cizgiKesiyor(b, b.satirY.get(son + 1), xl, xr)) son++;
                if (son > r) {
                    StringBuilder sb = new StringBuilder();
                    for (int k = r; k <= son; k++) {
                        String v = satirlar.get(k).get(c);
                        if (v != null && !v.isEmpty()) { if (sb.length() > 0) sb.append(" "); sb.append(v); }
                    }
                    String deger = sb.toString();
                    if (!deger.isEmpty())
                        for (int k = r; k <= son; k++) satirlar.get(k).set(c, deger);
                }
                r = son + 1;
            }
        }
    }

    static boolean cizgiKesiyor(Bolge b, int y, int xl, int xr) {
        for (float[] h : b.yatay)
            if (Math.abs(h[0] - y) <= 3 && h[1] <= xl + 3 && h[2] >= xr - 3) return true;
        return false;
    }

    static Tablo tabloBul(List<Tablo> tablolar, int sayfa, int tabloNo) {
        for (Tablo t : tablolar)
            if (t.sayfa == sayfa && t.tabloNo == tabloNo) return t;
        return null;
    }

    static List<List<String>> bosluklariAt(List<List<String>> satirlar) {
        if (satirlar.isEmpty()) return satirlar;
        int sut = satirlar.get(0).size();
        boolean[] sutunDolu = new boolean[sut];
        for (List<String> satir : satirlar)
            for (int c = 0; c < sut; c++)
                if (satir.get(c) != null && !satir.get(c).isEmpty()) sutunDolu[c] = true;
        List<List<String>> sonuc = new ArrayList<List<String>>();
        List<String> onceki = null;
        for (List<String> satir : satirlar) {
            List<String> yeni = new ArrayList<String>();
            boolean dolu = false;
            for (int c = 0; c < sut; c++) {
                if (!sutunDolu[c]) continue;
                String v = satir.get(c) == null ? "" : satir.get(c);
                yeni.add(v);
                if (!v.isEmpty()) dolu = true;
            }
            if (!dolu) continue;
            if (onceki != null && yeni.equals(onceki)) continue;
            sonuc.add(yeni);
            onceki = yeni;
        }
        return sonuc;
    }

    // --- OCR yardimcilari ---
    static List<Integer> ocrSatirSinirlari(List<Parca> parcalar, List<Integer> sutunX, int bosluk) {
        int sinirX = sutunX.size() > 2 ? sutunX.get(2)
                : (sutunX.size() > 1 ? sutunX.get(1) : Integer.MAX_VALUE);
        List<Integer> ys = new ArrayList<Integer>();
        for (Parca p : parcalar) {
            int cx = (p.x + p.sag) / 2;
            if (cx < sinirX) ys.add(p.y);          // sadece sol (POZ) sütunları
        }
        if (ys.size() < 2) { ys.clear(); for (Parca p : parcalar) ys.add(p.y); }  // yedek
        Collections.sort(ys);
        List<int[]> kume = new ArrayList<int[]>();
        int kmin = ys.get(0), kmax = ys.get(0);
        for (int i = 1; i < ys.size(); i++) {
            if (ys.get(i) - kmin > bosluk) { kume.add(new int[]{kmin, kmax}); kmin = ys.get(i); }
            kmax = ys.get(i);
        }
        kume.add(new int[]{kmin, kmax});
        List<Integer> sinir = new ArrayList<Integer>();
        sinir.add(kume.get(0)[0] - 5);
        for (int i = 0; i + 1 < kume.size(); i++)
            sinir.add((kume.get(i)[1] + kume.get(i + 1)[0]) / 2);
        sinir.add(kume.get(kume.size() - 1)[1] + 15);
        return sinir;
    }

    static List<Integer> sutunSinirlari(List<float[]> dikey, int tol) {
        List<Integer> xs = new ArrayList<Integer>();
        for (float[] d : dikey) xs.add(Math.round(d[0]));
        Collections.sort(xs);
        List<Integer> sonuc = new ArrayList<Integer>();
        for (int x : xs)
            if (sonuc.isEmpty() || x - sonuc.get(sonuc.size() - 1) > tol) sonuc.add(x);
        return sonuc;
    }

    // Bolge -> Tablo (ortak isleme; birlesikUygula sadece dijitalde true)
    static Tablo tabloYap(Bolge b, List<Parca> parcalar, int sayfaYazdir, int no, boolean birlesikUygula) {
        List<List<String>> satirlar = hucreleriDoldur(b, parcalar);
        if (birlesikUygula) birlesikDoldur(b, satirlar);
        satirlar = bosluklariAt(satirlar);
        StringBuilder bas = new StringBuilder();
        int tasinan = 0;
        while (satirlar.size() > 2 && tasinan < 3) {
            List<String> r0 = satirlar.get(0);
            int dolu = 0;
            for (String h : r0) if (h != null && !h.isEmpty()) dolu++;
            String son = r0.get(r0.size() - 1);
            boolean fiyatYok = (son == null || son.isEmpty());
            if (dolu == 1 || fiyatYok) {
                for (String h : r0) if (h != null && !h.isEmpty()) { if (bas.length() > 0) bas.append(" "); bas.append(h); }
                satirlar.remove(0); tasinan++;
            } else break;
        }
        satirlar = bosluklariAt(satirlar);
        int sonSat = satirlar.size();
        int sonSut = sonSat > 0 ? satirlar.get(0).size() : 0;
        if (sonSat < 2 || sonSut < 2) return null;
        String baslik = bas.toString().trim();
        if (baslik.isEmpty()) baslik = baslikBul(b, parcalar);
        return new Tablo(sayfaYazdir, no, baslik, satirlar);
    }

    static void ozet(String dosya, List<Tablo> tablolar) {
        System.out.println("=== " + dosya + " | tablo sayisi: " + tablolar.size() + " ===");
        for (Tablo t : tablolar) {
            int sat = t.satirlar.size();
            int sut = sat > 0 ? t.satirlar.get(0).size() : 0;
            String bas = t.baslik == null ? "" : t.baslik;
            if (bas.length() > 45) bas = bas.substring(0, 45) + "...";
            System.out.println("  Sayfa " + t.sayfa + ", Tablo " + t.tabloNo + ": "
                    + sat + " satir x " + sut + " sutun  | " + bas);        }
    }

    public static void main(String[] args) throws Exception {
        String dosya = args.length > 0 ? args[0]
                : "6-2020-yili-ustyapi-isleri-sogutma-tesisati-ve-monoray-aksamina-ait-birim-fiyatlar.pdf";        PDDocument belge = PDDocument.load(new File(dosya));
        List<Tablo> tablolar = new ArrayList<Tablo>();
        try {
            KelimeOkuyucu ko = new KelimeOkuyucu();
            ko.setSortByPosition(true);
            ko.getText(belge);
            List pages = belge.getDocumentCatalog().getAllPages();

            if (ko.parcalar.isEmpty()) {
                // ===== TARANMIŞ PDF -> OCR =====
                System.out.println("Metin yok -> TARANMIŞ PDF, OCR calisiyor (best-effort)...");
                for (int i = 0; i < pages.size(); i++) {
                    int sayfaNo = i + 1;
                    PDPage sayfa = (PDPage) pages.get(i);
                    BufferedImage img = sayfa.convertToImage(BufferedImage.TYPE_INT_RGB, 300);
                    img = OcrOkuyucu.onIsle(img);
                    List<Parca> parcalar = OcrOkuyucu.oku(img, sayfaNo);
                    if (parcalar.isEmpty()) continue;
                    CizgiTespit ct = new CizgiTespit();
                    ct.bul(img, 170, 300, 400);
                    Bolge b = new Bolge();
                    b.sutunX = sutunSinirlari(ct.dikey, 15);
                    b.satirY = ocrSatirSinirlari(parcalar, b.sutunX, 25);                    b.yatay = new ArrayList<float[]>();
                    if (b.sutunX.size() < 2 || b.satirY.size() < 2) continue;
                    Tablo t = tabloYap(b, parcalar, sayfaNo - SAYFA_OFSET, 1, false);
                    if (t != null) tablolar.add(t);
                }
            } else {
                // ===== DİJİTAL PDF =====
                java.util.Map<Integer, List<Parca>> metin = new java.util.HashMap<Integer, List<Parca>>();
                for (Parca p : ko.parcalar) {
                    if (!metin.containsKey(p.sayfa)) metin.put(p.sayfa, new ArrayList<Parca>());
                    metin.get(p.sayfa).add(p);
                }
                CizgiOkuyucu co = new CizgiOkuyucu();
                for (int i = 0; i < pages.size(); i++) {
                    int sayfaNo = i + 1;
                    co.sayfayiIsle((PDPage) pages.get(i));
                    List<Bolge> bolgeler = tablolariBul(yataylar(co.cizgiler), dikeyler(co.cizgiler));
                    List<Parca> sayfaMetni = metin.get(sayfaNo);
                    if (sayfaMetni == null) sayfaMetni = new ArrayList<Parca>();
                    int no = 1;
                    for (Bolge b : bolgeler) {
                        Tablo t = tabloYap(b, sayfaMetni, sayfaNo - SAYFA_OFSET, no, true);
                        if (t != null) { tablolar.add(t); no++; }
                    }
                }
            }
        } finally {
            belge.close();
        }
        String ciktiAd = dosya.replaceAll("(?i)\\.pdf$", "") + ".json";
        JsonYazici.yaz(new File(ciktiAd), tablolar);
        System.out.println("Yazildi: " + ciktiAd);
        ozet(dosya, tablolar);
    }
}
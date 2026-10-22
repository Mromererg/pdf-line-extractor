package org.example;

import org.example.model.Tablo;
import org.example.pdf.KelimeOkuyucu.Parca;

import java.util.*;

public class TabloCikarici {

    static final int SATIR_TOLERANS = 6;
    static final int TABLO_BOSLUK   = 22;
    static final int SUTUN_TOLERANS = 20;

    static class Satir {
        int y;
        List<Parca> parcalar;
        Satir(int y, List<Parca> parcalar) { this.y = y; this.parcalar = parcalar; }
    }

    public static List<Tablo> cikar(List<Parca> parcalar) {
        Map<Integer, List<Parca>> sayfaMap = new TreeMap<Integer, List<Parca>>();
        for (Parca p : parcalar) {
            if (!sayfaMap.containsKey(p.sayfa)) sayfaMap.put(p.sayfa, new ArrayList<Parca>());
            sayfaMap.get(p.sayfa).add(p);
        }

        List<Tablo> tablolar = new ArrayList<Tablo>();
        for (Map.Entry<Integer, List<Parca>> e : sayfaMap.entrySet()) {
            int sayfa = e.getKey();
            List<Satir> satirlar = satirlaraBol(e.getValue());

            // y boşluğuna göre tablolara böl
            List<List<Satir>> gruplar = new ArrayList<List<Satir>>();
            List<Satir> aktif = new ArrayList<Satir>();
            int oncekiY = Integer.MIN_VALUE;
            for (Satir s : satirlar) {
                if (oncekiY != Integer.MIN_VALUE && (s.y - oncekiY) > TABLO_BOSLUK && !aktif.isEmpty()) {
                    gruplar.add(aktif);
                    aktif = new ArrayList<Satir>();
                }
                aktif.add(s);
                oncekiY = s.y;
            }
            if (!aktif.isEmpty()) gruplar.add(aktif);

            int no = 1;
            for (List<Satir> grup : gruplar) {
                tablolar.add(new Tablo(sayfa, no++, "", sutunlaraHizala(grup)));            }
        }
        return tablolar;
    }

    static List<Satir> satirlaraBol(List<Parca> liste) {
        Collections.sort(liste, new Comparator<Parca>() {
            public int compare(Parca a, Parca b) {
                if (a.y != b.y) return Integer.compare(a.y, b.y);
                return Integer.compare(a.x, b.x);
            }
        });
        List<Satir> satirlar = new ArrayList<Satir>();
        List<Parca> aktif = new ArrayList<Parca>();
        int aktifY = 0;
        for (Parca p : liste) {
            if (aktif.isEmpty()) { aktifY = p.y; aktif.add(p); }
            else if (Math.abs(p.y - aktifY) <= SATIR_TOLERANS) aktif.add(p);
            else { satirlar.add(new Satir(aktifY, aktif)); aktif = new ArrayList<Parca>(); aktifY = p.y; aktif.add(p); }
        }
        if (!aktif.isEmpty()) satirlar.add(new Satir(aktifY, aktif));
        return satirlar;
    }

    static List<List<String>> sutunlaraHizala(List<Satir> grup) {
        // 1) sütun merkezlerini bul (x kümeleme)
        List<Integer> xler = new ArrayList<Integer>();
        for (Satir s : grup) for (Parca p : s.parcalar) xler.add(p.x);
        Collections.sort(xler);

        List<Integer> merkezler = new ArrayList<Integer>();
        int toplam = 0, adet = 0, sonX = Integer.MIN_VALUE;
        for (int x : xler) {
            if (sonX == Integer.MIN_VALUE || x - sonX <= SUTUN_TOLERANS) { toplam += x; adet++; }
            else { merkezler.add(toplam / adet); toplam = x; adet = 1; }
            sonX = x;
        }
        if (adet > 0) merkezler.add(toplam / adet);

        // 2) her satırı sütunlara yerleştir
        List<List<String>> izgara = new ArrayList<List<String>>();
        for (Satir s : grup) {
            String[] hucreler = new String[merkezler.size()];
            Arrays.fill(hucreler, "");
            for (Parca p : s.parcalar) {
                int sut = enYakinSutun(merkezler, p.x);
                hucreler[sut] = hucreler[sut].isEmpty() ? p.metin : hucreler[sut] + " " + p.metin;
            }
            izgara.add(Arrays.asList(hucreler));
        }
        return izgara;
    }

    static int enYakinSutun(List<Integer> merkezler, int x) {
        int en = 0, enFark = Integer.MAX_VALUE;
        for (int i = 0; i < merkezler.size(); i++) {
            int fark = Math.abs(merkezler.get(i) - x);
            if (fark < enFark) { enFark = fark; en = i; }
        }
        return en;
    }
}
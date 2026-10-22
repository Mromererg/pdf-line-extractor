package org.example.model;

import java.util.List;

public class Tablo {
    public int sayfa;
    public int tabloNo;
    public String baslik;
    public List<List<String>> satirlar;

    public Tablo() {}

    public Tablo(int sayfa, int tabloNo, String baslik, List<List<String>> satirlar) {
        this.sayfa = sayfa;
        this.tabloNo = tabloNo;
        this.baslik = baslik;
        this.satirlar = satirlar;
    }
}
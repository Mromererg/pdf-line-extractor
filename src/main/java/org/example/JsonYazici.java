package org.example;

import org.example.model.Tablo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class JsonYazici {

    public static void yaz(File dosya, List<Tablo> tablolar) throws IOException {
        BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(dosya), "UTF-8"));
        try {
            w.write("[\n");
            for (int i = 0; i < tablolar.size(); i++) {
                Tablo t = tablolar.get(i);
                w.write("  {\n");
                w.write("    \"sayfa\": " + t.sayfa + ",\n");
                w.write("    \"tabloNo\": " + t.tabloNo + ",\n");
                w.write("    \"baslik\": \"" + kacis(t.baslik) + "\",\n");
                w.write("    \"satirlar\": [\n");
                for (int r = 0; r < t.satirlar.size(); r++) {
                    List<String> satir = t.satirlar.get(r);
                    w.write("      [");
                    for (int c = 0; c < satir.size(); c++) {
                        w.write("\"" + kacis(satir.get(c)) + "\"");
                        if (c < satir.size() - 1) w.write(", ");
                    }
                    w.write("]");
                    if (r < t.satirlar.size() - 1) w.write(",");
                    w.write("\n");
                }
                w.write("    ]\n");
                w.write("  }");
                if (i < tablolar.size() - 1) w.write(",");
                w.write("\n");
            }
            w.write("]\n");
        } finally {
            w.close();
        }
    }

    static String kacis(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
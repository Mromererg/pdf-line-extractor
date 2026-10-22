# pdf-line-extractor[README.md](https://github.com/user-attachments/files/33830881/README.md)
# PDF Birim Fiyat Cetveli Çıkarıcı

Türkiye Cumhuriyeti kurumlarına ait "birim fiyat cetveli" PDF'lerindeki
tabloları (başlık + satırlar) çıkarıp JSON olarak kaydeder.

Program, PDF'in **dijital mi yoksa taranmış mı** olduğunu otomatik anlar:

- **Dijital PDF** → metin ve çizgiler doğrudan okunur.
- **Taranmış PDF** (metin yok) → otomatik olarak **OCR**'a düşer
  (Tesseract ile), sonra aynı tablo mantığı uygulanır.

Çıktı, her tabloyu `sayfa` + `tabloNo` ile bulunabilecek şekilde JSON'a yazar.

---

## Gereksinimler

| Gereksinim | Not |
|------------|-----|
| **Java 7+** | Kod Java 1.7'ye derlenir; Java 7 ve üstü her sürümde çalışır. |
| **Maven** | Bağımlılıkları indirir ve projeyi derler. |
| **PDFBox 1.8.16** | Maven otomatik indirir (fontbox, jempbox, commons-logging ile birlikte). |
| **Tesseract OCR** | *Yalnızca taranmış PDF'ler için gerekli.* Dijital PDF'lerde gerekmez. |

### Tesseract kurulumu (sadece taranmış PDF'ler için)

- **Windows:** Tesseract installer'ı kurun, ardından `tur.traineddata`
  dosyasını `C:\Program Files\Tesseract-OCR\tessdata\` içine koyun.
- **Linux:** `sudo apt install tesseract-ocr tesseract-ocr-tur`
- **macOS:** `brew install tesseract tesseract-lang`

Doğrulama:

```
tesseract --list-langs
```

Listede `tur` görünmelidir.

---

## Derleme

```
mvn clean package
```

Derlenmiş sınıflar `target/` altına üretilir.

---

## Çalıştırma

İşlenecek PDF'i proje klasörüne koyun ve adını argüman olarak verin:

```
mvn exec:java -Dexec.mainClass=org.example.Main -Dexec.args="dosya.pdf"
```

veya derlenmiş haliyle:

```
# Linux / macOS
java -cp "target/classes:LIB/*" org.example.Main dosya.pdf

# Windows (classpath ayıracı ; )
java -cp "target/classes;LIB/*" org.example.Main dosya.pdf
```

- Argüman **verilirse** → o PDF işlenir.
- Argüman **verilmezse** → varsayılan örnek PDF işlenir.
- PDF başka klasördeyse adı yerine **tam yolunu** verin.

### Tesseract yolu (taranmış PDF'lerde)

Program varsayılan olarak PATH'teki `tesseract` komutunu arar.

- **Linux / macOS:** kurulumla PATH'e eklendiği için ek bir şey gerekmez.
- **Windows:** Tesseract PATH'te değilse yolu belirtin:

  ```
  java -Dtesseract="C:\Program Files\Tesseract-OCR\tesseract.exe" -cp "..." org.example.Main dosya.pdf
  ```

  veya `TESSERACT_PATH` ortam değişkenini ayarlayın.

---

## Çıktı

Program, girdi PDF'iyle aynı adda bir `.json` üretir:

```
dosya.pdf  ->  dosya.json
```

JSON yapısı:

```json
[
  {
    "sayfa": 4,
    "tabloNo": 1,
    "baslik": "...",
    "satirlar": [
      ["POZ NO", "YENİ POZ NO", "İŞİN ADI", "...", "..."],
      ["007.101", "44.100.1551", "12,5 kg'lık kalıplar için", "457,00", "51,00"]
    ]
  }
]
```

---

## Teknik Notlar

- **Saf Java 7.** Java 8+ API kullanılmaz. JSON elle yazılır (Jackson vb. yok).
- OCR, **Tesseract'ın komut satırı arayüzü** `Runtime/ProcessBuilder` ile
  çağrılarak yapılır (Tess4J gibi Java 8 gerektiren kütüphaneler kullanılmaz).
- Taranmış PDF'lerde sayfa 300 DPI render edilir, gri tonlama + kontrast
  germe ön işlemeden geçirilir (soluk/renkli tablo yazılarını OCR'a hazırlar).
- Platform bağımsız: Tesseract yolu PATH/property'den, geçici dosyalar
  sistemin temp dizininden, girdi dosyası argümandan okunur.

## Bilinen Sınırlar

- Taranmış PDF sonuçları "best-effort"tür (~%85–90); OCR karakter hataları
  olabilir (ör. `Fr-12` → `Fr-42`).
- Bir sayfada birden çok tablo varsa taranmış modda yalnızca ilki alınır.

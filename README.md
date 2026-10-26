<div align="center">

# 🌤️ HavaBot

**Şehir arama, 15 günlük hava tahmini ve sohbet eden bir hava robotu**

Kotlin · Jetpack Compose · Material 3 · MVVM · Open-Meteo

[github.com/SenaDemirci/GK-weather-app](https://github.com/SenaDemirci/GK-weather-app)

```bash
git clone https://github.com/SenaDemirci/GK-weather-app.git
```

</div>

---

HavaBot, Bilişim Kampı Android eğitiminin referans projesi üzerine geliştirildi. Kurs projesi şehir arayıp anlık durumu, saatlik ve 7 günlük tahmini gösteriyordu. Üzerine yeni tasarım, iki dil, harita, polen, UV, kıyafet önerisi ve çevrimdışı çalışan bir hava robotu eklendi. Bütün veriler anahtar gerektirmeyen [Open-Meteo](https://open-meteo.com) servislerinden gelir.

## ✨ Özellikler

### 🔎 Şehir arama
- Dünyanın her yerinden şehir arama; sonuçlar bayrak, ülke ve bölgeyle listelenir.
- 🌍 **Ülke filtresi:** sonuçlar ülke çiplerine göre süzülür.
- 🕘 **Son bakılanlar:** tahminine bakılan son 8 şehir yatay bir listede durur, tek dokunuşla tekrar açılır. Uygulama kapansa da hatırlanır.
- 🔄 Arama metni, seçili ülke ve diğer ekran durumları ekran döndürülünce kaybolmaz (`rememberSaveable`).
- ⏳ Yükleniyor (shimmer), veri, boş ve hata durumlarının hepsi ele alınır; hatada "Tekrar dene" düğmesi çıkar.

### 🌦️ Hava tahmini ekranı
- 🎬 **Canlı hava animasyonları:** havaya göre renklenen ana kartın arkasında güneş, bulut, yağmur, kar, şimşek ve sis çizilir.
- 💬 **Ayrıntılı ifadeler:** "Hafif sağanak yağışlı", "Yoğun kar yağışlı" gibi her WMO hava koduna özel açıklamalar.
- 🌡️ Hissedilen sıcaklık, nem, rüzgâr ve "Gün içinde" en düşük / en yüksek değerler.
- ⏰ **Saatlik tahmin:** şehrin **yerel saatine** göre "Şimdi" ve ertesi güne düşen kartlarda "Yarın" etiketi. Saat aralığı filtresiyle (Sabah, Öğle, Akşam, Gece) yalnızca istenen saatler gösterilir.
- 📅 **7 / 15 günlük tahmin:** "Günlük Tahmin" başlığı altındaki sekmelerle ikisi arasında geçilir; her gün için en düşük ve en yüksek sıcaklık çubuğu ile yağış olasılığı gösterilir.
- 👕 **Ne giymeli?:** sıcaklık, yağmur, rüzgâr ve UV'ye göre kıyafet önerileri ve "Şemsiyeni al ☂️" gibi ipuçları.
- ☀️ **Güneş ve UV:** gün doğumu, gün batımı, gün uzunluğu ve renkli UV göstergesi (düşük → aşırı).
- 🌼 **Polen ve alerji:** 6 polen türü (çimen, huş, kızılağaç, zeytin, pelin, ambrosia) için genel uyarı ve 4 günlük renkli tablo. Veri yalnızca Avrupa ve Türkiye'yi kapsar.

### 🗺️ Türkiye sıcaklık haritası
- 81 il, Türkiye'nin harita şeklinin içinde noktalar olarak çizilir ve bugünkü ortalama sıcaklığına göre renklenir.
- 🤏 Yakınlaştırma, kaydırma ve ile dokunma. Seçilen ilin bilgisi kartta çıkar; karttan o ilin tahminine geçilir.
- 81 ilin verisi tek istekte alınır.

### 🤖 Hava Robotu
- **Ücretsiz ve çevrimdışı:** dil modeli kullanmaz; kurallara dayalı çalışır, internet yalnızca tahmin verisi için gerekir.
- Uygulama açılınca robot hafifçe büyür ve konuşma balonuyla kendini tanıtır; dokununca sohbet paneli açılır.
- 🌧️ Yağmur, 🌡️ sıcaklık, 👕 kıyafet, 🧺 piknik / dışarı çıkma, 🏆 haftanın en güzel günü, 🧴 UV ve 🌅 gün doğumu / batımı sorularını yanıtlar.
- 📍 **Sorudaki şehri kendisi bulur:** "İzmir'de yarın yağmur var mı?", "ankarada hava nasıl". Şehir yazılmazsa açık olan şehri, o da yoksa en son bakılan şehri kullanır.
- 📆 "bugün", "yarın", "hafta sonu" ve gün adlarını anlar; "nsıl", "hva" gibi yazım hatalarını şehir sanmaz.
- 👋 Selamlaşma ve teşekkürlere de cevap verir; Türkçe ve İngilizce soruları anlar.

### 🎨 Tasarım ve ayarlar
- ☁️ Bütün ekranlarda ortak gökyüzü arka planı, beyaz kartlar, büyük ve kalın yazılar.
- 🌗 **Açık / koyu / sistem teması** seçimi; sistem çubukları da temaya uyar.
- 🇹🇷 🇬🇧 **Türkçe ve İngilizce:** uygulama içinden dil seçimi, ilk açılışta Türkçe. Şehir adları da seçilen dilde aranır.
- 🖼️ Renkli robot logosu ve uygulama adı **HavaBot**.

## 🏗️ Mimari

Tek `:app` modülü, **MVVM** ve üç katman: `model/` · `data/` · `ui/`. Ayrıntılar için [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

```
┌──────────────────────┐   olay    ┌──────────────────────┐  suspend  ┌──────────────────────┐   HTTP   ┌─────────────┐
│ UI (Composable)      │ ───────▶ │ ViewModel            │ ───────▶ │ Repository           │ ───────▶ │ Open-Meteo  │
│ yalnızca state çizer │ ◀─────── │ StateFlow<LoadState> │ ◀─────── │ Retrofit + Gson      │ ◀─────── │ (4 servis)  │
└──────────────────────┘   state   └──────────────────────┘   model   └──────────────────────┘   JSON   └─────────────┘
                                             │
                                             ▼
                               model/ : saf Kotlin kurallar
                     (robot, kıyafet, UV, polen seviyesi, şehir bulma)
```

**Kurallar**
- 🖌️ **UI internete gitmez.** Composable aldığı state'i çizer, olayları yukarı bildirir.
- 🧠 **ViewModel çizim yapmaz.** Renk ya da Composable bilmez; yalnızca state üretir.
- 📦 **Repository ekranı bilmez.** Veriyi getirir ve modele çevirir.
- 🧮 **Kurallar `model/` içindedir.** Robot, kıyafet önerisi, UV ve polen seviyeleri Android'e bağlı olmayan saf Kotlin'dir; bu yüzden kolayca test edilir.
- 🌐 **Model metin üretmez.** Cevaplar ve öneriler `@StringRes` kimlikleri ve argümanlarla döner; metin UI'da seçili dilde kurulur.

**Durum ve saklama**

| Ne | Nasıl |
|---|---|
| Ekran verisi | `StateFlow<LoadState<T>>`: `Loading` · `Success` · `Empty` · `Error(@StringRes)` |
| Ekran dönünce korunan seçimler | `rememberSaveable` (arama metni, ülke, saat aralığı, 7/15 gün, haritada seçili il) |
| Tema ve dil | `SharedPreferences` → `AppPreferences` |
| Son bakılan şehirler | `SharedPreferences` + Gson JSON → `SharedPreferencesRecentCities` |
| Dil değişimi | `attachBaseContext` içinde `AppLanguage.wrap()` + `recreate()` |

**Ekranlar ve gezinme** (Navigation Compose)

```
city ──────────────▶ forecast/{şehir}
 │                       ▲
 └──▶ map ───────────────┘          + her ekrandan açılan Hava Robotu paneli
```

### 📁 Paket yapısı

```
com.kampplus.hava
├── MainActivity.kt                 Tema, dil ve sistem çubukları; NavHost'u başlatır
│
├── model/                          Veri neye benzer? Kurallar neler?
│   ├── City.kt                     City + arama yanıtı
│   ├── Forecast.kt                 Forecast · Current / Hourly / Daily (UV, gün doğumu / batımı)
│   ├── WeatherCondition.kt         WMO kodu → emoji, kısa ve ayrıntılı etiket
│   ├── ClothingAdvice.kt           "Ne giymeli?" kuralları ve ipuçları
│   ├── UvLevel.kt                  UV indeksi → seviye
│   ├── Pollen.kt                   Polen türleri, eşikler, seviyeler
│   ├── HourRange.kt                Sabah / Öğle / Akşam / Gece filtresi
│   ├── CountryFilter.kt            Arama sonuçlarından ülke listesi ve süzme
│   ├── RecentCities.kt             Son bakılanlar listesi kuralları (en fazla 8, tekrarsız)
│   ├── Province.kt                 81 il ve koordinatları
│   ├── TurkeyOutline.kt            Türkiye ve göllerin sınır çokgenleri
│   ├── WeatherAssistant.kt         Robot: konu ve gün bulma, cevap üretme
│   └── CityMention.kt              Robot: sorudaki şehir adayını bulma
│
├── data/                           Veri nereden gelir?
│   ├── ApiClient.kt                Retrofit kurulumu (geocoding · forecast · air-quality)
│   ├── CityApi.kt                  @GET v1/search
│   ├── ForecastApi.kt              @GET v1/forecast (15 gün; 81 il için çoklu konum)
│   ├── ForecastResponse.kt         DTO'lar + sütun → satır çevrimi
│   ├── AirQualityApi.kt            @GET v1/air-quality (polen)
│   ├── PollenResponse.kt           Polen DTO → PollenForecast
│   ├── CityRepository.kt           Şehir arama (seçili dilde)
│   ├── WeatherRepository.kt        Şehir tahmini
│   ├── PollenRepository.kt         Polen tahmini
│   ├── ProvinceTemperatureRepository.kt   81 ilin bugünkü ortalama sıcaklığı
│   ├── RecentCitiesRepository.kt   Son bakılanlar (arayüz + SharedPreferences)
│   └── AppPreferences.kt           Tema ve dil tercihi
│
└── ui/                             Ne çizilir, ne zaman değişir?
    ├── LoadState.kt                Loading / Success / Empty / Error
    ├── HavaNavHost.kt              city · map · forecast rotaları
    ├── city/                       CityScreen · CityViewModel · CityCard
    ├── forecast/                   ForecastScreen · ForecastViewModel · WeatherAnimation · PollenSection
    ├── map/                        MapScreen · MapViewModel · TurkeyDotMap (Canvas)
    ├── assistant/                  AssistantSheet · AssistantViewModel · RobotMascot
    ├── component/                  ShimmerList · EmptyView · ErrorView
    ├── locale/                     AppLanguage (dil seçimi)
    └── theme/                      Color · Theme · Type · Shape · ThemeMode · SkyBackground · WeatherColors
```

### 🔁 Veri akışları

- **Arama:** `CityScreen` → `CityViewModel.search()` → `CityRepository` → geocoding → `LoadState<List<City>>` → ülke filtresi UI'da uygulanır.
- **Tahmin:** şehre dokunulur → `HavaNavHost` şehri son bakılanlara ekler → `ForecastViewModel` tahmini ve polen verisini **ayrı** `LoadState`'lerle yükler; polen gelmese de tahmin gösterilir.
- **Harita:** `MapViewModel` → `ProvinceTemperatureRepository` 81 ilin günlük ortalamasını tek istekte alır → `TurkeyDotMap` sıcaklık renk skalasıyla çizer → ile dokununca tahmine geçilir.
- **Robot:** soru → `CityMention` şehir adaylarını çıkarır → `AssistantViewModel` adayları arar, bulamazsa açık ya da son bakılan şehre düşer → tahmini alır → `WeatherAssistant.reply()` `@StringRes` satırlarıyla cevap kurar → `AssistantSheet` seçili dilde yazar.

## 🌐 API: Open-Meteo (anahtar gerekmez)

| Amaç | Çağrı |
|---|---|
| Şehir arama | `geocoding-api.open-meteo.com/v1/search?name=Ankara&count=10&language=tr` |
| Tahmin | `api.open-meteo.com/v1/forecast?latitude=..&longitude=..&current=..,uv_index&hourly=..&daily=..,sunrise,sunset,uv_index_max&forecast_days=15&timezone=auto` |
| 81 il sıcaklığı | `api.open-meteo.com/v1/forecast?latitude=39.9,41.0,…&longitude=32.8,28.9,…&daily=temperature_2m_mean&forecast_days=1` |
| Polen | `air-quality-api.open-meteo.com/v1/air-quality?latitude=..&longitude=..&hourly=alder_pollen,birch_pollen,…&forecast_days=4` |

- Sonuç yoksa arama yanıtında `results` alanı **hiç gelmez** → Boş (Empty) durumu.
- Tahmin yanıtı **sütun** biçimindedir: `hourly.time[i]` ile `hourly.temperature_2m[i]` aynı saate aittir.
- `weather_code` WMO standardındadır (0 açık, 61 yağmur, 95 fırtına…); `WeatherCondition` ile görünüme çevrilir.
- Polen verisi CAMS Avrupa modelinden gelir; Avrupa dışındaki şehirlerde polen kartı görünmez.
- Ücretsiz kullanım ticari olmayan projeler içindir (~10.000 istek/gün).

## ▶️ Çalıştırma

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit testler
./gradlew spotlessApply        # ktlint formatı (denetim için spotlessCheck)
./gradlew assembleRelease      # release APK (R8 açık)
```

Android SDK yolunu `local.properties` içinde `sdk.dir=...` ile veya `ANDROID_HOME` ortam değişkeniyle verin. En düşük Android sürümü: **8.0 (API 26)**.

## 🧪 Testler

20 test sınıfı; ağ testleri `MockWebServer`, ViewModel testleri sahte (fake) repository'lerle çalışır.

| Katman | Test | Neyi korur |
|---|---|---|
| data | `CityApiTest` | Gson `@SerializedName` eşlemesi |
| data | `ForecastApiTest` | Sütun → satır çevrimi, UV ve gün doğumu / batımı |
| data | `PollenApiTest` | Polen yanıtının günlük en yüksek değerlere çevrimi |
| data | `ProvinceTemperatureApiTest` | 81 il için çoklu konum yanıtı |
| model | `WeatherConditionTest` | WMO kodu → emoji ve etiketler |
| model | `ClothingAdviceTest` | Kıyafet önerisi kuralları |
| model | `UvLevelTest` | UV seviyeleri |
| model | `PollenTest` | Polen eşikleri ve seviyeleri |
| model | `HourRangeTest` | Saat aralığı filtresi |
| model | `CountryFilterTest` | Ülke listesi ve süzme |
| model | `RecentCitiesTest` | Son bakılanların sırası, tekrarı ve sınırı |
| model | `ProvinceTest` | 81 ilin eksiksiz olması ve harita sınırları içinde kalması |
| model | `WeatherAssistantTest` | Robotun konu, gün ve cevap seçimi; önerilen soruların iki dilde anlaşılması |
| model | `CityMentionTest` | Sorudaki şehrin bulunması, yazım hatalarının elenmesi |
| ui | `CityViewModelTest` | Dört durum + "Tekrar dene" |
| ui | `ForecastViewModelTest` | Rota parametreleri, tahmin ve polen durumları |
| ui | `MapViewModelTest` | 81 ilin yüklenmesi, boş ve hata durumları |
| ui | `AssistantViewModelTest` | Sorudaki şehrin aranması, yanlış eşleşmenin reddi, son bakılan şehre düşme, hata mesajı |
| ui | `TemperatureColorTest` | Sıcaklık renk skalası |
| ui | `ThemeModeTest` | Sistem / açık / koyu seçiminin uygulanması |

## 🧭 Checkpoint'ler

Kurs sırasında her ders bloğunun sonunda projenin olması gereken hâli ayrı bir branch'tedir. `cpN` = **Uygulama N+1**.

| Branch | Karşılığı | İçerik |
|---|---|---|
| `cp1-baslangic` | — | Boş Empty Activity projesi, "Merhaba Kamp!" |
| `cp1-bitis` / `cp2-baslangic` | Uygulama 2 | Sabit şehir listesi, `CityCard` + plaka rozeti, `LazyColumn`, tıklamada `Log.d` |
| `cp2-bitis` / `cp3-baslangic` | Uygulama 3 | Arama kutusu, `rememberSaveable`, filtre, boş durum, `CityViewModel`, `model/data/ui` paketleri |
| `cp3-bitis` / `cp4-baslangic` | Uygulama 4 | Retrofit + Gson, `ApiClient`, `CityRepository`, dört durum, Shimmer, `try/catch` |
| `cp4-bitis` | Uygulama 5 | Tahmin ekranı + navigasyon, cila, testler |

`archive/v1-clean-arch` branch'i, projenin önceki Clean Architecture + Hilt + Room sürümünü saklar.

## 🔐 Release imzası

`keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp doldurun (repoya girmez). Dosya yoksa release APK debug anahtarıyla imzalanır.

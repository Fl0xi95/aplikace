OVLADAČ LED PÁSKU TEST – WLED

Projekt Android aplikace pro Samsung / Android, ovládá WLED přes jeho JSON API.
Funkce: barva RGB, jas, základní efekty, zapnutí/vypnutí a místně uložené scény.

Výchozí adresa WLED v režimu Access Point bývá http://4.3.2.1 . Pokud máš WLED připojené k domácí Wi‑Fi, zadej IP adresu zařízení (např. http://192.168.1.50). Mobil musí být ve stejné síti.

Jak získat instalační APK bez instalace Android Studia:
1. Nahraj celý obsah ZIPu do nového GitHub repository.
2. Otevři v repository Actions a spusť „Build Android APK“ (nebo počkej na dokončení běhu po nahrání souborů).
3. V dokončeném běhu stáhni artifact „Ovladac-LED-TEST-APK“. Rozbal ZIP a v něm bude app-debug.apk.
4. Přenes APK do Samsungu a otevři ho. Android může chtít povolit instalaci z daného zdroje.

Poznámka: v tomto prostředí není Android SDK/Gradle cache ani přístup k internetu, takže zde nemohu APK skutečně zkompilovat a ověřit. GitHub Actions ho sestaví v čistém Android build prostředí.

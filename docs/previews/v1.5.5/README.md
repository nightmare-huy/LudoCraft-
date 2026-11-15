# 1.5.5 — рукоять немного глубже в ладони

В 1.5.4 уже совпадали угол, размер и боковое положение с Punchy-эталоном алмазной кирки. По просьбе пользователя добавлен небольшой сдвиг вниз на **0,5 модельного пикселя** для более глубокого хвата. Это first-person display transform обеих рук; Punchy item profile, third-person pose, геометрия, текстура и gameplay не менялись.

## Проверка

- Minecraft Java 1.21.1, NeoForge 21.1.265, Punchy 3.2b, стандартные настройки.
- Реальные игровые кадры обеих рук: сравнение с 1.5.4, взгляд вниз, F5, взмах, добыча в выживании и возврат в покой.
- Тестовый клиент проверяет разрешённый Punchy-профиль и first-person transforms для обеих рук.
- Отдельно проверены vanilla-клиент без Punchy, модель/texture, 8+2 GameTests, правила и JAR.
- Не проверялись ручной полный FTB Evolution, шейдеры, пользовательские конфиги Punchy и все фазы всех анимаций.

Установите новый JAR на клиент и сервер и перезапустите игру. Оставьте Punchy и datapack Evolution как есть; отдельный resource pack не нужен. Шансы кирки 85/15, награды/эффекты, Pocket Ludka и balance preset не менялись.

- [Скачать JAR 1.5.5](https://github.com/nightmare-huy/LudoCraft-/releases/download/v1.5.5/ludocraft-1.5.5.jar)
- [Release и SHA256SUMS](https://github.com/nightmare-huy/LudoCraft-/releases/tag/v1.5.5)
- Исходные игровые кадры и provenance: `punchy/`, `punchy/capture.json`.

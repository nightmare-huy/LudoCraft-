# 1.5.4 — поза алмазной кирки для LudoCraft в Punchy

**Кадры в `punchy/` и сравнительный лист — реальные снимки автоматизированного клиента Minecraft с Punchy 3.2b.** Это не программные рендеры и не ручная игра в FTB Evolution.

## Что изменено

Для первого лица кирка Лудомании теперь использует фактические трансформы обычной алмазной кирки, считанные из работающего клиента Punchy 3.2b. Item-specific профиль Punchy для LudoCraft соответствует стандартному профилю pickaxe: масштаб 1, все смещения/вращения/pivot 0. Тест клиента сравнивает разрешённые профили и baked item display transforms обеих рук с ванильным `minecraft:diamond_pickaxe`.

Сама 3D-модель, её текстура, геометрия (241 quad), третье лицо и игровой код не менялись. Положение профиля ограничено только `ludocraft:gamblers_pickaxe`; глобальные руки и анимации Punchy не затрагиваются.

## Проверка

- Minecraft Java **1.21.1**, NeoForge **21.1.265**, Punchy **3.2b**, свежий стандартный конфиг.
- Runtime assertion: фактические first-person transforms и Punchy profiles правой/левой рук совпадают с diamond-pickaxe reference.
- Реальные игровые кадры обеих рук, взгляда вниз, F5, взмаха, добычи блока в выживании и возврата в покой. Успешные маркеры: `PUNCHY_COMPAT_PASS`, `PUNCHY_MINING_PASS`, `HAND_POSE_WORLD_PASS`.
- Отдельно проходит клиент без Punchy, геометрия модели, чистые механики, 8+2 GameTests, проверка JSON и содержимого JAR.
- Установка: заменить старый LudoCraft JAR на клиенте и сервере и перезапустить. Punchy и Evolution datapack менять не нужно; отдельный resource pack не требуется.

**Границы:** автоматическая test-world сцена, а не ручное прохождение FTB Evolution. Шейдеры, пользовательские конфиги Punchy, произвольный FOV и все фазы всех анимаций не проверялись. Механика кирки, шансы 85/15, дроп, звуки/эффекты, Лудка и баланс Evolution неизменны.

- [JAR 1.5.4](https://github.com/nightmare-huy/LudoCraft-/releases/download/v1.5.4/ludocraft-1.5.4.jar)
- [Release, ZIP профиля Evolution и SHA256SUMS](https://github.com/nightmare-huy/LudoCraft-/releases/tag/v1.5.4)
- Исходные PNG и `capture.json` находятся в соседней папке `punchy/`; сборный лист использует исходные кадры без ретуши.

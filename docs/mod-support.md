# Поддержка модов — LudoCraft 1.4.0

Minecraft **1.21.1 / NeoForge**. Это профили оценки предметов для Карманной Лудки,
не перенос модов между версиями Minecraft и не интеграция их машинных API.
Моды необязательны: используются только registry ID и стандартные теги предметов.
Если мод не установлен, его предметы не появляются и загрузка не ломается.

## Покрытие

| Мод / namespace | Что отдельно оценено |
|---|---|
| AllTheOres / `alltheores` | Металлы и сплавы, самоцветы, слитки/самородки/пыль/сырьё/руды, блоки, пластины, стержни, шестерни, молоты |
| AllTheModium / `allthemodium` | Три редких металла, их формы и сплавы, броня/инструменты, шаблоны, alloy tools, сердце пиглича, teleport pad |
| Mekanism / `mekanism` | Материалы, сплавы, схемы, четыре уровня фабрик/кубов/ёмкостей/труб, основные машины, Digital Miner, QIO, MekaSuit, антиматерия |
| Mekanism Generators / `mekanismgenerators` | Генераторы, основные компоненты турбины и реакторов |
| Mekanism Tools / `mekanismtools` | Материальные уровни инструментов, брони, paxel и щитов |
| Ender IO / `enderio` | Сплавы, конденсаторы/банки, основные машины, генерация, обелиски, кристаллы и компоненты |
| Applied Energistics 2 / `ae2` | Кварц/Fluix, процессоры/прессы, машины, кабели, шины, терминалы, карты; ячейки/компоненты 1k–256k и spatial 2/16/128 |
| Applied Mekanistics / `appmek` | Chemical cells и portable chemical cells 1k–256k, корпус и P2P |
| MEGACells / `megacells` | Ячейки/компоненты/crafting storage 1m–256m; item/fluid/chemical/experience/mana/soul/source, mega-машины, bulk/radioactive cells |
| ExtendedAE / `extendedae` | Entro, concurrent processor, расширенные машины/шины, assembler matrix, upgrade items, void и infinity water/cobblestone cells |
| AdvancedAE / `advanced_ae` | Quantum-материалы/процессоры, машины, storage 128/256, quantum armor и карты улучшений |
| Другие моды | Общие `c:`-теги материалов и прежняя эвристика; специализированный профиль для каждой машины не гарантируется |

**Это не обещание отдельной цены абсолютно каждому ID этих модов.** Декор, новые
предметы будущих версий и неописанные устройства используют общие теги/fallback.
Распознавание ёмкости ячеек ограничено известными размерами: произвольные похожие
имена из чужих namespaces не принимаются за дорогие предметы.

## Примеры стандартных цен

| Registry ID | Цена |
|---|---:|
| `alltheores:tin_nugget` / `tin_ingot` / `tin_block` | 1 / 16 / 144 |
| `allthemodium:allthemodium_ingot` | 4096 |
| `allthemodium:vibranium_ingot` | 8192 |
| `allthemodium:unobtainium_ingot` | 16384 |
| `mekanism:alloy_infused` / `alloy_reinforced` / `alloy_atomic` | 48 / 192 / 768 |
| `mekanism:basic_control_circuit` … `ultimate_control_circuit` | 32 / 128 / 512 / 2048 |
| `mekanism:digital_miner` | 8192 |
| `mekanism:pellet_antimatter` | 32767 |
| `enderio:basic_capacitor` / `double_layer_capacitor` / `octadic_capacitor` | 64 / 256 / 1024 |
| `ae2:engineering_processor` | 192 |
| `ae2:item_storage_cell_1k` … `item_storage_cell_256k` | 384 / 640 / 1152 / 2176 / 4224 |
| `appmek:chemical_storage_cell_64k` | 2176 |
| `megacells:item_storage_cell_1m` … `item_storage_cell_256m` | 8320 / 12416 / 18560 / 24704 / 30128 |
| `extendedae:ex_pattern_provider` | 2048 |
| `advanced_ae:quantum_processor` | 1024 |
| `advanced_ae:quantum_storage_256` | 24576 |

Ячейка дороже компонента; portable-ячейка дороже обычной. Внутреннее содержимое
на оценку не влияет. Примеры шансов: земля → allthemodium **0.0122%**,
алмаз → AE2 item cell 1k **16.6666%**, allthemodium → unobtainium **12.5000%**.
Формула, диапазон цен 1…32767, максимум шанса 95% и одна вещь за попытку не изменены.

### Общие теги

Например, у любого мода `c:ingots/tin` даёт 16, `c:nuggets/tin` — 1,
`c:gears/tin` — 64, `c:storage_blocks/tin` — 144.

Поддерживаемые формы: `ingots`, `gems`, `dusts`, `plates`, `clumps`, `crystals`,
`shards`, `ores` — цена материала; `nuggets`/`tiny_dusts` — 1/9;
`rods`/`raw_materials` — 1/2; `gears` — ×4; `storage_blocks` — ×9,
`storage_blocks/raw_<material>` — ×4.5. Округление вниз, минимум 1, максимум 32767.
Это условная политика баланса, а не обещание совпадения с каждым рецептом.
Список материалов находится в начале `LudkaModValues.java`.

Приоритет: **override-теги датапака → ваниль → модовый профиль → материальные теги
(максимальное совпадение) → общий fallback**. Сервер заново рассчитывает цену и
проверяет цель; клиент не передаёт цену и не выбирает исход.

## Исключения и данные предметов

- Известные creative/debug-предметы AE2/Mekanism/Ender IO и технические
  `ae2:cable_bus`, `mekanism:bounding_block` внесены в blacklist optional-записями
  (`required:false`). Отсутствие модов не делает теги недействительными.
- `enderio:conduit` использует один ID с компонентом типа провода. Его стандартный
  экземпляр без компонента не устанавливается. `enderio:loot_capacitor` без
  индивидуальных данных имеет нулевые характеристики. Оба исключены **как ставка
  и цель**, чтобы не выдавать неработоспособные предметы. Поддержка компонентных
  вариантов проводов — отдельная будущая задача, а не скрытая возможность этого релиза.
- `extendedae:infinity_water_cell` и `infinity_cobblestone_cell` — обычные survival-
  генераторы воды/булыжника, не creative-storage; они разрешены и оценены в 2048.
- Каталог содержит один стандартный экземпляр на разрешённый item ID, а не все
  варианты NBT/компонентов. Ячейки AE2 выдаются пустыми, устройства — без переноса
  заряда, модулей, настроек или привязок ставки. Не ставьте заполненные ячейки,
  рюкзаки и прокачанные устройства: содержимое **не повышает шанс и будет потеряно
  вместе с использованным предметом даже при победе**.
- Blacklist можно переопределить датапаком, но разрешение component-only ID само
  по себе не создаёт правильные компоненты награды.

Цены не вычисляются из рецептов: обход прогрессии через рулетку остаётся возможным
по замыслу режима. Особо дорогие вещи достигают потолка 32767 и становятся равноценны.
Администратор сборки должен проверить цены, конверсии и при необходимости запретить
эндгейм-цели. Инструкции по override/blacklist — в [README](../README.md#настройка-датапаком).

## Проверки и происхождение ID

Чистые Java-тесты проверяют примеры по каждому профилю, все записи на границы,
монотонность ячеек/машин, отношения форм, неизвестные ID и шансы. Серверные GameTests
проверяют реальные `c:`-теги самородков/сырья, прежние цены инструментов и отсутствие
копирования содержимого контейнера в выигрыш, плюс прежние транзакционные проверки.

**CI запускается без сторонних модов.** Это проверка правил и серверного кода,
не успешный запуск ATM-модпака. Клиентский GUI, совместный запуск перечисленных модов,
модовые рендереры, JEI и multiplayer с реальной сборкой вручную пока не проверены.
Перед основным миром нужен тест на копии сборки: поиск по namespace, цены разных
уровней, пустые ячейки после выигрыша, blacklist, `/reload` override-тегов,
сервер/клиент с одинаковым JAR.

Сопоставлялись модели предметов, локализации и в особых случаях регистрации upstream
для веток 1.21/1.21.1 (снимок исследования 2026-11-14):

| Проект | Ветка / исследованный commit |
|---|---|
| [AllTheOres](https://github.com/AllTheMods/AllTheOres) | `1.21` / `b82085be8c8e3bc9ed3148d40514555638bac563` |
| [AllTheModium](https://github.com/AllTheMods/AllTheModium) | `1.21.x` / `b68d348df1efa129e8288eb170f04cc2543b7514` |
| [Mekanism](https://github.com/mekanism/Mekanism) | `1.21.x` / `226a625c69b299ca8f9f6e6b7a719d2ccdd1a85d` |
| [Ender IO](https://github.com/Team-EnderIO/EnderIO) | `1.21.1` / `8989fcebda3eb9e678fea4138f577c47a22a5e8d` |
| [AE2](https://github.com/AppliedEnergistics/Applied-Energistics-2) | `1.21.1` / `da9bd7f09a94272a67f9825e4c0f4f6b61983b69` |
| [MEGACells](https://github.com/62832/MEGACells) | `master` (MC 1.21.1) / `d1eaad6ceac319a90db7c459c481337212715760` |
| [ExtendedAE](https://github.com/GlodBlock/ExtendedAE) | `1.21.1-neoforge` / `eaea510c5ae8e8563324f64c6c82fdac1fff2d27` |
| [Applied Mekanistics](https://github.com/AppliedEnergistics/Applied-Mekanistics) | `dev/1.21.1` / `137f24bb9a46775ddd5a620055270b5e8a540f5a` |
| [AdvancedAE](https://github.com/pedroksl/AdvancedAE) | `forge/1.21.1` / `7a2ad36f3741b34fdb776151b0e0a11293051cf1` |

Изменения ID в будущих версиях могут потребовать обновления профиля. Отсутствующий ID
не создаётся LudoCraft; неизвестная реальная вещь всё равно получает общую оценку.

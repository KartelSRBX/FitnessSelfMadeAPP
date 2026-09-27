# Fitness Assistant (личный Android-ассистент)

Личный инструмент под Samsung Galaxy S24+/S25/S26, Z Fold6/Flip6. Не публичный продукт.
Полное описание концепции, модулей и открытых вопросов — см. приложенный к проекту
Roadmap (Plans / Work / Schedule / Health / Nutrition + мини-ИИ на Gemini Nano).

## Стек

- Kotlin, Jetpack Compose, Material 3
- minSdk 26, targetSdk/compileSdk 36
- Room (локальное хранение, без облака)
- ML Kit GenAI Prompt API поверх AICore (Gemini Nano) — ядро мини-ИИ
- Android SpeechRecognizer (офлайн-режим) — голосовой ввод
- Calendar Provider (Schedule), Health Connect (Health)

## Структура

```
app/src/main/java/com/kartel/fitness/
  core/ai/        — обёртка над Gemini Nano, разбор голосовых команд в JSON
  core/voice/      — обёртка над SpeechRecognizer, overlay-кнопка (план)
  core/data/       — общая Room-база
  feature/plans/       — модуль Plans
  feature/work/        — модуль Work (+ Bitrix24)
  feature/schedule/    — модуль Schedule (Calendar Provider)
  feature/health/      — модуль Health (Health Connect, только чтение)
  feature/nutrition/   — модуль Nutrition (USDA FoodData Central)
  feature/analytics/   — простые графики по каждому модулю
  navigation/      — граф навигации, нижняя панель
  ui/theme/        — Material 3 тема
```

## Статус

Скелет проекта: навигация между модулями работает, экраны — заглушки.
Функциональность пока не реализована ни в одном модуле.

## Порядок разработки (см. roadmap, "Инструкции и промпты")

1. Ядро мини-ИИ (`core/ai`) — **следующий шаг**
2. Голосовой ввод (`core/voice`)
3. Plans
4. Work
5. Schedule
6. Health
7. Nutrition
8. Хранение и экспорт
9. Аналитика

## Запуск

Открыть папку в Android Studio, дождаться Gradle sync. Для проверки Gemini Nano
нужен физический Samsung-телефон с поддержкой AICore, подключённый по USB
с включённой отладкой — на эмуляторе AICore недоступен.

# Meu Assistente

Aplicativo Android nativo de gestão de tarefas, lembretes, alarmes urgentes e automação local desenvolvido em **Kotlin** e **Jetpack Compose**, seguindo **Clean Architecture** e **MVVM**, com persistência em **Room (SQLite)**, agendamento preciso via **AlarmManager**, tarefas em segundo plano via **WorkManager**, **App Widget** nativo na tela inicial e backup/restauração em JSON via **Storage Access Framework (SAF)**.

## Arquitetura e Stack Tecnológica

- **Clean Architecture + MVVM:** Separação entre `ui/` (Jetpack Compose e `TaskViewModel`), `scheduler/` (`AlarmManager`, `WorkManager`, `UrgentAlarmService`, `TaskAlarmReceiver`, `NotificationHelper`), `data/` (`AppDatabase`, `TaskDao`, `TaskEntity`, `TaskRepository`) e `widget/` (`TaskWidgetProvider`).
- **Fluxo Reativo:** Dados observados via `Flow` e coroutines (`kotlinx.coroutines`), com migrações Room (`MIGRATION_1_2`) e **KSP**.
- **Recursos do Sistema:** Entrada por voz (`Speech-to-Text`), recorrência (`Diário` e `Semanal`), tela cheia de alarme urgente (`AlarmAlertActivity`) com sonecas de `5m`, `10m` e `30m`, ações na notificação (`Adiar 5 min`, `Adiar 10 min`, `Concluir`), compartilhamento via `Intent.ACTION_SEND`, widget nativo e backup/restauração JSON via SAF.
- **Build Release:** Compilado com Gradle (`./gradlew assembleRelease`), otimizado com R8 e assinado com `keystore/release.jks`.

## Estrutura de Pastas

- `app/build.gradle.kts` e `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/meuassistente/MainActivity.kt`
- `app/src/main/java/com/example/meuassistente/data/local/` (`AppDatabase.kt`, `dao/TaskDao.kt`, `entity/TaskEntity.kt`)
- `app/src/main/java/com/example/meuassistente/data/repository/TaskRepository.kt`
- `app/src/main/java/com/example/meuassistente/scheduler/` (`NotificationHelper.kt`, `RescheduleWorker.kt`, `TaskAlarmReceiver.kt`, `TaskScheduler.kt`, `UrgentAlarmService.kt`)
- `app/src/main/java/com/example/meuassistente/ui/` (`alarm/AlarmAlertActivity.kt`, `screen/AssistantScreen.kt`, `theme/`, `viewmodel/TaskViewModel.kt`)
- `app/src/main/java/com/example/meuassistente/widget/TaskWidgetProvider.kt`
- `keystore/release.jks`

## Build Release e Instalação

- **Compilar Release:** `./gradlew assembleRelease` (gera `app/build/outputs/apk/release/app-release.apk`)
- **Instalar no aparelho:** `adb install -r app/build/outputs/apk/release/app-release.apk && adb shell am start -n com.example.meuassistente/.MainActivity`
- **Backup externo:** `mkdir -p ~/Backups/MeuAssistente_v2.2.2 && cp app/build/outputs/apk/release/app-release.apk keystore/release.jks ~/Backups/MeuAssistente_v2.2.2/`

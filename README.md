# FocusBreak – Sprint 3, parte 1: contador de pasos (HU11)

NUEVOS o REEMPLAZADOS (copiar sobre el proyecto):
- domain/StepMath.kt                         (nuevo: lógica pura + meta de pasos)
- data/StepsStore.kt                         (nuevo: DataStore)
- data/repository/StepSensorReader.kt, StepsRepository.kt   (nuevos)
- worker/StepSyncWorker.kt                   (nuevo: lectura cada ~15 min con WorkManager)
- FocusBreakApp.kt, MainActivity.kt          (REEMPLAZAN)
- ui/screens/HomeViewModel.kt, HomeScreen.kt (REEMPLAZAN: tarjeta "Pasos de hoy")
- AndroidManifest.xml                        (REEMPLAZA: añade <uses-feature> opcional del sensor)
- test/.../StepMathTest.kt                   (nuevo)

Dependencia nueva (app/build.gradle.kts):
    implementation("androidx.work:work-runtime-ktx:2.10.1")   // o la última estable

# ***NutritionTracker***

An Android app for tracking daily nutrition, body weight, and steps, with a home screen widget that mirrors the same data.

Built with Kotlin, Jetpack Compose, Room, and Glance. Nutrition data comes from the Swedish Food Agency's (Livsmedelsverket) open food composition database, and packaged products can be looked up by barcode through Open Food Facts.

## ***Features***

- **Food logging:** search Livsmedelsverket's ~2600-item Swedish food database, enter an amount in grams, and see kcal, protein, fat and carbs calculated automatically.
- **Barcode scanning:** scan a packaged product's barcode instead of typing its name. The product is looked up on Open Food Facts and logged the same way as any other food. Scanned products can't be added to favorites.
- **Manual entry:** for foods that aren't in either database, type in a name, the amount eaten, and the nutrition per 100 g (calories, protein, fat and carbs). Manually added foods can't be added to favorites.
- **Favorites:** star frequently eaten foods for one-tap logging.
- **Edit and history:** adjust a logged entry's amount afterwards, and browse previous days with the date arrows.
- **Backup:** the **Backup** button on the home screen exports your food log, weight history and favorites to a CSV file, and imports such a file again. Importing only adds what is missing, so nothing is overwritten and the same file can be imported twice safely. The file also opens in Excel or Google Sheets.
- **Nutrition goals:** set daily targets manually, or let the app calculate them from your stats (weight, height, age, sex, activity level, goal) using the Mifflin-St Jeor formula and a bodybuilding-oriented protein target.
- **Weight tracking:** log body weight per day and follow a simple trend chart.
- **Step tracking:** reads the device's step counter sensor and converts it to an estimated distance. The day starts at midnight: a background job reads the sensor about every 15 minutes, even when the app is closed, and the total at exactly 00:00 is estimated from the readings on either side of it, so a walk that crosses midnight is split correctly between the two days. The day's count also survives phone restarts.
- **Step goal:** set your own daily distance goal with the **Set Daily Steps** button on the home screen. The default is 5 km.
- **Home screen widget:** shows today's nutrition and step progress, with a configurable background (white, black or transparent). It refreshes right after you log food or change settings, and periodically in the background (every 30 minutes, Android's system minimum).

## ***Setup***

1. Clone the repo and open it in Android Studio.
2. Sync Gradle. `minSdk` is 24, so you need a device or emulator running Android 7.0 or newer.
3. Run the app and grant **Activity Recognition** when prompted (needed for step counting on Android 10+). Internet access is granted automatically.
4. To add the widget, long-press the home screen, choose **Widgets**, then **NutritionTracker**.

**Barcode scanner requirements:** the scanner uses Google's code scanner from Google Play services, so it needs no camera permission, but the device must have Google Play services. On an emulator, use a system image that includes the Play Store.

## ***Data sources***

- **Livsmedelsverket:** generic foods and their nutrient values per 100 g.
- **[Open Food Facts](https://world.openfoodfacts.org):** packaged products found by barcode. This is a community-contributed database, so some products are missing and some have no nutrition data. In that case the app shows a message instead of logging anything.
- **Manual entry:** values typed in by the user, for anything the two databases don't have.

Energy is stored in kcal. Both databases list energy in kJ as well, and the app takes the kcal value (or converts from kJ) so the two are never mixed up.

## ***Project structure***

| File | Purpose |
|---|---|
| `MainActivity.kt` | Navigation between the screens |
| `HomeScreen.kt`, `FoodLogScreen.kt`, `WeightScreen.kt`, `TargetsSettingsScreen.kt`, `StepGoalScreen.kt`, `WidgetSettingsScreen.kt` | The app's screens |
| `FoodRepository.kt`, `Network.kt` | Food logging, favorites, and the API clients (Livsmedelsverket and Open Food Facts) |
| `StepsRepository.kt`, `StepsWorker.kt` | Step counter sensor, daily step baseline, and the background reading |
| `NutritionGoals.kt` | Target calculation and the saved step goal |
| `BackupRepository.kt`, `BackupScreen.kt` | The CSV backup: export, import, and the screen for it |
| `NutritionWidget.kt`, `WidgetSupport.kt` | The Glance home screen widget |

## ***Known issues***

- **The backup covers the food log, weight and favorites only.** Nutrition goals, the step goal and the widget theme are not included and need to be set again after a reinstall.
- **Widget can lag behind.** The app asks the widget to refresh after logging or deleting food, changing settings, and opening the home screen. Android occasionally drops one of these requests when several arrive in quick succession, so the widget can briefly show old numbers. The stored data is always correct, and the widget catches up on the next update.
- **Widget steps can be slightly behind while the app is closed.** The step sensor only reports when you take a step, so a background refresh sometimes gets no fresh reading. The widget then shows the last known step count for the day instead of a fresh one, and updates the next time a reading succeeds or you open the app.
- **Midnight is only estimated when readings are close together.** The total at 00:00 is worked out from the last reading before midnight and the first one after it. That works well during a walk, when readings come every few minutes up to 15 minutes. If the two readings are more than 45 minutes apart, for example because Android delayed the background job, the day starts at the first reading and steps taken before it are not counted. If the phone's battery settings put the app to sleep, background readings can stop. On Samsung phones, add the app to **Never sleeping apps** in the phone's settings to avoid this.
- **A phone restart can cost a few minutes of steps.** The step sensor starts from zero when the phone restarts. The app keeps the steps it had already counted that day and continues from there, but steps taken between the last reading and the restart itself are lost.
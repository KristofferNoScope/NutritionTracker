***NutritionTracker***

An Android app for tracking daily nutrition, body weight, and steps, with a home screen widget that mirrors the same data.

Built with Kotlin, Jetpack Compose, Room, and Glance. Nutrition data comes from the Swedish Food Agency's (Livsmedelsverket) open food composition database, and packaged products can be looked up by barcode through Open Food Facts.

***Features***
Food logging: search Livsmedelsverket's ~2600-item Swedish food database, enter an amount in grams, and see kcal, protein, fat and carbs calculated automatically.
Barcode scanning: scan a packaged product's barcode instead of typing its name. The product is looked up on Open Food Facts and logged the same way as any other food. Scanned products can't be added to favorites.
Favorites: star frequently eaten foods for one-tap logging.
Edit and history: adjust a logged entry's amount afterwards, and browse previous days with the date arrows.
Nutrition goals: set daily targets manually, or let the app calculate them from your stats (weight, height, age, sex, activity level, goal) using the Mifflin-St Jeor formula and a bodybuilding-oriented protein target.
Weight tracking: log body weight per day and follow a simple trend chart.
Step tracking: reads the device's step counter sensor and converts it to an estimated distance.
Home screen widget: shows today's nutrition and step progress, with a configurable background (white, black or transparent). It refreshes right after you log food or change settings, and periodically in the background (every 30 minutes, Android's system minimum).

***Setup***
Clone the repo and open it in Android Studio.
Sync Gradle. minSdk is 24, so you need a device or emulator running Android 7.0 or newer.
Run the app and grant Activity Recognition when prompted (needed for step counting on Android 10+). Internet access is granted automatically.
To add the widget, long-press the home screen, choose Widgets, then NutritionTracker.

Barcode scanner requirements: the scanner uses Google's code scanner from Google Play services, so it needs no camera permission, but the device must have Google Play services. On an emulator, use a system image that includes the Play Store.

***Data sources***
Livsmedelsverket: generic foods and their nutrient values per 100 g.
Open Food Facts: packaged products found by barcode. This is a community-contributed database, so some products are missing and some have no nutrition data. In that case the app shows a message instead of logging anything.

Energy is stored in kcal. Both sources list energy in kJ as well, and the app takes the kcal value (or converts from kJ) so the two are never mixed up.

***Project structure***
File	Purpose
MainActivity.kt	Navigation between the screens
HomeScreen.kt, FoodLogScreen.kt, WeightScreen.kt, TargetsSettingsScreen.kt, WidgetSettingsScreen.kt	The app's screens
FoodRepository.kt, Network.kt	Food logging, favorites, and the API clients (Livsmedelsverket and Open Food Facts)
StepsRepository.kt	Step counter sensor and daily step baseline
NutritionGoals.kt	Target calculation
NutritionWidget.kt, WidgetSupport.kt	The Glance home screen widget

***Known issues***
Widget can lag behind. The app asks the widget to refresh after logging or deleting food, changing settings, and opening the home screen. Android occasionally drops one of these requests when several arrive in quick succession, so the widget can briefly show old numbers. The stored data is always correct, and the widget catches up on the next update.
Widget steps can be slightly behind while the app is closed. The step sensor only reports when you take a step, so a background refresh sometimes gets no fresh reading. The widget then shows the last known step count for the day instead of a fresh one, and updates the next time a reading succeeds or you open the app.
Today's steps start from the first sensor reading of the day. If the app or widget first reads the sensor late in the day, steps taken before that are not counted.

***NutritionTracker***

An Android app for tracking daily nutrition, body weight, and steps, with a home screen widget that mirrors the same data. Built with Jetpack Compose, Room, and Glance, using the Swedish Food Agency's (Livsmedelsverket) open food composition database as the source of nutrition data.

***Features***

Food logging — search Livsmedelsverket's ~2600-item Swedish food database, log an amount in grams, and see kcal/protein/fat/carbs calculated automatically.
Favorites — star frequently-eaten foods for one-tap logging.
Edit & history — adjust a logged entry's amount after the fact, and browse previous days with the date navigation arrows.
Nutrition goals — set daily targets manually, or let the app calculate them from your stats (weight, height, age, sex, activity level, goal) using the Mifflin-St Jeor BMR formula and a bodybuilding-oriented protein target.
Weight tracking — log body weight per day with a simple trend chart.
Step tracking — reads the device's step counter sensor, converted to an estimated distance.
Home screen widget — mirrors today's nutrition and step progress, with a configurable background theme (white/black/transparent). Refreshes instantly after logging food or changing settings, and periodically in the background (every 30 minutes, Android's system minimum) for steps.


***Setup***

Clone the repo and open it in Android Studio.
Sync Gradle. minSdk is 24; you'll need a device or emulator running Android 7.0+.
Grant permissions when prompted on first run: Internet (automatic) and Activity Recognition (for step counting, Android 10+).
To see the widget, add it to your home screen after installing the app — long-press the home screen → Widgets → NutritionTracker.

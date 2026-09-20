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

***Bugs***

**Widget refresh can lag by a log or two.** The app explicitly triggers a widget refresh after logging/deleting food, changing settings, and opening the home screen (plus a short delayed follow-up call as a safety net), but Android's widget host occasionally drops one of these requests when several fire in quick succession (e.g. deleting several entries back to back). The underlying data is always correct — only the widget's display can briefly lag. It always catches up within a log or two, or at the latest on the periodic 30-minute system refresh. This appears to be a platform-level timing quirk rather than something fixable purely from the app side.

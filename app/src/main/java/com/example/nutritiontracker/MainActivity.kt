package com.example.nutritiontracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nutritiontracker.ui.theme.NutritionTrackerTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keeps the step baseline starting near midnight, even when the app is closed.
        StepsWorker.schedule(applicationContext)
        setContent {
            NutritionTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onLogFoodClick = {
                    navController.navigate("food_log")
                },
                onWidgetSettingsClick = {
                    navController.navigate("widget_settings")
                },
                onNutritionGoalsClick = {
                    navController.navigate("nutrition_goals")
                },
                onStepGoalClick = {
                    navController.navigate("step_goal")
                },
                onWeightLogClick = {
                    navController.navigate("weight_log")
                },
                onBackupClick = {
                    navController.navigate("backup")
                }
            )
        }
        composable("food_log") {
            FoodLogScreen(onBackClick = {
                navController.popBackStack()
            })
        }
        composable("widget_settings") {
            WidgetSettingsScreen(onBackClick = {
                navController.popBackStack()
            })
        }
        composable("nutrition_goals") {
            TargetsSettingsScreen(onBackClick = {
                navController.popBackStack()
            })
        }
        composable("step_goal") {
            StepGoalScreen(onBackClick = {
                navController.popBackStack()
            })
        }
        composable("backup") {
            BackupScreen(onBackClick = {
                navController.popBackStack()
            })
        }
        composable("weight_log") {
            WeightScreen(onBackClick = {
                navController.popBackStack()
            })
        }
    }
}
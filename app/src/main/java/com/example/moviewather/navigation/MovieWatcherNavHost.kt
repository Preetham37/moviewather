package com.example.moviewather.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.moviewather.data.MovieRepository
import com.example.moviewather.data.preference.UserPreferencesRepository
import com.example.moviewather.ui.screens.AddMovieScreen
import com.example.moviewather.ui.screens.CategoriesScreen
import com.example.moviewather.ui.screens.CategoryDetailsScreen
import com.example.moviewather.ui.screens.MovieDetailsScreen
import com.example.moviewather.ui.screens.SettingsScreen
import com.example.moviewather.ui.screens.WatchedScreen
import com.example.moviewather.ui.screens.WatchlistScreen
import com.example.moviewather.ui.viewmodel.MovieViewModel

/**
 * Defines the full navigation graph for the application.
 *
 * [MovieViewModel] is created once at the NavHost level and shared with
 * all screens that need it.  This keeps a single ViewModel instance alive
 * for the lifetime of the NavHost, and screens never construct their own.
 *
 * @param repository Passed in from [MovieWatcherApp] which holds the
 *   database reference, keeping database construction out of this file.
 */
@Composable
fun MovieWatcherNavHost(
    navController: NavHostController,
    repository: MovieRepository,
    preferencesRepository: UserPreferencesRepository,
    modifier: Modifier = Modifier,
) {
    // Single shared ViewModel scoped to the NavHost composable's lifecycle.
    val movieViewModel: MovieViewModel = viewModel(
        factory = MovieViewModel.Factory(repository, preferencesRepository),
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Watchlist.route,
        modifier = modifier,
    ) {
        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                viewModel = movieViewModel,
                onAddMovieClick = { navController.navigate(Screen.AddMovie.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails.createRoute(movie.id))
                },
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                viewModel = movieViewModel,
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.CategoryDetails.createRoute(categoryId))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Watched.route) {
            WatchedScreen(
                viewModel = movieViewModel,
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails.createRoute(movie.id))
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = movieViewModel,
                onNavigateUp = { navController.navigateUp() },
            )
        }

        composable(Screen.AddMovie.route) {
            AddMovieScreen(
                viewModel = movieViewModel,
                onNavigateUp = { navController.navigateUp() },
            )
        }

        composable(
            route = Screen.MovieDetails.route,
            arguments = listOf(
                navArgument("movieId") {
                    type = NavType.LongType
                }
            ),
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getLong("movieId") ?: return@composable
            MovieDetailsScreen(
                movieId = movieId,
                viewModel = movieViewModel,
                onNavigateUp = { navController.navigateUp() },
            )
        }

        composable(
            route = Screen.CategoryDetails.route,
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.LongType
                }
            ),
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
            CategoryDetailsScreen(
                categoryId = categoryId,
                viewModel = movieViewModel,
                onMovieClick = { movie ->
                    navController.navigate(Screen.MovieDetails.createRoute(movie.id))
                },
                onNavigateUp = { navController.navigateUp() },
            )
        }
    }
}

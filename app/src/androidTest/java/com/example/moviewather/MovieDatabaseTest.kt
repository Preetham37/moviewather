package com.example.moviewather

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.moviewather.data.MovieRepository
import com.example.moviewather.data.local.AppDatabase
import com.example.moviewather.data.local.dao.MovieDao
import com.example.moviewather.data.local.entity.Movie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MovieDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var movieDao: MovieDao
    private lateinit var repository: MovieRepository
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        movieDao = db.movieDao()
        repository = MovieRepository(
            database = db,
            movieDao = movieDao,
            categoryDao = db.categoryDao(),
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetUnwatchedMovie() {
        runBlocking {
            val movie = Movie(title = "Inception", notes = "Mind-bending thriller")
            val id = repository.insert(movie)
            assertTrue(id > 0)

            val unwatched = repository.unwatchedMovies.first()
            assertEquals(1, unwatched.size)
            assertEquals("Inception", unwatched[0].title)
            assertEquals(false, unwatched[0].isWatched)
            assertEquals("Mind-bending thriller", unwatched[0].notes)

            val watched = repository.watchedMovies.first()
            assertTrue(watched.isEmpty())
        }
    }

    /**
     * Test 1 — Mark watched:
     * Start with: Interstellar, Inception, Dune on Watchlist.
     * Mark Interstellar as watched.
     * Result: Watchlist has Inception, Dune. Watched has Interstellar.
     */
    @Test
    fun testSection4_MarkWatchedMovesMovieWithoutDuplication() {
        runBlocking {
            val idInterstellar = repository.insert(Movie(title = "Interstellar"))
            val idInception = repository.insert(Movie(title = "Inception"))
            val idDune = repository.insert(Movie(title = "Dune"))

            var unwatched = repository.unwatchedMovies.first()
            assertEquals(3, unwatched.size)
            var watched = repository.watchedMovies.first()
            assertEquals(0, watched.size)

            val interstellar = repository.getMovieById(idInterstellar).first()!!
            val beforeTime = System.currentTimeMillis()
            repository.update(
                interstellar.copy(
                    isWatched = true,
                    dateWatched = System.currentTimeMillis()
                )
            )

            unwatched = repository.unwatchedMovies.first()
            assertEquals(2, unwatched.size)
            assertTrue(unwatched.none { it.title == "Interstellar" })
            assertTrue(unwatched.any { it.title == "Inception" })
            assertTrue(unwatched.any { it.title == "Dune" })

            watched = repository.watchedMovies.first()
            assertEquals(1, watched.size)
            assertEquals("Interstellar", watched[0].title)
            assertEquals(true, watched[0].isWatched)
            assertNotNull(watched[0].dateWatched)
            assertTrue(watched[0].dateWatched!! >= beforeTime)

            val all = repository.allMovies.first()
            assertEquals(3, all.size)
        }
    }

    /**
     * Test 3 — Undo:
     * Mark Interstellar as unwatched.
     * Result: Watchlist has all three movies. Watched is empty.
     */
    @Test
    fun testSection4_UndoWatchedRestoresToWatchlist() {
        runBlocking {
            val id = repository.insert(Movie(title = "Interstellar", isWatched = true, dateWatched = System.currentTimeMillis()))
            var watched = repository.watchedMovies.first()
            assertEquals(1, watched.size)
            var unwatched = repository.unwatchedMovies.first()
            assertEquals(0, unwatched.size)

            val movie = watched[0]
            repository.update(
                movie.copy(
                    isWatched = false,
                    dateWatched = null
                )
            )

            watched = repository.watchedMovies.first()
            assertTrue(watched.isEmpty())

            unwatched = repository.unwatchedMovies.first()
            assertEquals(1, unwatched.size)
            assertEquals("Interstellar", unwatched[0].title)
            assertEquals(false, unwatched[0].isWatched)
            assertNull(unwatched[0].dateWatched)
        }
    }

    /**
     * Test 4 — Delete:
     * Permanent removal from Room.
     */
    @Test
    fun testSection4_DeleteMoviePermanentlyRemovesFromRoom() {
        runBlocking {
            val id = repository.insert(Movie(title = "Interstellar"))
            val movie = repository.getMovieById(id).first()!!

            repository.delete(movie)

            val unwatched = repository.unwatchedMovies.first()
            assertTrue(unwatched.isEmpty())
            val byId = repository.getMovieById(id).first()
            assertNull(byId)
        }
    }

    /**
     * Test 5 — Counts:
     * Add 5 movies, mark 2 as watched.
     * Watchlist count: 3, Watched count: 2.
     */
    @Test
    fun testSection4_CountsUpdateAutomatically() {
        runBlocking {
            val m1 = repository.insert(Movie(title = "Movie 1"))
            val m2 = repository.insert(Movie(title = "Movie 2"))
            val m3 = repository.insert(Movie(title = "Movie 3"))
            val m4 = repository.insert(Movie(title = "Movie 4"))
            val m5 = repository.insert(Movie(title = "Movie 5"))

            assertEquals(5, repository.unwatchedMovies.first().size)
            assertEquals(0, repository.watchedMovies.first().size)

            val movie1 = repository.getMovieById(m1).first()!!
            val movie2 = repository.getMovieById(m2).first()!!
            repository.update(movie1.copy(isWatched = true, dateWatched = System.currentTimeMillis()))
            repository.update(movie2.copy(isWatched = true, dateWatched = System.currentTimeMillis()))

            assertEquals(3, repository.unwatchedMovies.first().size)
            assertEquals(2, repository.watchedMovies.first().size)
        }
    }

    /**
     * Test 2 — Persistence across close & reopen:
     */
    @Test
    fun testSection4_DiskPersistenceSurvivesCloseAndReopen() {
        runBlocking {
            val testDbName = "test_section4_persistence.db"
            context.deleteDatabase(testDbName)

            var diskDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbName).build()
            diskDb.movieDao().insert(Movie(title = "Interstellar", isWatched = true, dateWatched = 12345L))
            diskDb.movieDao().insert(Movie(title = "Inception", isWatched = false))
            diskDb.movieDao().insert(Movie(title = "Dune", isWatched = false))
            diskDb.close()

            diskDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbName).build()
            val watched = diskDb.movieDao().getWatchedMovies().first()
            val unwatched = diskDb.movieDao().getUnwatchedMovies().first()

            assertEquals(1, watched.size)
            assertEquals("Interstellar", watched[0].title)
            assertEquals(true, watched[0].isWatched)
            assertEquals(12345L, watched[0].dateWatched)

            assertEquals(2, unwatched.size)
            assertTrue(unwatched.any { it.title == "Inception" })
            assertTrue(unwatched.any { it.title == "Dune" })

            diskDb.close()
            context.deleteDatabase(testDbName)
        }
    }
}

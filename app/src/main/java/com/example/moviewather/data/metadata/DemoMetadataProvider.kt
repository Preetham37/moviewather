package com.example.moviewather.data.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * High-fidelity movie metadata provider providing realistic, deterministic data
 * for offline verification and out-of-the-box evaluations (e.g. Dune 1984 vs 2021).
 */
class DemoMetadataProvider : MovieMetadataProvider {

    override val providerName: String = "Movie Watcher Catalog"

    private val catalog = listOf(
        MovieMetadataResult(
            externalId = "tmdb_438631",
            title = "Dune",
            releaseYear = 2021,
            posterUrl = "https://image.tmdb.org/t/p/w500/d5NXSklXo0qyIYkgV94XAgMIckC.jpg",
            overview = "Paul Atreides, a brilliant and gifted young man born into a great destiny beyond his understanding, must travel to the most dangerous planet in the universe to ensure the future of his family and his people.",
            genres = listOf("Sci-Fi", "Adventure"),
            director = "Denis Villeneuve",
            mainCast = listOf("Timothée Chalamet", "Rebecca Ferguson", "Oscar Isaac", "Zendaya"),
            runtime = 155,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_841",
            title = "Dune",
            releaseYear = 1984,
            posterUrl = "https://image.tmdb.org/t/p/w500/gDztdfvT1gG233W89u05m72x7c.jpg",
            overview = "In the year 10191, a spice called melange is the most valuable substance in the universe, and its only known source is the desert planet Arrakis.",
            genres = listOf("Sci-Fi", "Action", "Adventure"),
            director = "David Lynch",
            mainCast = listOf("Kyle MacLachlan", "Francesca Annis", "Leonardo Cimino", "Sting"),
            runtime = 137,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_693134",
            title = "Dune: Part Two",
            releaseYear = 2024,
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            overview = "Follow the mythic journey of Paul Atreides as he unites with Chani and the Fremen while on a path of revenge against the conspirators who destroyed his family.",
            genres = listOf("Sci-Fi", "Adventure"),
            director = "Denis Villeneuve",
            mainCast = listOf("Timothée Chalamet", "Zendaya", "Rebecca Ferguson", "Javier Bardem"),
            runtime = 166,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_184345",
            title = "Jodorowsky's Dune",
            releaseYear = 2013,
            posterUrl = "https://image.tmdb.org/t/p/w500/m0ZzJm5n3n24L6F5Ua4dY91c14P.jpg",
            overview = "The story of cult film director Alejandro Jodorowsky's ambitious but ultimately doomed attempt to adapt the seminal sci-fi novel Dune in the mid-1970s.",
            genres = listOf("Documentary"),
            director = "Frank Pavich",
            mainCast = listOf("Alejandro Jodorowsky", "Michel Seydoux", "H.R. Giger", "Nicolas Winding Refn"),
            runtime = 90,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_157336",
            title = "Interstellar",
            releaseYear = 2014,
            posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            overview = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
            genres = listOf("Adventure", "Drama", "Sci-Fi"),
            director = "Christopher Nolan",
            mainCast = listOf("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain", "Michael Caine"),
            runtime = 169,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_27205",
            title = "Inception",
            releaseYear = 2010,
            posterUrl = "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
            overview = "Cobb, a skilled thief who steals corporate secrets through the use of dream-sharing technology, is given the inverse task of planting an idea into the mind of a C.E.O.",
            genres = listOf("Action", "Sci-Fi", "Adventure"),
            director = "Christopher Nolan",
            mainCast = listOf("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page", "Tom Hardy"),
            runtime = 148,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_872585",
            title = "Oppenheimer",
            releaseYear = 2023,
            posterUrl = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            overview = "The story of J. Robert Oppenheimer's role in the development of the atomic bomb during World War II.",
            genres = listOf("Biography", "Drama", "History"),
            director = "Christopher Nolan",
            mainCast = listOf("Cillian Murphy", "Emily Blunt", "Matt Damon", "Robert Downey Jr."),
            runtime = 180,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_414906",
            title = "The Batman",
            releaseYear = 2022,
            posterUrl = "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg",
            overview = "In his second year of fighting crime, Batman uncovers corruption in Gotham City that connects to his own family while facing a serial killer known as the Riddler.",
            genres = listOf("Action", "Crime", "Drama"),
            director = "Matt Reeves",
            mainCast = listOf("Robert Pattinson", "Zoë Kravitz", "Paul Dano", "Jeffrey Wright"),
            runtime = 176,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_272",
            title = "Batman Begins",
            releaseYear = 2005,
            posterUrl = "https://image.tmdb.org/t/p/w500/4MpN4Cwhv4agFOEkYXAhj96L5Mr.jpg",
            overview = "Driven by tragedy, billionaire Bruce Wayne dedicates his life to uncovering and defeating the corruption that plagues Gotham City.",
            genres = listOf("Action", "Crime", "Drama"),
            director = "Christopher Nolan",
            mainCast = listOf("Christian Bale", "Michael Caine", "Liam Neeson", "Katie Holmes"),
            runtime = 140,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_78",
            title = "Blade Runner",
            releaseYear = 1982,
            posterUrl = "https://image.tmdb.org/t/p/w500/63N9uy8nd9j7Eog2axPQ8lbr3Wj.jpg",
            overview = "A blade runner must pursue and terminate four replicants who stole a ship in space and have returned to Earth to find their creator.",
            genres = listOf("Sci-Fi", "Drama", "Thriller"),
            director = "Ridley Scott",
            mainCast = listOf("Harrison Ford", "Rutger Hauer", "Sean Young", "Edward James Olmos"),
            runtime = 117,
            provider = providerName,
        ),
        MovieMetadataResult(
            externalId = "tmdb_335984",
            title = "Blade Runner 2049",
            releaseYear = 2017,
            posterUrl = "https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
            overview = "Young Blade Runner K's discovery of a long-buried secret leads him to track down former Blade Runner Rick Deckard, who's been missing for thirty years.",
            genres = listOf("Sci-Fi", "Mystery", "Drama"),
            director = "Denis Villeneuve",
            mainCast = listOf("Ryan Gosling", "Harrison Ford", "Ana de Armas", "Sylvia Hoeks"),
            runtime = 164,
            provider = providerName,
        ),
    )

    override suspend fun searchMovies(query: String): Result<List<MovieMetadataResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

        // Small simulated network latency
        delay(250)

        val matches = catalog.filter {
            it.title.lowercase(Locale.ROOT).contains(trimmed) ||
                trimmed.contains(it.title.lowercase(Locale.ROOT))
        }

        Result.success(matches)
    }

    override suspend fun getMovieDetails(externalId: String): Result<MovieMetadataResult> = withContext(Dispatchers.IO) {
        delay(150)
        val found = catalog.firstOrNull { it.externalId == externalId }
        if (found != null) {
            Result.success(found)
        } else {
            Result.failure(MetadataException("Movie with ID $externalId not found in catalog."))
        }
    }
}

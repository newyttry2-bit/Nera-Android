package com.virex.music.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val streamUrl: String?,
    val streamUrlHq: String?,
    val streamUrlStd: String?,
    val durationSec: Int,
    val position: Int
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        durationSec = durationSec,
        streamUrl = streamUrl,
        streamUrlHq = streamUrlHq,
        streamUrlStd = streamUrlStd
    )
}

fun Song.toPlaylistSong(playlistId: Long, position: Int): PlaylistSongEntity =
    PlaylistSongEntity(
        playlistId = playlistId,
        songId = id,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        streamUrlHq = streamUrlHq,
        streamUrlStd = streamUrlStd,
        durationSec = durationSec,
        position = position
    )

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentPlayEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val streamUrl: String?,
    val streamUrlHq: String?,
    val streamUrlStd: String?,
    val durationSec: Int,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        durationSec = durationSec,
        streamUrl = streamUrl,
        streamUrlHq = streamUrlHq,
        streamUrlStd = streamUrlStd
    )
}

fun Song.toRecentPlay(): RecentPlayEntity = RecentPlayEntity(
    songId = id,
    title = title,
    artist = artist,
    artworkUrl = artworkUrl,
    streamUrl = streamUrl,
    streamUrlHq = streamUrlHq,
    streamUrlStd = streamUrlStd,
    durationSec = durationSec
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun playlistById(id: Long): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun songsFor(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun songCount(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSong(song: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: String)
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 12")
    fun recentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clearSearches()

    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 20")
    fun recentlyPlayed(): Flow<List<RecentPlayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecentPlay(play: RecentPlayEntity)

    @Query("DELETE FROM recently_played WHERE songId NOT IN (SELECT songId FROM recently_played ORDER BY playedAt DESC LIMIT 20)")
    suspend fun trimRecentPlays()
}

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        RecentSearchEntity::class,
        RecentPlayEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentDao(): RecentDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "virex_music.db"
                ).build().also { instance = it }
            }
    }
}

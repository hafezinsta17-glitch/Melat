package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records ORDER BY id DESC LIMIT :limit")
    fun getAllRecords(limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT COUNT(*) FROM records")
    fun getRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records")
    suspend fun getRecordCountSync(): Int

    @Query("""
        SELECT * FROM records 
        WHERE national_code LIKE '%' || :query || '%'
           OR account_number LIKE '%' || :query || '%'
           OR full_name LIKE '%' || :query || '%'
           OR father_name LIKE '%' || :query || '%'
           OR id_number LIKE '%' || :query || '%'
           OR card_no LIKE '%' || :query || '%'
           OR mobile LIKE '%' || :query || '%'
           OR city_name LIKE '%' || :query || '%'
           OR province_name LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY id DESC 
        LIMIT :limit
    """)
    fun searchAllFields(query: String, limit: Int = 200): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE national_code LIKE '%' || :query || '%' ORDER BY id DESC LIMIT :limit")
    fun searchByNationalCode(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE mobile LIKE '%' || :query || '%' ORDER BY id DESC LIMIT :limit")
    fun searchByMobile(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE card_no LIKE '%' || :query || '%' ORDER BY id DESC LIMIT :limit")
    fun searchByCardNo(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE account_number LIKE '%' || :query || '%' ORDER BY id DESC LIMIT :limit")
    fun searchByAccount(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE full_name LIKE '%' || :query || '%' OR father_name LIKE '%' || :query || '%' ORDER BY id DESC LIMIT :limit")
    fun searchByName(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE city_name LIKE '%' || :query || '%' OR province_name LIKE '%' || :query || '%' OR birth_city LIKE '%' || :query || '%' ORDER BY id DESC LIMIT :limit")
    fun searchByCity(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    fun getRecordById(id: Long): Flow<RecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(records: List<RecordEntity>): List<Long>

    @Query("DELETE FROM records")
    suspend fun deleteAllRecords(): Int

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecordById(id: Long): Int

    @Query("SELECT * FROM records ORDER BY id ASC")
    suspend fun getAllRecordsSync(): List<RecordEntity>
}

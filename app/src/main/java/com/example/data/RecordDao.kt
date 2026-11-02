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

    @Query("SELECT * FROM records ORDER BY id DESC LIMIT :limit")
    suspend fun getAllRecordsFast(limit: Int = 100): List<RecordEntity>

    @Query("SELECT COUNT(*) FROM records")
    fun getRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records")
    suspend fun getRecordCountSync(): Int

    // =========================================================================
    // EXACT B-TREE INDEX LOOKUPS: O(1) / O(log N)
    // Instantaneous single-key access for 100,000,000 records
    // =========================================================================

    @Query("SELECT * FROM records WHERE card_no = :query LIMIT :limit")
    suspend fun searchExactCard(query: String, limit: Int = 50): List<RecordEntity>

    @Query("SELECT * FROM records WHERE national_code = :query LIMIT :limit")
    suspend fun searchExactNationalCode(query: String, limit: Int = 50): List<RecordEntity>

    @Query("SELECT * FROM records WHERE mobile = :query LIMIT :limit")
    suspend fun searchExactMobile(query: String, limit: Int = 50): List<RecordEntity>

    @Query("SELECT * FROM records WHERE account_number = :query LIMIT :limit")
    suspend fun searchExactAccount(query: String, limit: Int = 50): List<RecordEntity>

    @Query("SELECT * FROM records WHERE id_number = :query LIMIT :limit")
    suspend fun searchExactIdNumber(query: String, limit: Int = 50): List<RecordEntity>

    // =========================================================================
    // FAST B-TREE RANGE PREFIX QUERIES: O(log N + limit)
    // Range query (col >= low AND col < high) avoids filesort on 100M rows!
    // =========================================================================

    @Query("SELECT * FROM records WHERE card_no >= :low AND card_no < :high LIMIT :limit")
    suspend fun searchCardPrefixRange(low: String, high: String, limit: Int = 100): List<RecordEntity>

    @Query("SELECT * FROM records WHERE national_code >= :low AND national_code < :high LIMIT :limit")
    suspend fun searchNationalCodePrefixRange(low: String, high: String, limit: Int = 100): List<RecordEntity>

    @Query("SELECT * FROM records WHERE mobile >= :low AND mobile < :high LIMIT :limit")
    suspend fun searchMobilePrefixRange(low: String, high: String, limit: Int = 100): List<RecordEntity>

    @Query("SELECT * FROM records WHERE account_number >= :low AND account_number < :high LIMIT :limit")
    suspend fun searchAccountPrefixRange(low: String, high: String, limit: Int = 100): List<RecordEntity>

    @Query("SELECT * FROM records WHERE full_name >= :low AND full_name < :high LIMIT :limit")
    suspend fun searchNamePrefixRange(low: String, high: String, limit: Int = 100): List<RecordEntity>

    @Query("SELECT * FROM records WHERE city_name >= :low AND city_name < :high LIMIT :limit")
    suspend fun searchCityPrefixRange(low: String, high: String, limit: Int = 100): List<RecordEntity>

    // =========================================================================
    // MULTI-INDEX PARALLEL RANGE SEEK FOR 'ALL FINANCIAL'

    // Legacy / Flow Prefix methods for backward compatibility
    @Query("SELECT * FROM records WHERE national_code LIKE :prefixQuery LIMIT :limit")
    fun searchByNationalCodePrefix(prefixQuery: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE mobile LIKE :prefixQuery LIMIT :limit")
    fun searchByMobilePrefix(prefixQuery: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE card_no LIKE :prefixQuery LIMIT :limit")
    fun searchByCardNoPrefix(prefixQuery: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE account_number LIKE :prefixQuery LIMIT :limit")
    fun searchByAccountPrefix(prefixQuery: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE full_name LIKE :prefixQuery LIMIT :limit")
    fun searchByNamePrefix(prefixQuery: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE city_name LIKE :prefixQuery LIMIT :limit")
    fun searchByCityPrefix(prefixQuery: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE national_code LIKE '%' || :query || '%' LIMIT :limit")
    fun searchByNationalCode(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE mobile LIKE '%' || :query || '%' LIMIT :limit")
    fun searchByMobile(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE card_no LIKE '%' || :query || '%' LIMIT :limit")
    fun searchByCardNo(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE account_number LIKE '%' || :query || '%' LIMIT :limit")
    fun searchByAccount(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE full_name LIKE '%' || :query || '%' OR father_name LIKE '%' || :query || '%' LIMIT :limit")
    fun searchByName(query: String, limit: Int = 100): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE city_name LIKE '%' || :query || '%' OR province_name LIKE '%' || :query || '%' OR birth_city LIKE '%' || :query || '%' LIMIT :limit")
    fun searchByCity(query: String, limit: Int = 100): Flow<List<RecordEntity>>

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
        LIMIT :limit
    """)
    fun searchAllFields(query: String, limit: Int = 100): Flow<List<RecordEntity>>

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

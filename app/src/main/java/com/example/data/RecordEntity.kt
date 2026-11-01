package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "records",
    indices = [
        Index(value = ["national_code"]),
        Index(value = ["account_number"]),
        Index(value = ["mobile"]),
        Index(value = ["card_no"]),
        Index(value = ["full_name"]),
        Index(value = ["id_number"]),
        Index(value = ["city_name"])
    ]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "national_code")
    val nationalCode: String = "",

    @ColumnInfo(name = "account_number")
    val accountNumber: String = "",

    @ColumnInfo(name = "full_name")
    val fullName: String = "",

    @ColumnInfo(name = "father_name")
    val fatherName: String = "",

    @ColumnInfo(name = "id_number")
    val idNumber: String = "",

    @ColumnInfo(name = "birth_date")
    val birthDate: String = "",

    @ColumnInfo(name = "city_name")
    val cityName: String = "",

    @ColumnInfo(name = "province_name")
    val provinceName: String = "",

    @ColumnInfo(name = "birth_city")
    val birthCity: String = "",

    @ColumnInfo(name = "birth_province")
    val birthProvince: String = "",

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "card_no")
    val cardNo: String = "",

    @ColumnInfo(name = "mobile")
    val mobile: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Formats 16-digit card number with standard quad spacing (e.g., 6037 9918 1234 5678)
     */
    val formattedCardNumber: String
        get() {
            val digits = cardNo.filter { it.isDigit() }
            return if (digits.length == 16) {
                digits.chunked(4).joinToString(" ")
            } else {
                cardNo
            }
        }

    /**
     * Formats masked card number for financial security (e.g., 6037 •••• •••• 5678)
     */
    val maskedCardNumber: String
        get() {
            val digits = cardNo.filter { it.isDigit() }
            return if (digits.length == 16) {
                "${digits.substring(0, 4)} •••• •••• ${digits.substring(12)}"
            } else {
                cardNo
            }
        }

    /**
     * Detects financial institution / bank issuer from the 6-digit BIN
     */
    val bankIssuerName: String
        get() {
            val digits = cardNo.filter { it.isDigit() }
            if (digits.length < 6) return "بانک عضو شتاب / SHETAB BANK"
            val bin = digits.substring(0, 6)
            return when (bin) {
                "603799" -> "بانک ملی / Bank Melli"
                "589210" -> "بانک سپه / Bank Sepah"
                "627648" -> "بانک توسعه صادرات / ED Bank"
                "627961" -> "بانک صنعت و معدن / Sanat o Madan"
                "603770" -> "بانک کشاورزی / Keshavarzi"
                "628023" -> "بانک مسکن / Maskan"
                "627760" -> "پست بانک ایران / Post Bank"
                "502908" -> "بانک توسعه تعاون / Taavon"
                "627412" -> "بانک اقتصاد نوین / EN Bank"
                "622106" -> "بانک پارسیان / Parsian"
                "502229" -> "بانک پاسارگاد / Pasargad"
                "627488" -> "بانک کارآفرین / Karafarin"
                "621986" -> "بانک سامان / Saman"
                "639346" -> "بانک سینا / Sina"
                "639607" -> "بانک سرمایه / Sarmayeh"
                "636214" -> "بانک آینده / Ayandeh"
                "502806", "504706" -> "بانک شهر / Shahr"
                "502938" -> "بانک دی / Dey"
                "603769" -> "بانک صادرات ایران / Saderat"
                "610433" -> "بانک ملت / Mellat"
                "589463" -> "بانک رفاه کارگران / Refah"
                "627353" -> "بانک تجارت / Tejarat"
                "504172" -> "بانک قرض‌الحسنه رسالت / Resalat"
                "606373" -> "بانک قرض‌الحسنه مهر ایران / Mehr"
                "639599" -> "بانک قوامین / Ghavamin"
                else -> "شتاب / SHETAB GATEWAY"
            }
        }

    /**
     * Formats mobile number (e.g. 0912 345 6789)
     */
    val formattedMobile: String
        get() {
            val digits = mobile.filter { it.isDigit() }
            return if (digits.length == 11 && digits.startsWith("0")) {
                "${digits.substring(0, 4)} ${digits.substring(4, 7)} ${digits.substring(7)}"
            } else {
                mobile
            }
        }
}

package com.resqnav.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.resqnav.app.model.DisasterAlert
import com.resqnav.app.model.Shelter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [DisasterAlert::class, Shelter::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun disasterDao(): DisasterDao
    abstract fun shelterDao(): ShelterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "disaster_navigator_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial shelter data
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateInitialShelters(database.shelterDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialShelters(shelterDao: ShelterDao) {
            val shelters = listOf(
                // ========== ASIA ==========

                // INDIA - PUNJAB - JALANDHAR (2 shelters)

                // INDIA - PUNJAB - JALANDHAR (2 shelters)
                Shelter(
                    name = "Jalandhar District Emergency Relief Center",
                    address = "Civil Lines, Near DC Office, Jalandhar, Punjab 144001",
                    latitude = 31.3260,
                    longitude = 75.5762,
                    capacity = 500,
                    currentOccupancy = 0,
                    contactNumber = "+91-181-2227307",
                    facilities = "Medical Aid, Food, Water, Electricity, Blankets, Security, Toilets",
                    type = "Government"
                ),
                Shelter(
                    name = "GNDU Sports Complex Emergency Shelter",
                    address = "GT Road, GNDU Campus, Jalandhar, Punjab 144007",
                    latitude = 31.3348,
                    longitude = 75.5681,
                    capacity = 800,
                    currentOccupancy = 0,
                    contactNumber = "+91-181-2258802",
                    facilities = "Medical Center, Food Distribution, Water Supply, Bedding, Power Backup",
                    type = "Educational Institution"
                ),

                // INDIA - PUNJAB - OTHER CITIES
                Shelter(
                    name = "Amritsar Community Relief Center",
                    address = "Golden Temple Complex, Amritsar, Punjab 143006",
                    latitude = 31.6200,
                    longitude = 74.8765,
                    capacity = 2000,
                    currentOccupancy = 0,
                    contactNumber = "+91-183-2553954",
                    facilities = "Free Food (Langar), Medical Aid, Water, Shelter, 24/7 Support",
                    type = "Religious"
                ),
                Shelter(
                    name = "Ludhiana Medical Emergency Center",
                    address = "Sherpur Chowk, Ludhiana, Punjab 141008",
                    latitude = 30.9010,
                    longitude = 75.8573,
                    capacity = 600,
                    currentOccupancy = 45,
                    contactNumber = "+91-161-2740445",
                    facilities = "Emergency Medical Care, ICU, Food, Water, Ambulance Services",
                    type = "Hospital"
                ),
                Shelter(
                    name = "Patiala District Hospital Relief Center",
                    address = "Mall Road, Patiala, Punjab 147001",
                    latitude = 30.3398,
                    longitude = 76.3869,
                    capacity = 400,
                    currentOccupancy = 20,
                    contactNumber = "+91-175-2212501",
                    facilities = "Medical Treatment, Emergency Beds, Food, Water, Medicine",
                    type = "Hospital"
                ),
                Shelter(
                    name = "Bathinda District Emergency Shelter",
                    address = "Mall Road, Bathinda, Punjab 151001",
                    latitude = 30.2110,
                    longitude = 74.9455,
                    capacity = 350,
                    currentOccupancy = 0,
                    contactNumber = "+91-164-2211234",
                    facilities = "Shelter, Food, Water, First Aid, Clothing Distribution",
                    type = "Educational Institution"
                ),
                Shelter(
                    name = "Mohali District Relief Center",
                    address = "Sector 65, SAS Nagar, Mohali, Punjab 160062",
                    latitude = 30.7046,
                    longitude = 76.7179,
                    capacity = 550,
                    currentOccupancy = 15,
                    contactNumber = "+91-172-2220201",
                    facilities = "Medical Aid, Food, Water, Security, Generator Backup, Toilets",
                    type = "Government"
                ),

                // INDIA - OTHER MAJOR CITIES
                Shelter(
                    name = "New Delhi Emergency Relief Center",
                    address = "1 Red Cross Road, New Delhi 110001",
                    latitude = 28.6139,
                    longitude = 77.2090,
                    capacity = 1500,
                    currentOccupancy = 120,
                    contactNumber = "+91-11-23716441",
                    facilities = "Medical Care, Food, Water, Bedding, Emergency Response Team",
                    type = "NGO"
                ),
                Shelter(
                    name = "Mumbai Municipal Disaster Relief Center",
                    address = "Byculla, Mumbai, Maharashtra 400027",
                    latitude = 18.9750,
                    longitude = 72.8258,
                    capacity = 2500,
                    currentOccupancy = 200,
                    contactNumber = "+91-22-23078585",
                    facilities = "Multi-level Shelter, Medical Teams, Food, Water, Power, Security",
                    type = "Government"
                ),
                Shelter(
                    name = "Bangalore District Emergency Shelter",
                    address = "HAL Airport Road, Bangalore, Karnataka 560017",
                    latitude = 12.9716,
                    longitude = 77.5946,
                    capacity = 1000,
                    currentOccupancy = 0,
                    contactNumber = "+91-80-25226271",
                    facilities = "Shelter, Food, Medical Aid, Water Supply, Communication Center",
                    type = "Government"
                ),
                Shelter(
                    name = "Chennai Municipal Relief Center",
                    address = "Rippon Building, George Town, Chennai, Tamil Nadu 600001",
                    latitude = 13.0827,
                    longitude = 80.2707,
                    capacity = 1200,
                    currentOccupancy = 80,
                    contactNumber = "+91-44-25384520",
                    facilities = "Emergency Shelter, Food Distribution, Medical Teams, Water, Boats",
                    type = "Government"
                ),
                Shelter(
                    name = "Kolkata Stadium Emergency Shelter",
                    address = "1A, J.L. Nehru Road, Kolkata, West Bengal 700020",
                    latitude = 22.5726,
                    longitude = 88.3639,
                    capacity = 3000,
                    currentOccupancy = 0,
                    contactNumber = "+91-33-22801346",
                    facilities = "Large Capacity Shelter, Medical Aid, Food, Water, Sanitation, Security",
                    type = "Sports Complex"
                ),

                // JAPAN
                Shelter(
                    name = "Tokyo Metropolitan Gymnasium Emergency Center",
                    address = "1-17-1 Sendagaya, Shibuya, Tokyo 151-0051",
                    latitude = 35.6762,
                    longitude = 139.7149,
                    capacity = 5000,
                    currentOccupancy = 0,
                    contactNumber = "+81-3-5474-2111",
                    facilities = "Earthquake-resistant, Medical Teams, Food, Water, Blankets, Generator",
                    type = "Sports Complex"
                ),
                Shelter(
                    name = "Osaka Castle Park Disaster Relief Shelter",
                    address = "1-1 Osakajo, Chuo Ward, Osaka 540-0002",
                    latitude = 34.6873,
                    longitude = 135.5262,
                    capacity = 3500,
                    currentOccupancy = 0,
                    contactNumber = "+81-6-6941-3044",
                    facilities = "Open Space, Emergency Supplies, Medical Aid, Communication Hub",
                    type = "Government"
                ),

                // CHINA
                Shelter(
                    name = "Beijing National Stadium Emergency Center",
                    address = "1 National Stadium S Rd, Chaoyang, Beijing 100101",
                    latitude = 39.9928,
                    longitude = 116.3975,
                    capacity = 8000,
                    currentOccupancy = 0,
                    contactNumber = "+86-10-8437-3008",
                    facilities = "Large Capacity, Medical Teams, Food Distribution, Water, Security",
                    type = "Sports Complex"
                ),
                Shelter(
                    name = "Shanghai Hongkou Stadium Relief Center",
                    address = "444 Dongjiangwan Rd, Hongkou, Shanghai 200081",
                    latitude = 31.2769,
                    longitude = 121.4838,
                    capacity = 4500,
                    currentOccupancy = 0,
                    contactNumber = "+86-21-6540-0009",
                    facilities = "Shelter, Medical Aid, Food, Water, Emergency Response",
                    type = "Sports Complex"
                ),

                // PHILIPPINES
                Shelter(
                    name = "Manila City Hall Evacuation Center",
                    address = "Arroceros St, Ermita, Manila, Metro Manila 1000",
                    latitude = 14.5907,
                    longitude = 120.9793,
                    capacity = 2000,
                    currentOccupancy = 150,
                    contactNumber = "+63-2-527-4000",
                    facilities = "Typhoon Shelter, Medical Aid, Food, Water, Emergency Teams",
                    type = "Government"
                ),

                // INDONESIA
                Shelter(
                    name = "Jakarta Gelora Bung Karno Stadium Shelter",
                    address = "Jl. Pintu Satu Senayan, Jakarta Pusat 10270",
                    latitude = -6.2188,
                    longitude = 106.8024,
                    capacity = 6000,
                    currentOccupancy = 0,
                    contactNumber = "+62-21-573-7373",
                    facilities = "Large Stadium, Medical Teams, Food, Water, Earthquake Response",
                    type = "Sports Complex"
                ),

                // THAILAND
                Shelter(
                    name = "Bangkok National Stadium Emergency Shelter",
                    address = "154 Rama I Rd, Pathum Wan, Bangkok 10330",
                    latitude = 13.7459,
                    longitude = 100.5297,
                    capacity = 3500,
                    currentOccupancy = 0,
                    contactNumber = "+66-2-214-0020",
                    facilities = "Flood Shelter, Medical Aid, Food, Water, Communication Center",
                    type = "Sports Complex"
                ),

                // ========== NORTH AMERICA ==========

                // USA
                Shelter(
                    name = "Los Angeles Convention Center Emergency Shelter",
                    address = "1201 S Figueroa St, Los Angeles, CA 90015",
                    latitude = 34.0407,
                    longitude = -118.2695,
                    capacity = 10000,
                    currentOccupancy = 0,
                    contactNumber = "+1-213-741-1151",
                    facilities = "Massive Capacity, Medical Teams, Food, Water, Red Cross, FEMA Support",
                    type = "Convention Center"
                ),
                Shelter(
                    name = "New Orleans Superdome Emergency Shelter",
                    address = "1500 Sugar Bowl Dr, New Orleans, LA 70112",
                    latitude = 29.9511,
                    longitude = -90.0812,
                    capacity = 15000,
                    currentOccupancy = 0,
                    contactNumber = "+1-504-587-3663",
                    facilities = "Hurricane Shelter, Medical Facilities, Food, Water, Security, Power",
                    type = "Sports Complex"
                ),
                Shelter(
                    name = "Houston George R. Brown Convention Center",
                    address = "1001 Avenida de las Americas, Houston, TX 77010",
                    latitude = 29.7520,
                    longitude = -95.3598,
                    capacity = 12000,
                    currentOccupancy = 0,
                    contactNumber = "+1-713-853-8000",
                    facilities = "Hurricane Relief, Medical Care, Food, Water, Red Cross, Supplies",
                    type = "Convention Center"
                ),
                Shelter(
                    name = "San Francisco Moscone Center Emergency Shelter",
                    address = "747 Howard St, San Francisco, CA 94103",
                    latitude = 37.7840,
                    longitude = -122.4014,
                    capacity = 8000,
                    currentOccupancy = 0,
                    contactNumber = "+1-415-974-4000",
                    facilities = "Earthquake Shelter, Medical Teams, Food, Water, Emergency Response",
                    type = "Convention Center"
                ),
                Shelter(
                    name = "Miami-Dade County Emergency Operations Center",
                    address = "9300 NW 41st St, Miami, FL 33178",
                    latitude = 25.8153,
                    longitude = -80.3369,
                    capacity = 5000,
                    currentOccupancy = 0,
                    contactNumber = "+1-305-468-5400",
                    facilities = "Hurricane Shelter, Medical Aid, Food, Water, Emergency Coordination",
                    type = "Government"
                ),

                // CANADA
                Shelter(
                    name = "Toronto Rogers Centre Emergency Shelter",
                    address = "1 Blue Jays Way, Toronto, ON M5V 1J1",
                    latitude = 43.6414,
                    longitude = -79.3894,
                    capacity = 7000,
                    currentOccupancy = 0,
                    contactNumber = "+1-416-341-1000",
                    facilities = "Indoor Stadium, Medical Teams, Food, Water, Heating, Security",
                    type = "Sports Complex"
                ),
                Shelter(
                    name = "Vancouver Convention Centre Emergency Shelter",
                    address = "1055 Canada Pl, Vancouver, BC V6C 0C3",
                    latitude = 49.2888,
                    longitude = -123.1116,
                    capacity = 6000,
                    currentOccupancy = 0,
                    contactNumber = "+1-604-689-8232",
                    facilities = "Earthquake/Tsunami Shelter, Medical Aid, Food, Water, Emergency Teams",
                    type = "Convention Center"
                ),

                // MEXICO
                Shelter(
                    name = "Mexico City Estadio Azteca Emergency Shelter",
                    address = "Calz. de Tlalpan 3465, Mexico City 04220",
                    latitude = 19.3030,
                    longitude = -99.1506,
                    capacity = 10000,
                    currentOccupancy = 0,
                    contactNumber = "+52-55-5617-8080",
                    facilities = "Earthquake Shelter, Medical Teams, Food, Water, Emergency Response",
                    type = "Sports Complex"
                ),

                // ========== SOUTH AMERICA ==========

                // BRAZIL
                Shelter(
                    name = "Rio de Janeiro Maracanã Stadium Shelter",
                    address = "Av. Pres. Castelo Branco, Rio de Janeiro 20271-130",
                    latitude = -22.9122,
                    longitude = -43.2302,
                    capacity = 12000,
                    currentOccupancy = 0,
                    contactNumber = "+55-21-2334-1705",
                    facilities = "Large Stadium, Medical Teams, Food, Water, Emergency Services",
                    type = "Sports Complex"
                ),
                Shelter(
                    name = "São Paulo Anhembi Convention Center",
                    address = "Av. Olavo Fontoura, 1209, São Paulo 02012-021",
                    latitude = -23.5154,
                    longitude = -46.6375,
                    capacity = 8000,
                    currentOccupancy = 0,
                    contactNumber = "+55-11-2226-0400",
                    facilities = "Flood Shelter, Medical Aid, Food, Water, Emergency Response",
                    type = "Convention Center"
                ),

                // CHILE
                Shelter(
                    name = "Santiago National Stadium Emergency Center",
                    address = "Av. Grecia 2001, Ñuñoa, Santiago",
                    latitude = -33.4653,
                    longitude = -70.6102,
                    capacity = 9000,
                    currentOccupancy = 0,
                    contactNumber = "+56-2-2238-8102",
                    facilities = "Earthquake Shelter, Medical Teams, Food, Water, Emergency Supplies",
                    type = "Sports Complex"
                ),

                // ARGENTINA
                Shelter(
                    name = "Buenos Aires Estadio Monumental Emergency Shelter",
                    address = "Av. Pres. Figueroa Alcorta 7597, Buenos Aires C1428",
                    latitude = -34.5453,
                    longitude = -58.4499,
                    capacity = 8500,
                    currentOccupancy = 0,
                    contactNumber = "+54-11-4789-1200",
                    facilities = "Disaster Shelter, Medical Aid, Food, Water, Emergency Response",
                    type = "Sports Complex"
                ),

                // ========== EUROPE ==========

                // UNITED KINGDOM
                Shelter(
                    name = "London ExCeL Emergency Shelter",
                    address = "1 Western Gateway, Royal Victoria Dock, London E16 1XL",
                    latitude = 51.5081,
                    longitude = 0.0294,
                    capacity = 15000,
                    currentOccupancy = 0,
                    contactNumber = "+44-20-7069-5000",
                    facilities = "NHS Medical Teams, Food, Water, Bedding, Emergency Coordination",
                    type = "Convention Center"
                ),
                Shelter(
                    name = "Manchester Central Convention Complex",
                    address = "Petersfield, Manchester M2 3GX",
                    latitude = 53.4757,
                    longitude = -2.2445,
                    capacity = 7000,
                    currentOccupancy = 0,
                    contactNumber = "+44-161-834-2700",
                    facilities = "Emergency Shelter, Medical Teams, Food, Water, Red Cross Support",
                    type = "Convention Center"
                ),

                // FRANCE
                Shelter(
                    name = "Paris Stade de France Emergency Center",
                    address = "93200 Saint-Denis, France",
                    latitude = 48.9245,
                    longitude = 2.3601,
                    capacity = 12000,
                    currentOccupancy = 0,
                    contactNumber = "+33-1-55-93-00-00",
                    facilities = "Large Stadium, Medical Teams, Food, Water, Emergency Services",
                    type = "Sports Complex"
                ),

                // GERMANY
                Shelter(
                    name = "Berlin Messe Exhibition Center Shelter",
                    address = "Messedamm 22, 14055 Berlin",
                    latitude = 52.5053,
                    longitude = 13.2827,
                    capacity = 10000,
                    currentOccupancy = 0,
                    contactNumber = "+49-30-3038-0",
                    facilities = "Emergency Shelter, Medical Teams, Food, Water, THW Support",
                    type = "Convention Center"
                ),

                // ITALY
                Shelter(
                    name = "Rome Stadio Olimpico Emergency Shelter",
                    address = "Viale dei Gladiatori, 00135 Roma RM",
                    latitude = 41.9341,
                    longitude = 12.4547,
                    capacity = 9000,
                    currentOccupancy = 0,
                    contactNumber = "+39-06-323-7333",
                    facilities = "Earthquake Shelter, Medical Aid, Food, Water, Civil Protection",
                    type = "Sports Complex"
                ),

                // SPAIN
                Shelter(
                    name = "Barcelona Fira de Barcelona Emergency Center",
                    address = "Av. Reina Maria Cristina, 08004 Barcelona",
                    latitude = 41.3719,
                    longitude = 2.1532,
                    capacity = 8000,
                    currentOccupancy = 0,
                    contactNumber = "+34-93-233-2000",
                    facilities = "Emergency Shelter, Medical Teams, Food, Water, Emergency Response",
                    type = "Convention Center"
                ),

                // ========== AFRICA ==========

                // SOUTH AFRICA
                Shelter(
                    name = "Cape Town International Convention Centre",
                    address = "1 Lower Long St, Cape Town City Centre, Cape Town 8001",
                    latitude = -33.9199,
                    longitude = 18.4236,
                    capacity = 6000,
                    currentOccupancy = 0,
                    contactNumber = "+27-21-410-5000",
                    facilities = "Emergency Shelter, Medical Teams, Food, Water, Security",
                    type = "Convention Center"
                ),
                Shelter(
                    name = "Johannesburg FNB Stadium Emergency Shelter",
                    address = "Nasrec, Johannesburg 2147",
                    latitude = -26.2349,
                    longitude = 27.9821,
                    capacity = 10000,
                    currentOccupancy = 0,
                    contactNumber = "+27-11-247-5000",
                    facilities = "Large Stadium, Medical Aid, Food, Water, Emergency Services",
                    type = "Sports Complex"
                ),

                // EGYPT
                Shelter(
                    name = "Cairo International Stadium Emergency Center",
                    address = "El-Geish Road, Nasr City, Cairo",
                    latitude = 30.0695,
                    longitude = 31.3176,
                    capacity = 8000,
                    currentOccupancy = 0,
                    contactNumber = "+20-2-2402-7400",
                    facilities = "Disaster Shelter, Medical Teams, Food, Water, Emergency Response",
                    type = "Sports Complex"
                ),

                // KENYA
                Shelter(
                    name = "Nairobi Kasarani Stadium Emergency Shelter",
                    address = "Thika Rd, Nairobi",
                    latitude = -1.2206,
                    longitude = 36.8897,
                    capacity = 7000,
                    currentOccupancy = 0,
                    contactNumber = "+254-20-231-5000",
                    facilities = "Emergency Shelter, Medical Aid, Food, Water, Red Cross Support",
                    type = "Sports Complex"
                ),

                // NIGERIA
                Shelter(
                    name = "Lagos National Stadium Emergency Center",
                    address = "Surulere, Lagos",
                    latitude = 6.4967,
                    longitude = 3.3680,
                    capacity = 6500,
                    currentOccupancy = 0,
                    contactNumber = "+234-1-772-8944",
                    facilities = "Disaster Shelter, Medical Teams, Food, Water, Emergency Services",
                    type = "Sports Complex"
                ),

                // ========== OCEANIA ==========

                // AUSTRALIA
                Shelter(
                    name = "Sydney Convention and Exhibition Centre",
                    address = "14 Darling Dr, Sydney NSW 2000",
                    latitude = -33.8736,
                    longitude = 151.1989,
                    capacity = 8000,
                    currentOccupancy = 0,
                    contactNumber = "+61-2-9215-7100",
                    facilities = "Emergency Shelter, Medical Teams, Food, Water, SES Support",
                    type = "Convention Center"
                ),
                Shelter(
                    name = "Melbourne Cricket Ground Emergency Center",
                    address = "Brunton Ave, Richmond VIC 3002",
                    latitude = -37.8200,
                    longitude = 144.9834,
                    capacity = 12000,
                    currentOccupancy = 0,
                    contactNumber = "+61-3-9657-8888",
                    facilities = "Large Stadium, Medical Teams, Food, Water, Emergency Response",
                    type = "Sports Complex"
                ),
                Shelter(
                    name = "Brisbane Convention Centre Emergency Shelter",
                    address = "Merivale St & Glenelg St, South Brisbane QLD 4101",
                    latitude = -27.4748,
                    longitude = 153.0196,
                    capacity = 6000,
                    currentOccupancy = 0,
                    contactNumber = "+61-7-3308-3000",
                    facilities = "Cyclone Shelter, Medical Aid, Food, Water, Emergency Services",
                    type = "Convention Center"
                ),

                // NEW ZEALAND
                Shelter(
                    name = "Auckland Eden Park Emergency Shelter",
                    address = "Reimers Ave, Kingsland, Auckland 1024",
                    latitude = -36.8747,
                    longitude = 174.7444,
                    capacity = 7000,
                    currentOccupancy = 0,
                    contactNumber = "+64-9-815-5551",
                    facilities = "Earthquake Shelter, Medical Teams, Food, Water, Civil Defence",
                    type = "Sports Complex"
                ),
                Shelter(
                    name = "Wellington Westpac Stadium Emergency Center",
                    address = "105 Waterloo Quay, Pipitea, Wellington 6011",
                    latitude = -41.2729,
                    longitude = 174.7856,
                    capacity = 5500,
                    currentOccupancy = 0,
                    contactNumber = "+64-4-473-3881",
                    facilities = "Earthquake/Tsunami Shelter, Medical Aid, Food, Water, Emergency Teams",
                    type = "Sports Complex"
                )
            )

            shelterDao.insertAll(shelters)
        }
    }
}



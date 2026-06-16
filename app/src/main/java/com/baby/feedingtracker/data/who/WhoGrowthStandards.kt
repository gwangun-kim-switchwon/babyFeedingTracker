package com.baby.feedingtracker.data.who

object WhoGrowthStandards {
    data class Percentiles(val p3: Float, val p50: Float, val p97: Float)

    // 남아 몸무게 kg (월령 → P3/P50/P97)
    val weightBoyKg: Map<Int, Percentiles> = mapOf(
        0  to Percentiles(2.5f, 3.3f, 4.4f),
        1  to Percentiles(3.4f, 4.5f, 5.8f),
        2  to Percentiles(4.4f, 5.6f, 7.1f),
        3  to Percentiles(5.0f, 6.4f, 8.0f),
        4  to Percentiles(5.6f, 7.0f, 8.7f),
        5  to Percentiles(6.0f, 7.5f, 9.3f),
        6  to Percentiles(6.4f, 7.9f, 9.8f),
        7  to Percentiles(6.7f, 8.3f, 10.2f),
        8  to Percentiles(6.9f, 8.6f, 10.7f),
        9  to Percentiles(7.2f, 8.9f, 11.0f),
        10 to Percentiles(7.5f, 9.2f, 11.4f),
        11 to Percentiles(7.4f, 9.4f, 11.7f),
        12 to Percentiles(7.7f, 9.6f, 12.0f),
        15 to Percentiles(8.3f, 10.3f, 12.8f),
        18 to Percentiles(8.8f, 10.9f, 13.6f),
        21 to Percentiles(9.2f, 11.5f, 14.4f),
        24 to Percentiles(9.7f, 12.2f, 15.3f),
    )

    // 여아 몸무게 kg
    val weightGirlKg: Map<Int, Percentiles> = mapOf(
        0  to Percentiles(2.4f, 3.2f, 4.2f),
        1  to Percentiles(3.2f, 4.2f, 5.5f),
        2  to Percentiles(4.0f, 5.1f, 6.6f),
        3  to Percentiles(4.6f, 5.8f, 7.5f),
        4  to Percentiles(5.1f, 6.4f, 8.2f),
        5  to Percentiles(5.5f, 6.9f, 8.8f),
        6  to Percentiles(5.8f, 7.3f, 9.3f),
        7  to Percentiles(6.1f, 7.6f, 9.8f),
        8  to Percentiles(6.3f, 7.9f, 10.2f),
        9  to Percentiles(6.6f, 8.2f, 10.5f),
        10 to Percentiles(6.8f, 8.5f, 10.9f),
        11 to Percentiles(7.0f, 8.7f, 11.2f),
        12 to Percentiles(7.0f, 8.9f, 11.5f),
        15 to Percentiles(7.6f, 9.6f, 12.4f),
        18 to Percentiles(8.1f, 10.2f, 13.2f),
        21 to Percentiles(8.6f, 10.9f, 14.2f),
        24 to Percentiles(9.0f, 11.5f, 15.3f),
    )

    // 남아 키 cm
    val heightBoyCm: Map<Int, Percentiles> = mapOf(
        0  to Percentiles(46.1f, 49.9f, 53.7f),
        1  to Percentiles(50.8f, 54.7f, 58.6f),
        2  to Percentiles(54.4f, 58.4f, 62.4f),
        3  to Percentiles(57.3f, 61.4f, 65.5f),
        4  to Percentiles(59.7f, 63.9f, 68.0f),
        5  to Percentiles(61.7f, 65.9f, 70.1f),
        6  to Percentiles(63.3f, 67.6f, 72.0f),
        7  to Percentiles(64.8f, 69.2f, 73.6f),
        8  to Percentiles(66.2f, 70.6f, 75.1f),
        9  to Percentiles(68.0f, 72.3f, 76.7f),
        10 to Percentiles(69.2f, 73.8f, 78.4f),
        11 to Percentiles(70.7f, 75.3f, 79.9f),
        12 to Percentiles(71.7f, 75.7f, 80.2f),
        15 to Percentiles(74.8f, 79.1f, 83.5f),
        18 to Percentiles(76.9f, 82.3f, 87.7f),
        21 to Percentiles(80.1f, 85.1f, 90.6f),
        24 to Percentiles(81.7f, 87.1f, 93.2f),
    )

    // 여아 키 cm
    val heightGirlCm: Map<Int, Percentiles> = mapOf(
        0  to Percentiles(45.4f, 49.1f, 52.9f),
        1  to Percentiles(49.8f, 53.7f, 57.6f),
        2  to Percentiles(53.0f, 57.1f, 61.1f),
        3  to Percentiles(55.6f, 59.8f, 64.0f),
        4  to Percentiles(57.8f, 62.1f, 66.4f),
        5  to Percentiles(59.6f, 64.0f, 68.5f),
        6  to Percentiles(61.5f, 65.7f, 70.3f),
        7  to Percentiles(62.7f, 67.3f, 71.9f),
        8  to Percentiles(64.3f, 68.7f, 73.5f),
        9  to Percentiles(65.6f, 70.1f, 74.7f),
        10 to Percentiles(67.0f, 71.5f, 76.4f),
        11 to Percentiles(68.3f, 72.8f, 77.8f),
        12 to Percentiles(69.2f, 74.0f, 78.9f),
        15 to Percentiles(72.8f, 77.5f, 82.5f),
        18 to Percentiles(75.0f, 80.7f, 86.4f),
        21 to Percentiles(78.0f, 83.7f, 89.8f),
        24 to Percentiles(80.0f, 86.0f, 92.2f),
    )

    // 남아 머리둘레 cm
    val headBoyCm: Map<Int, Percentiles> = mapOf(
        0  to Percentiles(32.1f, 34.5f, 36.9f),
        1  to Percentiles(35.1f, 37.3f, 39.7f),
        2  to Percentiles(36.9f, 39.1f, 41.3f),
        3  to Percentiles(38.1f, 40.5f, 42.9f),
        4  to Percentiles(39.2f, 41.6f, 44.0f),
        5  to Percentiles(40.1f, 42.6f, 44.9f),
        6  to Percentiles(41.5f, 44.0f, 46.4f),
        9  to Percentiles(43.5f, 45.8f, 48.2f),
        12 to Percentiles(44.7f, 46.8f, 49.3f),
        18 to Percentiles(46.1f, 48.3f, 50.7f),
        24 to Percentiles(47.1f, 49.2f, 51.8f),
    )

    // 여아 머리둘레 cm
    val headGirlCm: Map<Int, Percentiles> = mapOf(
        0  to Percentiles(31.5f, 33.9f, 36.2f),
        1  to Percentiles(34.3f, 36.6f, 38.9f),
        2  to Percentiles(36.0f, 38.3f, 40.5f),
        3  to Percentiles(37.1f, 39.5f, 41.9f),
        4  to Percentiles(38.2f, 40.6f, 43.0f),
        5  to Percentiles(38.9f, 41.5f, 44.0f),
        6  to Percentiles(40.2f, 42.4f, 44.8f),
        9  to Percentiles(42.0f, 44.2f, 46.5f),
        12 to Percentiles(43.1f, 45.4f, 47.8f),
        18 to Percentiles(44.7f, 46.9f, 49.2f),
        24 to Percentiles(45.7f, 48.0f, 50.4f),
    )

    /**
     * 월령에 해당하는 백분위수를 선형 보간하여 반환합니다.
     * [ageMonths]가 데이터 범위를 벗어나면 null을 반환합니다.
     */
    fun interpolate(data: Map<Int, Percentiles>, ageMonths: Int): Percentiles? {
        if (ageMonths < 0) return null
        val sorted = data.keys.sorted()
        val lower = sorted.lastOrNull { it <= ageMonths } ?: return null
        val upper = sorted.firstOrNull { it > ageMonths } ?: return data[lower]
        if (lower == ageMonths) return data[lower]
        val lp = data[lower]!!
        val up = data[upper]!!
        val r = (ageMonths - lower).toFloat() / (upper - lower).toFloat()
        return Percentiles(
            p3  = lp.p3  + (up.p3  - lp.p3)  * r,
            p50 = lp.p50 + (up.p50 - lp.p50) * r,
            p97 = lp.p97 + (up.p97 - lp.p97) * r,
        )
    }
}

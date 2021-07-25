/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*
import kotlin.test.assertFails

import kotlin.time.*

class Durations {

    @Sample
    fun toIsoString() {
        assertPrints(Duration.nanoseconds(25).toIsoString(), "PT0.000000025S")
        assertPrints(Duration.milliseconds(120.3).toIsoString(), "PT0.120300S")
        assertPrints(Duration.seconds(30.5).toIsoString(), "PT30.500S")
        assertPrints(Duration.minutes(30.5).toIsoString(), "PT30M30S")
        assertPrints(Duration.seconds(86420).toIsoString(), "PT24H0M20S")
        assertPrints(Duration.days(2).toIsoString(), "PT48H")
        assertPrints(Duration.ZERO.toIsoString(), "PT0S")
        assertPrints(Duration.INFINITE.toIsoString(), "PT9999999999999H")
    }

    @Sample
    fun toStringDefault() {
        assertPrints(Duration.days(45), "45d")
        assertPrints(Duration.days(1.5), "1d 12h")
        assertPrints(Duration.minutes(1230), "20h 30m")
        assertPrints(Duration.minutes(920), "15h 20m")
        assertPrints(Duration.seconds(1.546), "1.546s")
        assertPrints(Duration.milliseconds(25.12), "25.12ms")
    }

    @Sample
    fun toStringDecimals() {
        assertPrints(Duration.minutes(1230).toString(DurationUnit.DAYS, 2), "0.85d")
        assertPrints(Duration.minutes(1230).toString(DurationUnit.HOURS, 2), "20.50h")
        assertPrints(Duration.minutes(1230).toString(DurationUnit.MINUTES), "1230m")
        assertPrints(Duration.minutes(1230).toString(DurationUnit.SECONDS), "73800s")
    }

    @Sample
    fun parse() {
        val isoString = "PT1H30M"
        val defaultString = "1d 16h 15m"
        val invalidFormat = "1 hour"

        assertPrints(Duration.parse(isoString) == Duration.minutes(90), "true")
        assertPrints(Duration.parse(defaultString) == Duration.hours(40) + Duration.minutes(15), "true")
        assertFails { Duration.parse(invalidFormat) }
        assertPrints(Duration.parseOrNull(invalidFormat), "null")
    }

    @Sample
    fun parseIsoString() {
        val isoString = "PT1H30M"
        val defaultString = "1d 16h 15m"

        assertPrints(Duration.parseIsoString(isoString) == Duration.minutes(90), "true")
        assertFails { Duration.parseIsoString(defaultString) }
        assertPrints(Duration.parseIsoStringOrNull(defaultString), "null")
    }

}
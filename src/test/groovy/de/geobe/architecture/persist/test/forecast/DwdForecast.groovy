/*
 * The MIT License (MIT)
 *
 *                            Copyright (c) 2021. Georg Beier
 *
 *                            Permission is hereby granted, free of charge, to any person obtaining a copy
 *                            of this software and associated documentation files (the "Software"), to deal
 *                            in the Software without restriction, including without limitation the rights
 *                            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *                            copies of the Software, and to permit persons to whom the Software is
 *                            furnished to do so, subject to the following conditions:
 *
 *                            The above copyright notice and this permission notice shall be included in all
 *                            copies or substantial portions of the Software.
 *
 *                            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *                            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *                            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *                            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *                            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *                            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *                            SOFTWARE.
 *
 */

package de.geobe.architecture.persist.test.forecast


import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity
class DwdForecast {
    static zoneId = ZoneId.systemDefault()
    static timeformat = DateTimeFormatter.ofPattern("dd.MM.yy  HH.mm")

    static xmlMapping = [
            TTT  : 'temperature',
            PPPP : 'pressure',
            FF   : 'windSpeed',
            FX1  : 'windGust',
            DD   : 'windDegrees',
            VV   : 'visibility',
            N    : 'totalCloudCover',
            SunD1: 'sunshineDuration',
            RR1c : 'hourlyPrecipitation',
            Rad1h: 'globalIrradiance'
    ]

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id

    Long issuedAt
    Long forecastTime

    Float temperature
    Float pressure
    Float visibility
    Float totalCloudCover
    Float sunshineDuration
    Float windSpeed
    Float windGust
    Float windDegrees
    Float hourlyPrecipitation
    Float globalIrradiance

    @Override
    String toString() {
        return "${longAsTime(forecastTime)} forecast, ${hoursAhead()} hours ahead, " +
                "issued at ${longAsTime(issuedAt)}" + """
\ttemp ${temperature ? String.format('% 5.1f', temperature - 273.15) : '???'} °C, p ${pressure ? pressure / 100.0 : '???'} hPa \
rain $hourlyPrecipitation mm, clouds $totalCloudCover %, sun $sunshineDuration s,  irradiance $globalIrradiance kJ/m²\
"""
    }

    def hoursAhead() {
        String.format('% 5.1f', (forecastTime - issuedAt) / 3600.0)
    }

    def longAsTime(long timestamp) {
        Instant.ofEpochSecond(timestamp).atZone(zoneId).format(timeformat)
    }

    def asLocalDateTime(long timestamp) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zoneId)
    }
}

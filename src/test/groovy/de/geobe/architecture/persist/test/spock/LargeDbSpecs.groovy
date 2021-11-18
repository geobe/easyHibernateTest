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

package de.geobe.architecture.persist.test.spock

import de.geobe.architecture.persist.test.EasyHibernateDbConnection
import de.geobe.architecture.persist.test.forecast.DwdForecast
import spock.lang.Shared
import spock.lang.Specification

class LargeDbSpecs extends  Specification {

    def easy = EasyHibernateDbConnection.easyDatabase
    def dao = easy.forecastDao
    def rows = dao.find('select count(*) from DwdForecast')[0]

    def setupSpec() {
        EasyHibernateDbConnection.runServer()
    }

    def cleanupSpec() {
        EasyHibernateDbConnection.easyDatabase.forecastDao.closeSession()
        EasyHibernateDbConnection.easyDatabase.forecastDb.closeDatabase()
        EasyHibernateDbConnection.stopServer()
    }

    def "can get rowcount from large table"() {
        when:
        println rows
        def count = (Integer) rows
        then:
        count != null
    }

    def 'iteration over a large dataset hits all of its elements'() {
        given:
        long count = 0
        when:
        def forecastIterator = dao.iterateAll()
        def begin = System.nanoTime()
        forecastIterator.eachWithIndex { forecast, i ->
            count++
        }
        println "test done with $count rows in ${(System.nanoTime() - begin).intdiv(1000000)} ms"
        then:
        count == rows
    }

    def 'pagewise iteration over a large dataset hits all of its elements'() {
        given:
        long count = 0
        long pages = 0
        int pageSize = 30000
        when:
        def forecastListIterator = dao.iteratePages(pageSize, 0)
        def begin = System.nanoTime()
        forecastListIterator.each { pageOfForecasts ->
            count += pageOfForecasts.size()
            pages++
        }
        println "$pages pages from $count rows of upto $pageSize elements done in ${(System.nanoTime() - begin).intdiv(1000000)} ms"
        def mod = count % pageSize
        then:
        count == rows
        mod == 0 ? pages == count.intdiv(pageSize) : pages == count.intdiv(pageSize) + 1
    }

    def 'iteration with predicates works'() {
        given:
        def predicates = 'where globalIrradiance > 2600' +
                ' and forecastTime - issuedAt = 3600' +
                ' order by totalCloudCover'
        def count = 0
        def complied = true
        def ordered = true
        def lastValue = 0.0f
        when:
        def forecastIterator = dao.iterateAll(predicates)
        forecastIterator.each { DwdForecast forecast ->
            count++
            complied = complied && forecast.globalIrradiance > 2600.0f
            ordered = ordered && forecast.totalCloudCover >= lastValue
            lastValue = forecast.totalCloudCover
        }
        println "$count record match predicates"
        then:
        count > 0
        complied
        ordered
    }

    def 'paged iteration with predicates works'() {
        given:
        def predicates = 'where globalIrradiance > 2000' +
                ' and forecastTime - issuedAt = 3600' +
                ' and sunshineDuration >= 3500' +
                ' order by sunshineDuration desc'
        def count = 0
        def complied = true
        def ordered = true
        def lastValue = 3600.0f // seconds per hour
        when:
        def forecastListIterator = dao.iteratePages(10, 0, predicates)
        forecastListIterator.each {forecasts ->
            forecasts.each {forecast ->
                count++
                complied = complied && forecast.globalIrradiance > 2000.0f
                ordered = ordered && forecast.sunshineDuration <= lastValue
                lastValue = forecast.sunshineDuration
            }
        }
        println "$count record match predicates"
        then:
        count > 0
        complied
        ordered
    }
}
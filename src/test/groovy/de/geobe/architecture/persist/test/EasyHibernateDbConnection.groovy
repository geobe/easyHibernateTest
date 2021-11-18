/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021. Georg Beier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package de.geobe.architecture.persist.test

import de.geobe.architecture.persist.DaoHibernate
import de.geobe.architecture.persist.DbHibernate
import de.geobe.architecture.persist.test.address.AddressBase
import de.geobe.architecture.persist.test.address.Communication
import de.geobe.architecture.persist.test.address.OrganisationalAddress
import de.geobe.architecture.persist.test.address.PersonalAddress
import de.geobe.architecture.persist.test.forecast.DwdForecast
import org.h2.tools.Server;

public class EasyHibernateDbConnection {

    static Server tcpServer
    DbHibernate forecastDb
    DbHibernate addressDb
    DbHibernate address2Db
    DaoHibernate<DwdForecast> forecastDao
    DaoHibernate<AddressBase> baseDao;
    DaoHibernate<AddressBase> base2Dao;
    DaoHibernate<Communication> commDao;
    DaoHibernate<OrganisationalAddress> orgaDao;
    DaoHibernate<PersonalAddress> persDao;
    DaoHibernate<PersonalAddress> pers2Dao;

    def classes = [
            'de.geobe.architecture.persist.test.address.AddressBase',
            'de.geobe.architecture.persist.test.address.Communication',
            'de.geobe.architecture.persist.test.address.OrganisationalAddress',
            'de.geobe.architecture.persist.test.address.PersonalAddress'
    ]

    static private EasyHibernateDbConnection easyDatatbase

    static synchronized EasyHibernateDbConnection getEasyDatabase() {
        if (!easyDatatbase) {
            easyDatatbase = new EasyHibernateDbConnection()
        }
        easyDatatbase
    }

    /**
     * Default constructor starts H2 server on port 19092 and binds @Entity classes
     * for address and forecast data records to the database using hibernate.
     */
    EasyHibernateDbConnection() {
//        runServer()
        forecastDb = new DbHibernate(['de.geobe.architecture.persist.test.forecast.DwdForecast'], 'forecast.cfg.xml')
        addressDb = new DbHibernate(classes, "address.cfg.xml")
        // simulate access from an other session
        address2Db = new DbHibernate(classes, "address.cfg.xml")

        forecastDao = new DaoHibernate<DwdForecast>(DwdForecast.class, forecastDb)
        baseDao = new DaoHibernate<>(AddressBase.class, addressDb);
        commDao = new DaoHibernate<>(Communication.class, addressDb);
        orgaDao = new DaoHibernate<>(OrganisationalAddress.class, addressDb);
        persDao = new DaoHibernate<>(PersonalAddress.class, addressDb);

        base2Dao = new DaoHibernate<>(AddressBase.class, address2Db);
        pers2Dao = new DaoHibernate<>(PersonalAddress.class, address2Db);
    }

    /**
     * start H2 tcp server on non-standard port 19092 if not already running
     */
    static void runServer() {
        try {
            tcpServer = Server.createTcpServer('-baseDir', './src/test/resources/h2', '-tcpAllowOthers',
                    '-tcpDaemon', '-ifNotExists', '-tcpPort', '19092')
            if (!tcpServer.isRunning(true)) {
                tcpServer.start()
                println "tcpServer started"
            } else {
                println "tcpServer already running"
            }
        } catch (Exception ex) {
            println "tcpServer already running"
        }
    }

    static void stopServer() {
        tcpServer.shutdown()
        println "tcpServer stopped"
    }

}
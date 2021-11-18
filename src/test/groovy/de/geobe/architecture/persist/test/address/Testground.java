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

package de.geobe.architecture.persist.test.address;

import de.geobe.architecture.persist.DaoHibernate;
import de.geobe.architecture.persist.DbHibernate;
import de.geobe.architecture.persist.test.EasyHibernateDbConnection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * test class to initialize small test database.
 */
public class Testground {

    static DbHibernate addressDb = EasyHibernateDbConnection.getEasyDatabase().getAddressDb();
    static DaoHibernate<AddressBase> baseDao = EasyHibernateDbConnection.getEasyDatabase().getBaseDao();
    static DaoHibernate<Communication> commDao = EasyHibernateDbConnection.getEasyDatabase().getCommDao();
    static DaoHibernate<OrganisationalAddress> orgaDao = EasyHibernateDbConnection.getEasyDatabase().getOrgaDao();
    static DaoHibernate<PersonalAddress> persDao = EasyHibernateDbConnection.getEasyDatabase().getPersDao();

    static List<AddressBase> generateSample() {
        List<AddressBase> result = new ArrayList<>();
        Communication[] comms = {
                new Communication(CommType.MOBILE, "0167 345 6789", "Nikki Handy"),
                new Communication(CommType.MESSENGER, "telegram://Nico_Lausi_1234", "Nikki Telegram")
        };
        PersonalAddress nikki = new PersonalAddress(
                "Nikki", "Nico", "Lausi", Instant.parse("1234-12-06T06:00:00Z"));
        nikki.addComm(comms[0]);
        nikki.addComm(comms[1]);
        comms[0].setOwner(nikki);
        comms[1].setOwner(nikki);
        result.add(nikki);
        result.add(new PersonalAddress("Doggi", "Oggi", "Dalmatian", Instant.parse("2008-11-06T00:00:00Z")));
        result.add(new PersonalAddress("Pipa", "Pille", "Palle", Instant.parse("2000-01-01T00:00:00Z")));
        result.add(new PersonalAddress("Lups", "Luna", "Pudel", Instant.parse("2018-11-06T00:00:00Z")));
        result.add(new PersonalAddress("Lemmi", "Ein", "Lemming", Instant.now()));
        result.add(new OrganisationalAddress("Die Firma", "TBQ"));
        return result;
    }

    public static void main(String[] args) {
//        runServer();
//        createDbConnection();
        commDao.deleteAll();
        baseDao.deleteAll();
        baseDao.closeSession();
        generateSample().forEach(adb -> baseDao.save(adb));
        List<AddressBase> adbs = baseDao.fetchAll();
        assert adbs.size() == 6;
        PersonalAddress sample = new PersonalAddress();
        sample.setNickname("L%");
        List<PersonalAddress> qbe1 = persDao.findByExample(sample);
        assert qbe1.stream().map(PersonalAddress::getNickname).collect(Collectors.toList()).containsAll(Arrays.asList("Lups", "Lemmi"));
        PersonalAddress anAddress = qbe1.get(0);
        long id = anAddress.getId();
        anAddress.setNickname("Duffy");
        persDao.save(anAddress);
        AddressBase toFetch = baseDao.fetch(id);
        assert toFetch.getNickname().equals("Duffy");
        List<Object> cl = commDao.find("from Comm");
        assert cl.size() == 2;
        List<Object> hql = commDao.find("select owner from Comm where commtype = :ct", Map.of("ct", CommType.MOBILE.ordinal()));
        assert hql.size() == 1;
        assert hql.get(0) instanceof PersonalAddress;
        baseDao.commit();
        toFetch.setNickname("Schnuffy");
        baseDao.save(toFetch);
        baseDao.rollback();
        // rollback doesn't change the local object
        assert toFetch.getNickname().equals("Schnuffy");
        // so retrieve a new copy from db
        AddressBase newFetch = baseDao.fetch(toFetch.getId());
        // assert rollback succeeded
        assert newFetch.getNickname().equals("Duffy");
        baseDao.delete(newFetch);
        baseDao.commit();
        toFetch = baseDao.fetch(id);
        assert toFetch == null;
        baseDao.closeSession();
        persDao.closeSession();
        addressDb.closeSession();
        System.out.println("Database " + addressDb.toString() + ", baseDao " + baseDao.toString());
//        stopServer();
        addressDb.closeDatabase();
//        while (true) {
//            Thread.sleep(1000);
//        }
    }
}

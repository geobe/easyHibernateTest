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
import de.geobe.architecture.persist.test.address.*
import spock.lang.Specification

import java.time.Instant

class DaoMethodsSpecs extends Specification {
    def easy = EasyHibernateDbConnection.easyDatabase
    def baseDao = easy.baseDao
    def commDao = easy.commDao
    def persDao = easy.persDao
    def orgaDao = easy.orgaDao
    def sample = generateSample()


    def setupSpec() {
        EasyHibernateDbConnection.runServer()
    }

    def cleanupSpec() {
        EasyHibernateDbConnection.easyDatabase.baseDao.closeSession()
        EasyHibernateDbConnection.easyDatabase.commDao.closeSession()
        EasyHibernateDbConnection.easyDatabase.addressDb.closeSession()
        EasyHibernateDbConnection.stopServer()
    }

    def setup() {
        commDao.deleteAll()
        baseDao.deleteAll()
        baseDao.closeSession()
        saveSample()
    }

    def 'sample is stored completely in database using baseclass dao'() {
        expect:
        baseDao.fetchAll().size() == 6
        persDao.fetchAll().size() == 5
        orgaDao.fetchAll().size() == 1
        commDao.fetchAll().size() == 2
    }

    def 'query by example returns right objects'() {
        when:
        def qbeTemplate = new PersonalAddress()
        qbeTemplate.setNickname("L%")
        def qbeResult = persDao.findByExample(qbeTemplate)
        then:
        qbeResult.collect {it.nickname}.containsAll(["Lups", "Lemmi"])
    }

    def 'changes are saved, commited, rolled back in db across inheritance hierarchy'() {
        given:
        def nick = 'Duffy'
        def nick2 = 'Snoopy'
        when:
        def someone = persDao.findByExample(new PersonalAddress(nickname: 'L%')).first()
        then:
        someone != null
        when: 'a nickname is changed and saved'
        def id = someone.id
        someone.nickname = nick
        persDao.save(someone)
        def fetched = baseDao.fetch(id)
        then:
        fetched.nickname == nick
        when: 'the nickname is changed again after a commit'
        persDao.commit()
        fetched.nickname = nick2
        baseDao.save(fetched)
        persDao.rollback()
        then: 'the local object remains changed but in the db, change is reverted'
        fetched.nickname == nick2
        baseDao.fetch(fetched.id).nickname == nick
    }

    def 'HQL queries can cross associations'() {
        given:
        when: 'HQL selects all objects'
        def comms = commDao.find('from Comm')
        then: 'it\'s the same as findAll'
        comms.containsAll(commDao.fetchAll())
        when: 'we select associated objects'
        def hqlResult = commDao.find('select owner from Comm where commtype = :ct', [ct: CommType.MOBILE.ordinal()])
        then: 'we get one result from our sample data of class PersonalAddress'
        hqlResult.size() == 1
        hqlResult[0] instanceof PersonalAddress
    }

    def 'delete an object'() {
        when:
        def all = persDao.fetchAll()
        def target = all[2]
        baseDao.delete(target)
        baseDao.commit()
        then:
        persDao.fetchAll().size() == all.size() - 1

    }

    def 'discover stale object access'() {
        given: 'a dao from a different database connection'
        def pers2Dao = easy.pers2Dao
        when: ' we have the same object in different sessions'
        def nikki1 = persDao.findByExample(new PersonalAddress(nickname: 'Nikki')).first()
        persDao.closeSession()
        def nikki2 = pers2Dao.fetch(nikki1.id)
        then: 'both objects are equal'
        nikki1.id == nikki2.id
        nikki1.firstName == nikki2.firstName
        when: 'we change both objects  differently'
        nikki1.firstName = 'Klaus'
        nikki2.firstName = 'Nikolaus'
        def save1 = pers2Dao.save(nikki2)
        def comm2 = pers2Dao.commit()
        def save2 = persDao.save(nikki1)
        def comm1 = persDao.commit()
        then: ' only one object can be saved and committed, other is "stale"'
        save1 == true
        comm1 == false
        save2 == true
        comm2 == true
        when: 'we update the stale object'
        nikki1 = persDao.fetch(nikki1.id)
        then: 'own changes are gone, changes from other instance are taken'
        nikki1.lastName == nikki2.lastName
        nikki1.firstName == nikki2.firstName
    }

    def 'calling toString for higher coverage'() {
        when:
        def out = baseDao.toString()
        println out
        then:
        out != null
    }

    def saveSample() {
        sample.each {
            baseDao.save(it)
        }
    }

    List<AddressBase> generateSample() {
        List<AddressBase> result = new ArrayList<>()
        Communication[] comms = [
            new Communication(CommType.MOBILE, "0167 345 6789", "Nikki Handy"),
            new Communication(CommType.MESSENGER, "telegram://Nico_Lausi_1234", "Nikki Telegram")
        ]
        PersonalAddress nikki = new PersonalAddress(
                "Nikki", "Nico", "Lausi", Instant.parse("1234-12-06T06:00:00Z"))
        nikki.addComm(comms[0])
        nikki.addComm(comms[1])
        comms[0].setOwner(nikki)
        comms[1].setOwner(nikki)
        result.add(nikki)
        result.add(new PersonalAddress("Doggi", "Oggi", "Dalmatian", Instant.parse("2008-11-06T00:00:00Z")))
        result.add(new PersonalAddress("Pipa", "Pille", "Palle", Instant.parse("2000-01-01T00:00:00Z")))
        result.add(new PersonalAddress("Lups", "Luna", "Pudel", Instant.parse("2018-11-06T00:00:00Z")))
        result.add(new PersonalAddress("Lemmi", "Ein", "Lemming", Instant.now()))
        result.add(new OrganisationalAddress("Die Firma", "TBQ"))
        return result
    }
}

package fr.echoes.labs.ksf.cc.ui;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import fr.echoes.labs.ksf.cc.KomeaFoundationGuiApplication;

@ActiveProfiles({ "test,internalAuth" })
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { KomeaFoundationGuiApplication.class })
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public abstract class AbstractSpringTest {

	@Autowired
	private MongoTemplate mongoTemplate;

	@After
	public void cleanDB() {
//		for (String collectionName : mongoTemplate.getCollectionNames()) {
//            if (!collectionName.startsWith("system.")) {
//                mongoTemplate.getCollection(collectionName).findAndRemove(null);
//            }
//        }
		this.mongoTemplate.getDb().dropDatabase();
	}

}

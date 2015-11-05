/**
 * © Nowina Solutions, 2015-2015
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.server.manager;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import lu.nowina.nexu.ConfigurationException;

public class SCDataBaseManagerTest {

	@Test
	public void test1() {
		SCDatabaseManager manager = new SCDatabaseManager();
		manager.nexuDatabaseFile = new FileSystemResource("target/non-existing.xml");
		Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", manager.getDatabaseDigest());
	}

	@Test
	public void test2() {
		SCDatabaseManager manager = new SCDatabaseManager();
		manager.nexuDatabaseFile = new FileSystemResource("src/test/resources/db.xml");
		Assert.assertEquals("98259f589138d7509e90cb3668f47be8", manager.getDatabaseDigest());
	}

	@Test(expected = ConfigurationException.class)
	public void test3() {
		SCDatabaseManager manager = new SCDatabaseManager();
		manager.postConstruct();
	}

}

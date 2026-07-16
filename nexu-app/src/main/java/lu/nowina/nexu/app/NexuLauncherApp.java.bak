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
package lu.nowina.nexu.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lu.nowina.nexu.NexuLauncher;

public class NexuLauncherApp {
	
	public static void main(String[] args) throws Exception {
		
		List<String> list = new ArrayList<String>(args!=null ? Arrays.asList(args) : new ArrayList<String>());
		
		list.add("--add-modules");
		list.add("jdk.crypto.cryptoki,javafx.controls,javafx.fxml,javafx.swing,java.xml.bind,java.smartcardio");
		
		list.add("--add-exports");
		list.add("jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED");		
								
		list.add("--add-opens");
		list.add("jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED");
		
		list.add("javafx.preloader");
		list.add("lu.nowina.nexu.NexUPreLoader");
		list.add("glass.accessible.force");
		list.add("false");
		
		args = new String[list.size()];
		list.toArray(args);
		
		// Call main javafx11 application
		NexuLauncher.main(args);
	}
}

package lu.nowina.nexu.api;

import java.util.ArrayList;
import java.util.List;

import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
// MOD 4535992 TODO to re-enable for dss 5.9
//import eu.europa.esig.dss.enumerations.KeyUsageBit;
import eu.europa.esig.dss.tsl.KeyUsageBit;
import lu.nowina.nexu.api.CertificateFilter;

/**
 * Provides filtering capabilities for product adapters.
 * 
 * @author Landry Soules
 *
 */
public class CertificateFilterHelper {

	public List<DSSPrivateKeyEntry> filterKeys(SignatureTokenConnection token, CertificateFilter filter) {
		if (filter.getNonRepudiationBit()) {
			List<DSSPrivateKeyEntry> filteredList = new ArrayList<>();
			for (DSSPrivateKeyEntry entry : token.getKeys()) {
                // MOD 4535992 TODO to re-enable for dss 5.9 and disabled if
				////if (entry.getCertificate().checkKeyUsage(KeyUsageBit.NON_REPUDIATION)) {
				//	filteredList.add(entry);
				////}
                // MOD 4535992 disabled if               
                //if (entry.getCertificate().checkKeyUsage(KeyUsageBit.nonRepudiation)) {
					filteredList.add(entry);
				//}
			}
			return filteredList;
		}
		return token.getKeys();
	}
}

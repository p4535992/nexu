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
package lu.nowina.nexu.flow.operation;

import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.CancelledOperationException;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.Operation;
import lu.nowina.nexu.api.flow.OperationResult;

/**
 * Signs a digest that was prepared by the remote signing application. This is
 * deliberately separate from {@link SignOperation}, which hashes the supplied
 * data before signing it.
 */
public class SignDigestOperation implements Operation<SignatureValue> {

	private SignatureTokenConnection token;
	private Digest digest;
	private DSSPrivateKeyEntry key;

	public SignDigestOperation() {
		super();
	}

	@Override
	public void setParams(Object... params) {
		try {
			this.token = (SignatureTokenConnection) params[0];
			this.digest = (Digest) params[1];
			this.key = (DSSPrivateKeyEntry) params[2];
		} catch (final ArrayIndexOutOfBoundsException | ClassCastException e) {
			throw new IllegalArgumentException(
					"Expected parameters: SignatureTokenConnection, Digest, DSSPrivateKeyEntry", e);
		}
	}

	@Override
	public OperationResult<SignatureValue> perform() {
		try {
			return new OperationResult<SignatureValue>(token.signDigest(digest, key));
		} catch (final CancelledOperationException e) {
			return new OperationResult<SignatureValue>(BasicOperationStatus.USER_CANCEL);
		}
	}
}

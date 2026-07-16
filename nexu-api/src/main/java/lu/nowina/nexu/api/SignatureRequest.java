/**
 * © Nowina Solutions, 2015-2015
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.api;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.ToBeSigned;

/**
 * Signature request supporting both the historical "data to be signed" flow and
 * the modern pre-hashed flow used when a remote signing application prepares
 * the document signature structure.
 */
public class SignatureRequest extends NexuRequest {

	private TokenId tokenId;

	private ToBeSigned toBeSigned;

	private Digest digest;

	private DigestAlgorithm digestAlgorithm;

	private String keyId;

	/*
	 * Historical flag name retained for JSON compatibility. False keeps the
	 * cached token session available for a following operation.
	 */
	private String doClearCache;

	public SignatureRequest() {
	}

	public TokenId getTokenId() {
		return tokenId;
	}

	public void setTokenId(TokenId tokenId) {
		this.tokenId = tokenId;
	}

	public ToBeSigned getToBeSigned() {
		return toBeSigned;
	}

	public void setToBeSigned(ToBeSigned toBeSigned) {
		this.toBeSigned = toBeSigned;
	}

	public Digest getDigest() {
		return digest;
	}

	public void setDigest(Digest digest) {
		this.digest = digest;
	}

	public boolean isPreHashed() {
		return digest != null && digest.getValue() != null;
	}

	public DigestAlgorithm getDigestAlgorithm() {
		return digestAlgorithm;
	}

	public void setDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
		this.digestAlgorithm = digestAlgorithm;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public boolean isDoClearCache() {
		return !"false".equals(this.doClearCache);
	}

	public void setDoClearCache(final String doClearCache) {
		this.doClearCache = doClearCache;
	}

	public String getDoClearCache() {
		return this.doClearCache;
	}
}

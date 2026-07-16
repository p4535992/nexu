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
package lu.nowina.nexu.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.NexuException;
import lu.nowina.nexu.api.Execution;
import lu.nowina.nexu.api.LogoutRequest;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.Product;
import lu.nowina.nexu.api.ProductAdapter;
import lu.nowina.nexu.api.SignatureRequest;
import lu.nowina.nexu.api.SignatureResponse;
import lu.nowina.nexu.api.TokenId;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.operation.AdvancedCreationFeedbackOperation;
import lu.nowina.nexu.flow.operation.GetTokenConnectionOperation;
import lu.nowina.nexu.flow.operation.GetTokenOperation;
import lu.nowina.nexu.flow.operation.SelectPrivateKeyOperation;
import lu.nowina.nexu.flow.operation.SignDigestOperation;
import lu.nowina.nexu.flow.operation.SignOperation;
import lu.nowina.nexu.flow.operation.TokenOperationResultKey;
import lu.nowina.nexu.view.core.UIDisplay;
import lu.nowina.nexu.view.core.UIOperation;

class SignatureFlow extends AbstractCoreFlow<SignatureRequest, SignatureResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SignatureFlow.class.getName());

	public SignatureFlow(UIDisplay display, NexuAPI api) {
		super(display, api);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Execution<SignatureResponse> process(NexuAPI api, SignatureRequest req) throws Exception {
		final boolean hasDigest = req.isPreHashed();
		final boolean hasDataToSign = req.getToBeSigned() != null && req.getToBeSigned().getBytes() != null;

		if (hasDigest == hasDataToSign) {
			throw new NexuException("Exactly one of digest or toBeSigned must be provided");
		}
		if (hasDigest && req.getDigest().getAlgorithm() == null) {
			throw new NexuException("Digest algorithm expected");
		}
		if (hasDataToSign && req.getDigestAlgorithm() == null) {
			throw new NexuException("Digest algorithm expected");
		}

		SignatureTokenConnection token = null;
		TokenId tokenId = null;
		try {
			final OperationResult<Map<TokenOperationResultKey, Object>> getTokenOperationResult =
					getOperationFactory().getOperation(GetTokenOperation.class, api, req.getTokenId()).perform();
			if (getTokenOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
				final Map<TokenOperationResultKey, Object> map = getTokenOperationResult.getResult();
				tokenId = (TokenId) map.get(TokenOperationResultKey.TOKEN_ID);

				final OperationResult<SignatureTokenConnection> getTokenConnectionOperationResult =
						getOperationFactory().getOperation(GetTokenConnectionOperation.class, api, tokenId).perform();
				if (getTokenConnectionOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
					token = getTokenConnectionOperationResult.getResult();

					final Product product = (Product) map.get(TokenOperationResultKey.SELECTED_PRODUCT);
					final ProductAdapter productAdapter = (ProductAdapter) map.get(TokenOperationResultKey.SELECTED_PRODUCT_ADAPTER);
					final OperationResult<DSSPrivateKeyEntry> selectPrivateKeyOperationResult =
							getOperationFactory().getOperation(
									SelectPrivateKeyOperation.class, token, api, product, productAdapter, null, req.getKeyId()).perform();
					if (selectPrivateKeyOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
						final DSSPrivateKeyEntry key = selectPrivateKeyOperationResult.getResult();
						final OperationResult<SignatureValue> signOperationResult;
						if (hasDigest) {
							signOperationResult = getOperationFactory().getOperation(
									SignDigestOperation.class, token, req.getDigest(), key).perform();
						} else {
							signOperationResult = getOperationFactory().getOperation(
									SignOperation.class, token, req.getToBeSigned(), req.getDigestAlgorithm(), key).perform();
						}

						if (signOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
							final SignatureValue value = signOperationResult.getResult();

							if ((Boolean) map.get(TokenOperationResultKey.ADVANCED_CREATION)) {
								getOperationFactory().getOperation(AdvancedCreationFeedbackOperation.class,
										api, map).perform();
							}

							if (api.getAppConfig().isEnablePopUps() && api.getAppConfig().isEnableInformativePopUps()) {
								getOperationFactory().getOperation(UIOperation.class, "/fxml/message.fxml",
										"signature.flow.finished", api.getAppConfig().getApplicationName()).perform();
							}

							return new Execution<SignatureResponse>(
									new SignatureResponse(value, key.getCertificate(), key.getCertificateChain()));
						}
						return handleErrorOperationResult(signOperationResult);
					}

					if (api.getAppConfig().isEnablePopUps()) {
						getOperationFactory().getOperation(UIOperation.class, "/fxml/message.fxml",
								"signature.flow.no.key.selected", api.getAppConfig().getApplicationName()).perform();
					}
					return handleErrorOperationResult(selectPrivateKeyOperationResult);
				}

				if (api.getAppConfig().isEnablePopUps()) {
					getOperationFactory().getOperation(UIOperation.class, "/fxml/message.fxml",
							"signature.flow.bad.token", api.getAppConfig().getApplicationName()).perform();
				}
				return handleErrorOperationResult(getTokenConnectionOperationResult);
			}
			return handleErrorOperationResult(getTokenOperationResult);
		} catch (Exception e) {
			logger.error("Signature flow failed", e);
			throw handleException(e);
		} finally {
			if (token != null) {
				// LogoutFlow owns token closing and cache cleanup. Avoid closing the
				// same connection twice here.
				api.logout(new LogoutRequest(tokenId, req.isDoClearCache(), true));
			}
		}
	}
}

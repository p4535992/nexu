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
package lu.nowina.nexu.rest;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.ToBeSigned;
import lu.nowina.nexu.api.CertificateFilter;
import lu.nowina.nexu.api.Execution;
import lu.nowina.nexu.api.Feedback;
import lu.nowina.nexu.api.FeedbackStatus;
import lu.nowina.nexu.api.GetCertificateRequest;
import lu.nowina.nexu.api.LogoutRequest;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.NexuRequest;
import lu.nowina.nexu.api.Purpose;
import lu.nowina.nexu.api.SignatureRequest;
import lu.nowina.nexu.api.TokenId;
import lu.nowina.nexu.api.plugin.HttpPlugin;
import lu.nowina.nexu.api.plugin.HttpRequest;
import lu.nowina.nexu.api.plugin.HttpResponse;
import lu.nowina.nexu.api.plugin.HttpStatus;
import lu.nowina.nexu.api.plugin.InitializationMessage;
import lu.nowina.nexu.json.GsonHelper;

/**
 * Legacy REST compatibility plugin. New integrations should use the /v1 API.
 */
public class RestHttpPlugin implements HttpPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RestHttpPlugin.class.getName());
	private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

	@Override
	public List<InitializationMessage> init(String pluginId, NexuAPI api) {
		return Collections.emptyList();
	}

	@Override
	public HttpResponse process(NexuAPI api, HttpRequest req) throws Exception {
		final String target = req.getTarget();
		logger.debug("Legacy REST request for {}", target);

		// Never log request bodies: they may contain hashes, token handles,
		// certificate identifiers or other signing material.
		final String payload = IOUtils.toString(req.getInputStream());

		switch (target) {
		case "/sign":
			return signRequest(api, req, payload);
		case "/certificates":
			return getCertificates(api, req, payload);
		case "/logout":
			return logout(api);
		case "/identityInfo":
			return retiredEndpoint("identityInfo",
					"Identity information was never backed by a complete registered flow and has been removed.");
		case "/authenticate":
			return retiredEndpoint("authenticate",
					"Raw challenge signing has been removed. Use the origin-bound, one-time challenge authentication protocol when available.");
		default:
			return new HttpResponse(
					"{\"success\":false,\"error\":\"ENDPOINT_NOT_FOUND\",\"errorMessage\":\"Unknown NexU REST endpoint\"}",
					JSON_CONTENT_TYPE,
					HttpStatus.NOT_FOUND);
		}
	}

	protected <T> Execution<T> returnNullIfValid(NexuRequest request) {
		return null;
	}

	private HttpResponse signRequest(NexuAPI api, HttpRequest req, String payload) {
		final SignatureRequest request;
		if (StringUtils.isEmpty(payload)) {
			request = new SignatureRequest();

			final String data = req.getParameter("dataToSign");
			if (data != null) {
				final ToBeSigned toBeSigned = new ToBeSigned();
				toBeSigned.setBytes(DatatypeConverter.parseBase64Binary(data));
				request.setToBeSigned(toBeSigned);
			}

			final String digestAlgorithm = req.getParameter("digestAlgo");
			if (digestAlgorithm != null) {
				request.setDigestAlgorithm(DigestAlgorithm.forName(digestAlgorithm, DigestAlgorithm.SHA256));
			}

			final String tokenId = req.getParameter("tokenId");
			if (tokenId != null) {
				request.setTokenId(new TokenId(tokenId));
			}

			final String keyId = req.getParameter("keyId");
			if (keyId != null) {
				request.setKeyId(keyId);
			}
		} else {
			request = GsonHelper.fromJson(payload, SignatureRequest.class);
		}

		final HttpResponse invalidRequest = checkRequestValidity(api, request);
		if (invalidRequest != null) {
			return invalidRequest;
		}
		return toHttpResponse(api.sign(request));
	}

	private HttpResponse logout(NexuAPI api) {
		// Legacy HTTP logout intentionally clears all cached state.
		final LogoutRequest request = new LogoutRequest(null, true, true);
		final HttpResponse invalidRequest = checkRequestValidity(api, request);
		if (invalidRequest != null) {
			return invalidRequest;
		}
		return toHttpResponse(api.logout(request));
	}

	private HttpResponse getCertificates(NexuAPI api, HttpRequest req, String payload) {
		final GetCertificateRequest request;
		if (StringUtils.isEmpty(payload)) {
			request = new GetCertificateRequest();

			final String certificatePurpose = req.getParameter("certificatePurpose");
			if (certificatePurpose != null) {
				final CertificateFilter filter = new CertificateFilter();
				filter.setPurpose(Enum.valueOf(Purpose.class, certificatePurpose));
				request.setCertificateFilter(filter);
			} else {
				final String nonRepudiation = req.getParameter("nonRepudiation");
				if (isNotBlank(nonRepudiation)) {
					final CertificateFilter filter = new CertificateFilter();
					filter.setNonRepudiationBit(Boolean.parseBoolean(nonRepudiation));
					request.setCertificateFilter(filter);
				}
			}
		} else {
			request = GsonHelper.fromJson(payload, GetCertificateRequest.class);
		}

		final HttpResponse invalidRequest = checkRequestValidity(api, request);
		if (invalidRequest != null) {
			return invalidRequest;
		}
		return toHttpResponse(api.getCertificate(request));
	}

	private HttpResponse checkRequestValidity(final NexuAPI api, final NexuRequest request) {
		final Execution<Object> verification = returnNullIfValid(request);
		if (verification == null) {
			return null;
		}

		final Feedback feedback;
		if (verification.getFeedback() == null) {
			feedback = new Feedback();
			feedback.setFeedbackStatus(FeedbackStatus.SIGNATURE_VERIFICATION_FAILED);
			verification.setFeedback(feedback);
		} else {
			feedback = verification.getFeedback();
		}
		feedback.setInfo(api.getEnvironmentInfo());
		feedback.setNexuVersion(api.getAppConfig().getApplicationVersion());
		return toHttpResponse(verification);
	}

	private HttpResponse toHttpResponse(final Execution<?> response) {
		return new HttpResponse(
				GsonHelper.toJson(response),
				JSON_CONTENT_TYPE,
				response.isSuccess() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY);
	}

	private static HttpResponse retiredEndpoint(final String endpoint, final String message) {
		final String content = "{\"success\":false,\"error\":\"ENDPOINT_RETIRED\","
				+ "\"errorMessage\":\"Legacy /rest/" + endpoint + " is retired. " + escapeJson(message) + "\"}";
		return new HttpResponse(content, JSON_CONTENT_TYPE, HttpStatus.GONE);
	}

	private static String escapeJson(final String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}

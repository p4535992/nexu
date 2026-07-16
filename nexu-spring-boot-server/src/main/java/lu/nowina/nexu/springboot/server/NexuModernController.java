package lu.nowina.nexu.springboot.server;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.x509.CertificateToken;
import lu.nowina.nexu.api.CertificateFilter;
import lu.nowina.nexu.api.Execution;
import lu.nowina.nexu.api.GetCertificateRequest;
import lu.nowina.nexu.api.GetCertificateResponse;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.Purpose;
import lu.nowina.nexu.api.SignatureRequest;
import lu.nowina.nexu.api.SignatureResponse;
import lu.nowina.nexu.api.TokenId;
import lu.nowina.nexu.springboot.server.NexuModernDtos.ApiResponse;
import lu.nowina.nexu.springboot.server.NexuModernDtos.KeyHandle;
import lu.nowina.nexu.springboot.server.NexuModernDtos.SignHashRequest;
import lu.nowina.nexu.springboot.server.NexuModernDtos.SignHashResponse;
import lu.nowina.nexu.springboot.server.NexuModernDtos.SigningCertificateRequest;
import lu.nowina.nexu.springboot.server.NexuModernDtos.SigningCertificateResponse;
import lu.nowina.nexu.springboot.server.NexuModernDtos.StatusResponse;

/**
 * Modern local signing API. It follows the Web eID separation of certificate
 * selection and pre-hashed signing while retaining the NexU smart-card engine.
 */
@RestController
@RequestMapping("/v1")
final class NexuModernController {

    static final String PROTOCOL_VERSION = "nexu:2.0";

    private final NexuAPI api;

    NexuModernController(final NexuAPI api) {
        this.api = Objects.requireNonNull(api, "api");
    }

    @GetMapping("/status")
    ApiResponse<StatusResponse> status() {
        return ApiResponse.success(new StatusResponse(
                trim(api.getAppConfig().getApplicationVersion()),
                PROTOCOL_VERSION,
                List.of("getSigningCertificate", "signDigest", "legacyRestCompatibility")));
    }

    @PostMapping("/signing-certificate")
    ResponseEntity<ApiResponse<SigningCertificateResponse>> signingCertificate(
            @RequestBody(required = false) final SigningCertificateRequest requestBody) {

        final SigningCertificateRequest input = requestBody == null
                ? new SigningCertificateRequest(null, null, null)
                : requestBody;

        final GetCertificateRequest request = new GetCertificateRequest();
        request.setCloseToken(input.closeToken() != null
                ? input.closeToken().booleanValue()
                : api.getAppConfig().getCloseToken());

        final CertificateFilter filter = certificateFilter(input);
        if (filter != null) {
            request.setCertificateFilter(filter);
        }

        final Execution<GetCertificateResponse> execution = api.getCertificate(request);
        if (!execution.isSuccess()) {
            return operationFailure(execution);
        }

        final GetCertificateResponse response = execution.getResponse();
        if (response == null || response.getTokenId() == null || response.getCertificate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "NexU returned an incomplete signing certificate response");
        }

        final SigningCertificateResponse modernResponse = new SigningCertificateResponse(
                encode(response.getCertificate()),
                encode(response.getCertificateChain()),
                response.getEncryptionAlgorithm() == null ? null : response.getEncryptionAlgorithm().name(),
                digestNames(response.getSupportedDigests()),
                response.getPreferredDigest() == null ? null : javaDigestName(response.getPreferredDigest()),
                new KeyHandle(response.getTokenId().getId(), response.getKeyId()));

        return ResponseEntity.ok(ApiResponse.success(modernResponse));
    }

    @PostMapping("/sign")
    ResponseEntity<ApiResponse<SignHashResponse>> sign(
            @RequestBody final SignHashRequest input) {

        if (input == null || input.keyHandle() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keyHandle is required");
        }
        if (isBlank(input.keyHandle().tokenId()) || isBlank(input.keyHandle().keyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keyHandle is incomplete");
        }
        if (isBlank(input.hash()) || isBlank(input.hashFunction())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hash and hashFunction are required");
        }

        final byte[] hash;
        try {
            hash = Base64.getDecoder().decode(input.hash());
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hash must be Base64 encoded", e);
        }

        final DigestAlgorithm digestAlgorithm = parseDigestAlgorithm(input.hashFunction());
        validateDigestLength(digestAlgorithm, hash);

        final SignatureRequest request = new SignatureRequest();
        request.setTokenId(new TokenId(input.keyHandle().tokenId()));
        request.setKeyId(input.keyHandle().keyId());
        request.setDigest(new Digest(digestAlgorithm, hash));
        request.setDoClearCache(Boolean.toString(input.clearToken() == null || input.clearToken().booleanValue()));

        final Execution<SignatureResponse> execution = api.sign(request);
        if (!execution.isSuccess()) {
            return operationFailure(execution);
        }

        final SignatureResponse response = execution.getResponse();
        if (response == null || response.getSignatureValue() == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "NexU returned an incomplete signature response");
        }

        final SignHashResponse modernResponse = new SignHashResponse(
                Base64.getEncoder().encodeToString(response.getSignatureValue()),
                response.getSignatureAlgorithm() == null ? null : response.getSignatureAlgorithm().name(),
                encode(response.getCertificate()),
                encode(response.getCertificateChain()));

        return ResponseEntity.ok(ApiResponse.success(modernResponse));
    }

    private static CertificateFilter certificateFilter(final SigningCertificateRequest input) {
        if (isBlank(input.certificatePurpose()) && input.nonRepudiation() == null) {
            return null;
        }

        final CertificateFilter filter = new CertificateFilter();
        if (!isBlank(input.certificatePurpose())) {
            try {
                filter.setPurpose(Purpose.valueOf(input.certificatePurpose().trim().toUpperCase(Locale.ROOT)));
            } catch (final IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported certificatePurpose: " + input.certificatePurpose(), e);
            }
        }
        if (input.nonRepudiation() != null) {
            filter.setNonRepudiationBit(input.nonRepudiation());
        }
        return filter;
    }

    private static DigestAlgorithm parseDigestAlgorithm(final String value) {
        final String normalized = value.trim().toUpperCase(Locale.ROOT).replace("-", "");
        final DigestAlgorithm algorithm = DigestAlgorithm.forName(normalized, null);
        if (algorithm == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported hashFunction: " + value);
        }
        return algorithm;
    }

    private static void validateDigestLength(final DigestAlgorithm algorithm, final byte[] digest) {
        final int expectedLength;
        try {
            expectedLength = algorithm.getMessageDigest().getDigestLength();
        } catch (final NoSuchAlgorithmException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Hash function is not available in the current Java runtime: " + javaDigestName(algorithm), e);
        }
        if (expectedLength > 0 && digest.length != expectedLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid " + javaDigestName(algorithm) + " hash length: expected "
                            + expectedLength + " bytes but received " + digest.length);
        }
    }

    private static List<String> digestNames(final List<DigestAlgorithm> algorithms) {
        if (algorithms == null) {
            return Collections.emptyList();
        }
        return algorithms.stream().filter(Objects::nonNull).map(NexuModernController::javaDigestName).toList();
    }

    private static String javaDigestName(final DigestAlgorithm algorithm) {
        return algorithm.getJavaName();
    }

    private static String encode(final CertificateToken certificate) {
        return certificate == null ? null : Base64.getEncoder().encodeToString(certificate.getEncoded());
    }

    private static List<String> encode(final CertificateToken[] certificates) {
        if (certificates == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(certificates)
                .filter(Objects::nonNull)
                .map(NexuModernController::encode)
                .toList();
    }

    private static <T> ResponseEntity<ApiResponse<T>> operationFailure(final Execution<?> execution) {
        return ResponseEntity.unprocessableEntity().body(
                ApiResponse.failure(execution.getError(), execution.getErrorMessage()));
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(final String value) {
        return value == null ? "" : value.trim();
    }
}

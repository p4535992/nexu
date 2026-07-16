package lu.nowina.nexu.springboot.server;

import java.util.List;

/**
 * JSON objects for the Web eID-inspired NexU protocol. The key handle is opaque
 * to callers and only identifies an already selected local token and key.
 */
final class NexuModernDtos {

    private NexuModernDtos() {
    }

    record ApiResponse<T>(
            boolean success,
            T response,
            String error,
            String errorMessage) {

        static <T> ApiResponse<T> success(final T response) {
            return new ApiResponse<>(true, response, null, null);
        }

        static <T> ApiResponse<T> failure(final String error, final String errorMessage) {
            return new ApiResponse<>(false, null, error, errorMessage);
        }
    }

    record StatusResponse(
            String version,
            String protocol,
            List<String> capabilities) {
    }

    record KeyHandle(String tokenId, String keyId) {
    }

    record SigningCertificateRequest(
            Boolean closeToken,
            String certificatePurpose,
            Boolean nonRepudiation) {
    }

    record SigningCertificateResponse(
            String certificate,
            List<String> certificateChain,
            String encryptionAlgorithm,
            List<String> supportedHashFunctions,
            String preferredHashFunction,
            KeyHandle keyHandle) {
    }

    record SignHashRequest(
            KeyHandle keyHandle,
            String hash,
            String hashFunction,
            Boolean clearToken) {
    }

    record SignHashResponse(
            String signature,
            String signatureAlgorithm,
            String certificate,
            List<String> certificateChain) {
    }
}

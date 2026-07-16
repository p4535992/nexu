package lu.nowina.nexu.springboot.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.Execution;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.SignatureRequest;
import lu.nowina.nexu.api.SignatureResponse;
import lu.nowina.nexu.springboot.server.NexuModernDtos.ApiResponse;
import lu.nowina.nexu.springboot.server.NexuModernDtos.KeyHandle;
import lu.nowina.nexu.springboot.server.NexuModernDtos.SignHashRequest;
import lu.nowina.nexu.springboot.server.NexuModernDtos.SignHashResponse;
import lu.nowina.nexu.springboot.server.NexuModernDtos.StatusResponse;

class NexuModernControllerTest {

    private NexuAPI api;
    private AppConfig appConfig;
    private NexuModernController controller;

    @BeforeEach
    void setUp() {
        api = mock(NexuAPI.class);
        appConfig = mock(AppConfig.class);
        when(api.getAppConfig()).thenReturn(appConfig);
        controller = new NexuModernController(api);
    }

    @Test
    void exposesModernCapabilities() {
        when(appConfig.getApplicationVersion()).thenReturn("1.24-SNAPSHOT\n");

        final ApiResponse<StatusResponse> result = controller.status();

        assertTrue(result.success());
        assertEquals("1.24-SNAPSHOT", result.response().version());
        assertEquals("nexu:2.0", result.response().protocol());
        assertTrue(result.response().capabilities().contains("signDigest"));
    }

    @Test
    void sendsPreparedHashToDssSignDigestFlow() {
        final byte[] hash = new byte[32];
        hash[0] = 42;
        final SignatureResponse signatureResponse = mock(SignatureResponse.class);
        when(signatureResponse.getSignatureValue()).thenReturn(new byte[] {1, 2, 3});
        when(api.sign(any(SignatureRequest.class))).thenReturn(new Execution<>(signatureResponse));

        final SignHashRequest input = new SignHashRequest(
                new KeyHandle("token-1", "key-1"),
                Base64.getEncoder().encodeToString(hash),
                "SHA-256",
                true);

        final ResponseEntity<ApiResponse<SignHashResponse>> result = controller.sign(input);

        final ArgumentCaptor<SignatureRequest> requestCaptor = ArgumentCaptor.forClass(SignatureRequest.class);
        verify(api).sign(requestCaptor.capture());
        final SignatureRequest request = requestCaptor.getValue();

        assertTrue(request.isPreHashed());
        assertEquals(DigestAlgorithm.SHA256, request.getDigest().getAlgorithm());
        assertArrayEquals(hash, request.getDigest().getValue());
        assertEquals("token-1", request.getTokenId().getId());
        assertEquals("key-1", request.getKeyId());
        assertEquals(200, result.getStatusCode().value());
        assertEquals(Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}),
                result.getBody().response().signature());
    }
}

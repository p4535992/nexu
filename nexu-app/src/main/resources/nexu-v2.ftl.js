/*
 * NexU browser client v2.
 *
 * The modern API follows the Web eID-style certificate -> prepare hash -> sign
 * flow. Legacy callback functions remain available and continue to use the
 * historical REST protocol for backwards compatibility.
 */
(function (global) {
    "use strict";

    const baseUrl = "${scheme}://${nexu_hostname}:${nexu_port}";
    const certificateHandles = new Map();

    class NexuError extends Error {
        constructor(message, code, status, details) {
            super(message || code || "NexU request failed");
            this.name = "NexuError";
            this.code = code || "ERR_NEXU_REQUEST_FAILED";
            this.status = status;
            this.details = details;
        }
    }

    async function request(path, method, body) {
        const response = await fetch(baseUrl + path, {
            method: method || "GET",
            mode: "cors",
            cache: "no-store",
            headers: body === undefined ? undefined : {"Content-Type": "application/json"},
            body: body === undefined ? undefined : JSON.stringify(body)
        });

        const text = await response.text();
        let payload = null;
        if (text) {
            try {
                payload = JSON.parse(text);
            } catch (error) {
                throw new NexuError("NexU returned invalid JSON", "ERR_NEXU_INVALID_RESPONSE", response.status, text);
            }
        }

        if (!response.ok || (payload && payload.success === false)) {
            const code = payload && payload.error ? payload.error : "ERR_NEXU_REQUEST_FAILED";
            const message = payload && payload.errorMessage
                ? payload.errorMessage
                : "NexU request failed with HTTP status " + response.status;
            throw new NexuError(message, code, response.status, payload);
        }

        return payload && Object.prototype.hasOwnProperty.call(payload, "response")
            ? payload.response
            : payload;
    }

    function certificateValue(certificate) {
        return typeof certificate === "string" ? certificate : certificate && certificate.certificate;
    }

    function keyHandleFor(certificate) {
        if (certificate && typeof certificate === "object" && certificate.keyHandle) {
            return certificate.keyHandle;
        }
        return certificateHandles.get(certificateValue(certificate));
    }

    async function status() {
        return request("/v1/status", "GET");
    }

    async function getSigningCertificate(options) {
        const result = await request("/v1/signing-certificate", "POST", options || {});
        if (result && result.certificate && result.keyHandle) {
            certificateHandles.set(result.certificate, result.keyHandle);
        }
        return result;
    }

    async function sign(certificate, hash, hashFunction, options) {
        const keyHandle = keyHandleFor(certificate);
        if (!keyHandle) {
            throw new NexuError(
                "No local key handle is associated with the supplied certificate. Call getSigningCertificate first.",
                "ERR_NEXU_KEY_HANDLE_MISSING");
        }

        const result = await request("/v1/sign", "POST", {
            keyHandle: keyHandle,
            hash: hash,
            hashFunction: hashFunction,
            clearToken: !options || options.clearToken !== false
        });

        return {
            signature: result.signature,
            signatureAlgorithm: result.signatureAlgorithm,
            certificate: result.certificate,
            certificateChain: result.certificateChain
        };
    }

    async function legacyRequest(service, body) {
        const response = await fetch(baseUrl + "/rest/" + service, {
            method: "POST",
            mode: "cors",
            cache: "no-store",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(body || {})
        });
        const payload = await response.json();
        if (!response.ok || payload.success === false) {
            throw new NexuError(
                payload.errorMessage || "Legacy NexU request failed",
                payload.error || "ERR_NEXU_LEGACY_REQUEST_FAILED",
                response.status,
                payload);
        }
        return payload;
    }

    function callbacks(promise, successCallback, errorCallback) {
        promise.then(function (value) {
            if (typeof successCallback === "function") {
                successCallback(value);
            }
        }).catch(function (error) {
            if (typeof errorCallback === "function") {
                errorCallback(error);
            }
        });
    }

    global.NexU = Object.freeze({
        protocolVersion: "nexu:2.0",
        status: status,
        getSigningCertificate: getSigningCertificate,
        sign: sign,
        NexuError: NexuError
    });

    // Historical callback API, now implemented without jQuery.
    global.nexu_get_certificates = function (successCallback, errorCallback) {
        callbacks(
            legacyRequest("certificates", {closeToken: ${close_token}}),
            successCallback,
            errorCallback);
    };

    global.nexu_sign_with_token_infos = function (
        tokenId,
        keyId,
        dataToSign,
        digestAlgorithm,
        successCallback,
        errorCallback,
        doClearCache) {

        callbacks(
            legacyRequest("sign", {
                tokenId: {id: tokenId},
                keyId: keyId,
                toBeSigned: {bytes: dataToSign},
                digestAlgorithm: digestAlgorithm,
                doClearCache: String(doClearCache)
            }),
            successCallback,
            errorCallback);
    };

    global.nexu_sign = function (dataToSign, digestAlgorithm, successCallback, errorCallback) {
        callbacks(
            legacyRequest("sign", {
                toBeSigned: {bytes: dataToSign},
                digestAlgorithm: digestAlgorithm
            }),
            successCallback,
            errorCallback);
    };
})(window);

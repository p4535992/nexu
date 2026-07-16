package lu.nowina.nexu.generic;

import static sun.security.pkcs11.wrapper.PKCS11Constants.CKF_OS_LOCKING_OK;

import java.io.File;
import java.lang.reflect.Field;
import java.security.AuthProvider;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.PasswordInputCallback;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;
import eu.europa.esig.dss.token.SunPKCS11Initializer;
import lu.nowina.nexu.CancelledOperationException;
import sun.security.pkcs11.SunPKCS11;
import sun.security.pkcs11.wrapper.CK_C_INITIALIZE_ARGS;
import sun.security.pkcs11.wrapper.PKCS11;
import sun.security.pkcs11.wrapper.PKCS11Constants;

/**
 * PKCS#11 token adapter that translates middleware cancellation errors into the
 * NexU cancellation contract.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
@SuppressWarnings("restriction")
public class Pkcs11SignatureTokenAdapter extends Pkcs11SignatureToken {

    private static final Logger LOG = LoggerFactory.getLogger(Pkcs11SignatureTokenAdapter.class);

    private Provider provider;
    private final int slotListIndex;

    public Pkcs11SignatureTokenAdapter(final File pkcs11lib, final PasswordInputCallback callback,
            final int terminalIndex) {
        // A detected PC/SC terminal is mapped to the PKCS#11 slot-list index, as
        // in the historical NexU implementation. A negative slot id disables the
        // alternative slot-id selector in DSS.
        super(pkcs11lib.getAbsolutePath(), callback, -1, terminalIndex, null);
        this.slotListIndex = terminalIndex;
        LOG.info("Using PKCS#11 library {}", pkcs11lib.getAbsolutePath());
    }

    @Override
    public void close() {
        if (provider == null) {
            return;
        }

        try {
            if (provider instanceof AuthProvider) {
                ((AuthProvider) provider).logout();
            }

            // Some PKCS#11 middleware cannot be reopened in the same JVM unless
            // the native module is finalized and removed from the JDK cache.
            final Class<?> sunPkcs11ProviderClass = Class.forName("sun.security.pkcs11.SunPKCS11");
            if (provider instanceof SunPKCS11 || provider.getClass().equals(sunPkcs11ProviderClass)) {
                final CK_C_INITIALIZE_ARGS initArgs = new CK_C_INITIALIZE_ARGS();
                initArgs.flags = CKF_OS_LOCKING_OK;
                final PKCS11 pkcs11 = PKCS11.getInstance(getPkcs11Path(), "C_GetFunctionList", initArgs, true);
                pkcs11.C_Finalize(PKCS11Constants.NULL_PTR);

                final Field moduleMapField = PKCS11.class.getDeclaredField("moduleMap");
                moduleMapField.setAccessible(true);
                ((Map<?, ?>) moduleMapField.get(null)).remove(getPkcs11Path());
            }
        } catch (final LoginException e) {
            LOG.error("Unable to log out from PKCS#11 provider '{}'", provider.getName(), e);
        } catch (final Throwable e) {
            // The cleanup uses JDK-internal APIs and is best effort. Failure must
            // not keep the application from releasing the Java provider.
            LOG.error("Unable to finalize PKCS#11 provider '{}'", provider.getName(), e);
        } finally {
            provider.clear();
            try {
                Security.removeProvider(provider.getName());
            } catch (final SecurityException e) {
                LOG.error("Unable to remove PKCS#11 provider '{}'", provider.getName(), e);
            } finally {
                provider = null;
            }
        }
    }

    @Override
    protected Provider getProvider() {
        if (provider == null) {
            String libraryPath = escapePath(getPkcs11Path());
            final StringBuilder config = new StringBuilder();
            config.append("name = SmartCard").append(UUID.randomUUID()).append('\n');
            config.append("library = \"").append(libraryPath).append("\"").append('\n');
            config.append("slotListIndex = ").append(slotListIndex);

            final String configString = config.toString();
            LOG.debug("PKCS11 Config :\n{}", configString);
            provider = SunPKCS11Initializer.getProvider(configString);
            if (provider == null) {
                throw new DSSException("Unable to create PKCS11 provider");
            }
            Security.addProvider(provider);
        }
        return provider;
    }

    @Override
    public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
        try {
            return super.getKeys();
        } catch (final RuntimeException e) {
            rethrowIfCancelled(e);
            throw e;
        }
    }

    @Override
    public SignatureValue sign(final ToBeSigned toBeSigned, final DigestAlgorithm digestAlgorithm,
            final DSSPrivateKeyEntry keyEntry) throws DSSException {
        try {
            return super.sign(toBeSigned, digestAlgorithm, keyEntry);
        } catch (final RuntimeException e) {
            rethrowIfCancelled(e);
            throw e;
        }
    }

    @Override
    public SignatureValue sign(final ToBeSigned toBeSigned, final SignatureAlgorithm signatureAlgorithm,
            final DSSPrivateKeyEntry keyEntry) throws DSSException {
        try {
            return super.sign(toBeSigned, signatureAlgorithm, keyEntry);
        } catch (final RuntimeException e) {
            rethrowIfCancelled(e);
            throw e;
        }
    }

    @Override
    public SignatureValue signDigest(final Digest digest, final DSSPrivateKeyEntry keyEntry) throws DSSException {
        try {
            return super.signDigest(digest, keyEntry);
        } catch (final RuntimeException e) {
            rethrowIfCancelled(e);
            throw e;
        }
    }

    @Override
    public SignatureValue signDigest(final Digest digest, final SignatureAlgorithm signatureAlgorithm,
            final DSSPrivateKeyEntry keyEntry) throws DSSException {
        try {
            return super.signDigest(digest, signatureAlgorithm, keyEntry);
        } catch (final RuntimeException e) {
            rethrowIfCancelled(e);
            throw e;
        }
    }

    private void rethrowIfCancelled(final Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof CancelledOperationException) {
                throw (CancelledOperationException) current;
            }
            if ("CKR_CANCEL".equals(current.getMessage())
                    || "CKR_FUNCTION_CANCELED".equals(current.getMessage())) {
                throw new CancelledOperationException(error);
            }
            current = current.getCause();
        }
    }
}

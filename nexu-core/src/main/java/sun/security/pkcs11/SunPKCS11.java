package sun.security.pkcs11;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @href https://github.com/DDoSolitary/SunPKCS11Wrapper
	# SunPKCS11Wrapper
	
	Starting from OpenJDK 9, the `sun.security.pkcs11.SunPKCS11` class no longer provides the constructor accepting a configuration file, and instead, a new `configure()` method is added to the `java.security.Provider` class to support the same functionality. 
	However, the `apksigner` tool from Android SDK doesn't support this new interface and will crash when trying to instantiate the `SunPKCS11` class.
	
	This project provides a wrapper class with the old constructor interface, making `apksigner` compatible with OpenJDK 9 and later releases.
	
	## Usage
	
	1. [Download `SunPKCS11Wrapper.jar`](https://ddosolitary-builds.sourceforge.io/SunPKCS11Wrapper.jar) and copy it to `<sdk-path>/build-tools/<version>/lib/`.
	
	2. **Linux**:  
	   In the last line of the file `<sdk-path>/build-tools/<version>/apksigner` (which should be an `exec` command), replace `-jar "$jarpath"` with `-cp "$jarpath:$libdir/SunPKCS11Wrapper.jar" com.android.apksigner.ApkSignerTool`.
	
	   **Windows**:  
	   In the last line of the file `<sdk-path>/build-tools/<version>/apksigner.bat` (which should be a `call` command), replace `-jar "%jarpath%"` with `-cp "%jarpath%;%frameworkdir%\SunPKCS11Wrapper.jar" com.android.apksigner.ApkSignerTool`.
	
	3. Run apksigner as usual, but pass `org.ddosolitary.pkcs11.SunPKCS11Wrapper` instead of `sun.security.pkcs11.SunPKCS11` to the `--provider-class` option.  
	   For example:  
	   ```bash
	   apksigner sign \
	       --ks NONE \
	       --ks-key-alias <YOUR_KEY_ALIAS> \
	       --ks-type PKCS11
	       --provider-class org.ddosolitary.pkcs11.SunPKCS11Wrapper \
	       --provider-arg <PATH_TO_CONFIG_FILE> \
	       <PATH_TO_APK_FILE>
	   ```
	
	## See also
	
	http://mail.openjdk.java.net/pipermail/jep-changes/2015-November/000219.html
	
	https://issuetracker.google.com/issues/132333137
*/
public final class SunPKCS11 extends Provider {
	@SuppressWarnings("unchecked")
	public SunPKCS11(String cfg) {
		super(
			"sun.security.pkcs11.SunPKCS11",
			11,
			"A wrapper for SunPKCS11 providing compatibility with Java 8"
		);
		
		//Provider provider = Security.getProvider("SunPKCS11").configure(cfg);
		Provider provider = Security.getProvider("SunPKCS11");
		provider.forEach(this::put);
		provider.getServices().forEach(s -> {
			try {
				Method aliasMethod = Provider.Service.class.getDeclaredMethod("getAliases");
				aliasMethod.setAccessible(true);
				Field attrField = Provider.Service.class.getDeclaredField("attributes");
				attrField.setAccessible(true);
				Map<String, String> attrs = ((Map<Object, String>)attrField.get(s))
					.entrySet().stream().collect(Collectors.toMap(
						e -> e.getKey().toString(),
						Map.Entry::getValue
					));
				putService(new ProviderService(this, s, (List<String>)aliasMethod.invoke(s), attrs));
			} catch (Exception ignored) {}
		});
	}

	private static final class ProviderService extends Provider.Service {
		private final Service service;

		public ProviderService(
			Provider provider,
			Provider.Service service,
			List<String> aliases,
			Map<String, String> attributes
		) {
			super(
				provider,
				service.getType(),
				service.getAlgorithm(),
				service.getClassName(),
				aliases,
				attributes
			);
			this.service = service;
		}

		@Override
		public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
			return service.newInstance(constructorParameter);
		}

		@Override
		public boolean supportsParameter(Object parameter) {
			return service.supportsParameter(parameter);
		}
	}
}

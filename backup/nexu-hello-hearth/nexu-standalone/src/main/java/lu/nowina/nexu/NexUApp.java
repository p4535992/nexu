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
package lu.nowina.nexu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import lu.nowina.nexu.NexUPreLoader.PreloaderMessage;
import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.plugin.InitializationMessage;
import lu.nowina.nexu.flow.BasicFlowRegistry;
import lu.nowina.nexu.flow.Flow;
import lu.nowina.nexu.flow.FlowRegistry;
import lu.nowina.nexu.flow.operation.BasicOperationFactory;
import lu.nowina.nexu.generic.SCDatabase;
import lu.nowina.nexu.view.core.UIDisplay;

public class NexUApp extends Application {

	private static final Logger logger = LoggerFactory.getLogger(NexUApp.class.getName());

	private HttpServer server;
	
	private AppConfig getConfig() {
		return NexuLauncher.getConfig();
	}

	private Properties getProperties() {
		return NexuLauncher.getProperties();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Platform.setImplicitExit(false);

		final StandaloneUIDisplay uiDisplay = new StandaloneUIDisplay(getConfig());
		final OperationFactory operationFactory = new BasicOperationFactory();
		((BasicOperationFactory)operationFactory).setDisplay(uiDisplay);
		uiDisplay.setOperationFactory(operationFactory);
		
		final NexuAPI api = buildAPI(uiDisplay, operationFactory);

		logger.info("Start Jetty");

		server = startHttpServer(api);

		if(api.getAppConfig().isEnableSystrayMenu()) {
			new SystrayMenu(operationFactory, api, new UserPreferences(getConfig().getApplicationName()));
		} else {
			logger.info("Systray menu is disabled.");
		}

		logger.info("Start finished");
	}
      
      private void exportDefaultATRDriverStore(String path) {
         InputStream stream = null;
         FileOutputStream resStreamOut = null;
         String jarFolder;
         try {
             stream = NexUApp.class.getResourceAsStream("/store.xml");//note that each / is a directory down in the "jar tree" been the jar the root of the tree
             if(stream == null) {
                 throw new Exception("Cannot get resource \"store.xml\" from Jar file.");
             }

             resStreamOut = new FileOutputStream(path);
             int readBytes;
             byte[] buffer = new byte[4096];
             while ((readBytes = stream.read(buffer)) > 0) {
                 resStreamOut.write(buffer, 0, readBytes);
             }
         } catch (Exception ex) {
             logger.error("Cannot populate store.xml - starting with empty");
         } finally {
             try { if (stream != null) { stream.close(); } } catch (Exception ex) {}
             try { if (resStreamOut != null) { resStreamOut.close(); } } catch (Exception ex) {}
         }         
      }

	private NexuAPI buildAPI(final UIDisplay uiDisplay, final OperationFactory operationFactory) throws IOException {
		File nexuHome = getConfig().getNexuHome();
		SCDatabase db = null;
		if (nexuHome != null) {
			File store = new File(nexuHome, "store.xml");
                  
                  // unisystems change - provide the default store with known driver locations (these have to be installed first anyway)
                  if (store != null && !store.exists()) {
                     exportDefaultATRDriverStore(store.getPath());
                  }
                  
			logger.info("Load database from " + store.getAbsolutePath());
			db = ProductDatabaseLoader.load(SCDatabase.class, store);
		} else {
			db = new SCDatabase();
		}
		final APIBuilder builder = new APIBuilder();
		final NexuAPI api = builder.build(uiDisplay, getConfig(), getFlowRegistry(), db, operationFactory);
		notifyPreloader(builder.initPlugins(api, getProperties()));
		return api;
	}

	/**
	 * Returns the {@link FlowRegistry} to use to resolve {@link Flow}s.
	 * @return The {@link FlowRegistry} to use to resolve {@link Flow}s.
	 */
	protected FlowRegistry getFlowRegistry() {
		return new BasicFlowRegistry();
	}
	
	private HttpServer startHttpServer(NexuAPI api) throws Exception {
		final HttpServer server = buildHttpServer();
		server.setConfig(api);
		try {
			server.start();
		} catch(Exception e) {
			try {
				server.stop();
			} catch(Exception e1) {}
			throw e;
		}
		return server;
	}

	/**
	 * Build the HTTP Server for the platform
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private HttpServer buildHttpServer() {
		try {
			Class<HttpServer> cla = (Class<HttpServer>) Class.forName(getConfig().getHttpServerClass());
			logger.info("HttpServer is " + getConfig().getHttpServerClass());
			HttpServer server = cla.newInstance();
			return server;
		} catch (Exception e) {
			logger.error("Cannot instanciate Http Server " + getConfig().getHttpServerClass(), e);
			throw new RuntimeException("Cannot instanciate Http Server");
		}
	}

	@Override
	public void stop() throws Exception {
		logger.info("Stopping application...");
		try {
			if(server != null) {
				server.stop();
				server = null;
			}
		} catch (final Exception e) {
			logger.error("Cannot stop server", e);
		}
	}

	private void notifyPreloader(final List<InitializationMessage> messages) {
		for(final InitializationMessage message : messages) {
			final AlertType alertType;
			switch(message.getMessageType()) {
			case WARNING:
				alertType = AlertType.WARNING;
				break;
			default:
				throw new IllegalArgumentException("Unknown message type: " + message.getMessageType());	
			}
			final PreloaderMessage preloaderMessage = new PreloaderMessage(alertType, message.getTitle(),
					message.getHeaderText(), message.getContentText());
			notifyPreloader(preloaderMessage);
		}
	}
}

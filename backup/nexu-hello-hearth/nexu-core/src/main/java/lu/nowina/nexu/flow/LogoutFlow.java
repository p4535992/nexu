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
// Unisystems change: created new logout flow
package lu.nowina.nexu.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.token.SignatureTokenConnection;
import lu.nowina.nexu.InternalAPI;
import lu.nowina.nexu.NexuException;
import lu.nowina.nexu.api.Execution;
import lu.nowina.nexu.api.LogoutRequest;
import lu.nowina.nexu.api.LogoutResponse;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.SignatureResponse;
import lu.nowina.nexu.api.TokenId;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.operation.GetTokenConnectionOperation;
import lu.nowina.nexu.flow.operation.GetTokenOperation;
import lu.nowina.nexu.flow.operation.TokenOperationResultKey;
import lu.nowina.nexu.view.core.UIDisplay;
import lu.nowina.nexu.view.core.UIOperation;

class LogoutFlow extends AbstractCoreFlow<LogoutRequest, LogoutResponse> {

   private static final Logger logger = LoggerFactory.getLogger(LogoutFlow.class.getName());

   public LogoutFlow(UIDisplay display, NexuAPI api) {
      super(display, api);
   }
   
   // we have three cases:
   // 1) logout called manually when error occurs outside NexU - must clear cache, must close token
   // 2) logout called after getCertificates - must NOT clear cache, must NOT close token
   // 3) logout called after signing - must NOT clear cache for 1st etc. document, must close token, MUST clear cache for the last document
   // 4) logout should be called manually when errors occur inside NexU operations, like in 1)
   // 5) logout should be called even if token/tokenId are null, in case when cache should be cleared anyway

   @Override
   @SuppressWarnings("unchecked")
   protected Execution<LogoutResponse> process(NexuAPI api, LogoutRequest req) throws Exception {
       SignatureTokenConnection token = null;
       try {
           if (req.getTokenId() != null) {

               // try to connect to token, if tokenId is provided - in order to be able to call close() on it in the finally block
               final OperationResult<Map<TokenOperationResultKey, Object>> getTokenOperationResult
                       = getOperationFactory().getOperation(GetTokenOperation.class, api, req.getTokenId()).perform();
               if (getTokenOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                   final Map<TokenOperationResultKey, Object> map = getTokenOperationResult.getResult();
                   final TokenId tokenId = (TokenId) map.get(TokenOperationResultKey.TOKEN_ID);

                   final OperationResult<SignatureTokenConnection> getTokenConnectionOperationResult
                           = getOperationFactory().getOperation(GetTokenConnectionOperation.class, api, tokenId).perform();
                   if (getTokenConnectionOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                       token = getTokenConnectionOperationResult.getResult();
                       logger.info("Retrieved successfully Token " + token);
                   } else {
                       if (api.getAppConfig().isEnablePopUps()) {
                           getOperationFactory().getOperation(UIOperation.class, "/fxml/message.fxml",
                                   "signature.flow.bad.token", api.getAppConfig().getApplicationName()).perform();
                       }
                       return handleErrorOperationResult(getTokenConnectionOperationResult);
                   }
               } else {
                   return handleErrorOperationResult(getTokenOperationResult);
               }
           }
       } catch (Exception e) {
           logger.error("Flow error", e);
           throw handleException(e);
       } finally {
           if (token != null) {
               if (req.isDoCloseToken()) {
                   try {
                       // might have been left open after GetCertificates operation and before Sign operation
                       token.close();
                       logger.info("token.close called");
                   } catch (final Exception e) {
                       logger.error("Exception when closing token", e);
                   }
               } else {
                   logger.info("doCloseToken = false, not checking if token is closed");
               }
           }

           // must be called even if token is null - since we do operations with global api here, regardless of the token
           if (req.isDoClearCache()) {
               try {
                   // this will uncache the selected product (selected token)
                   api.getAppConfig().setDefaultProduct(null);
                   logger.info("reset the default product");

                   if (api instanceof InternalAPI) {
                       ((InternalAPI) api).resetDisplayState(); // will reset the cached password
                       logger.info("called resetDisplayState");
                   }
               } catch (final Exception e) {
                   logger.error("Exception when closing token", e);
               }
           } else {
               logger.info("doClearCache = false, not checking for cached data");
           }
       }
      
       // must not do it as follows because this creates an 500 response
      // return new Execution<LogoutResponse>(BasicOperationStatus.SUCCESS);
      return new Execution<LogoutResponse>(new LogoutResponse());
   }
}

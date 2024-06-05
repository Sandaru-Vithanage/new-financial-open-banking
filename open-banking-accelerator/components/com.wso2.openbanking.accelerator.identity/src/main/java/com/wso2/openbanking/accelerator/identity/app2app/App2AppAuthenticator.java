/**
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wso2.openbanking.accelerator.identity.app2app;

import com.nimbusds.jwt.SignedJWT;
import com.wso2.openbanking.accelerator.common.exception.OpenBankingException;
import com.wso2.openbanking.accelerator.common.util.JWTUtils;
import com.wso2.openbanking.accelerator.identity.app2app.exception.JWTValidationException;
import com.wso2.openbanking.accelerator.identity.app2app.model.DeviceVerificationToken;
import com.wso2.openbanking.accelerator.identity.app2app.utils.App2AppAuthUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authenticator.push.device.handler.DeviceHandler;
import org.wso2.carbon.identity.application.authenticator.push.device.handler.exception.PushDeviceHandlerClientException;
import org.wso2.carbon.identity.application.authenticator.push.device.handler.exception.PushDeviceHandlerServerException;
import org.wso2.carbon.identity.application.authenticator.push.device.handler.impl.DeviceHandlerImpl;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * App2App authenticator for authenticating users from native auth attempt.
 */
public class App2AppAuthenticator extends AbstractApplicationAuthenticator
        implements FederatedApplicationAuthenticator {

    private static final Log log = LogFactory.getLog(App2AppAuthenticator.class);
    private static final long serialVersionUID = -5439464372188473141L;
    private static DeviceHandler deviceHandler;

    /**
     * Constructor for the App2AppAuthenticator.
     */
    public App2AppAuthenticator() {

        if (deviceHandler == null) {
            deviceHandler = new DeviceHandlerImpl();
        }
    }

    /**
     * This method is used to get authenticator name.
     *
     * @return String Authenticator name.
     */
    @Override
    public String getName() {

        return App2AppAuthenticatorConstants.AUTHENTICATOR_NAME;
    }

    /**
     * This method is used to get the friendly name of the authenticator.
     *
     * @return String Friendly name of the authenticator
     */
    @Override
    public String getFriendlyName() {

        return App2AppAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
    }

    /**
     * This method processes the authentication response received from the client.
     * It verifies the authenticity of the received JWT token, extracts necessary information,
     * and performs validations before authenticating the user.
     *
     * @param httpServletRequest     The HTTP servlet request object containing the authentication response.
     * @param httpServletResponse    The HTTP servlet response object for sending responses.
     * @param authenticationContext  The authentication context containing information related to the authentication
     *         process.
     * @throws AuthenticationFailedException If authentication fails due to various reasons such as missing parameters,
     *         parsing errors, JWT validation errors, or exceptions during authentication process.
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse,
                                                 AuthenticationContext authenticationContext)
            throws AuthenticationFailedException {

        authenticationContext.setCurrentAuthenticator(App2AppAuthenticatorConstants.AUTHENTICATOR_NAME);
        String jwtString =
                httpServletRequest.getParameter(App2AppAuthenticatorConstants.DEVICE_VERIFICATION_TOKEN_IDENTIFIER);
        String request =
                httpServletRequest.getParameter(App2AppAuthenticatorConstants.REQUEST);

        try {
            SignedJWT signedJWT = JWTUtils.getSignedJWT(jwtString);
            DeviceVerificationToken deviceVerificationToken = new DeviceVerificationToken(signedJWT);
            //Extracting deviceId and loginHint is necessary to retrieve the public key
            String loginHint = deviceVerificationToken.getLoginHint();
            String deviceID = deviceVerificationToken.getDeviceId();

            //Checking whether deviceId and loginHint present in passed jwt
            if (StringUtils.isBlank(loginHint) || StringUtils.isBlank(deviceID)) {
                if (log.isDebugEnabled()) {
                    log.debug(App2AppAuthenticatorConstants.REQUIRED_PARAMS_MISSING_MESSAGE);
                }
                throw new AuthenticationFailedException(App2AppAuthenticatorConstants.REQUIRED_PARAMS_MISSING_MESSAGE);
            }

            AuthenticatedUser userToBeAuthenticated =
                    App2AppAuthUtils.getAuthenticatedUserFromSubjectIdentifier(loginHint);
            String publicKey = getPublicKeyByDeviceID(deviceID, userToBeAuthenticated);
            deviceVerificationToken.setPublicKey(publicKey);
            deviceVerificationToken.setRequestObject(request);
            // setting the user is mandatory for data publishing purposes
            //If exception is thrown before setting a user data publishing will encounter exceptions
            authenticationContext.setSubject(userToBeAuthenticated);
            /*
                if validations are failed it will throw a JWTValidationException and flow will be interrupted.
                Hence, user Authentication will fail.
             */
            App2AppAuthUtils.validateToken(deviceVerificationToken);
            //If the flow is not interrupted user will be authenticated.
            if (log.isDebugEnabled()) {
                log.debug(String.format(App2AppAuthenticatorConstants.USER_AUTHENTICATED_MSG,
                        userToBeAuthenticated.getUserName()));
            }
        } catch (ParseException e) {
            log.error(e.getMessage());
            throw new AuthenticationFailedException(App2AppAuthenticatorConstants.PARSE_EXCEPTION_MESSAGE, e);
        } catch (JWTValidationException e) {
            log.error(e.getMessage());
            throw new AuthenticationFailedException
                    (App2AppAuthenticatorConstants.APP_AUTH_IDENTIFIER_VALIDATION_EXCEPTION_MESSAGE, e);
        } catch (OpenBankingException e) {
            log.error(e.getMessage());
            throw new AuthenticationFailedException(App2AppAuthenticatorConstants.OPEN_BANKING_EXCEPTION_MESSAGE, e);
        } catch (PushDeviceHandlerServerException e) {
            log.error(e.getMessage());
            throw new AuthenticationFailedException
                    (App2AppAuthenticatorConstants.PUSH_DEVICE_HANDLER_SERVER_EXCEPTION_MESSAGE, e);
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            throw new AuthenticationFailedException(App2AppAuthenticatorConstants.USER_STORE_EXCEPTION_MESSAGE, e);
        } catch (PushDeviceHandlerClientException e) {
            log.error(e.getMessage());
            throw new AuthenticationFailedException
                    (App2AppAuthenticatorConstants.PUSH_DEVICE_HANDLER_CLIENT_EXCEPTION_MESSAGE, e);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            throw new
                    AuthenticationFailedException(App2AppAuthenticatorConstants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE, e);
        }
    }

    /**
     * Determines whether this authenticator can handle the incoming HTTP servlet request.
     * This method checks if the request contains the necessary parameter for App2App authentication,
     * which is the device verification token identifier.
     *
     * @param httpServletRequest The HTTP servlet request object to be checked for handling.
     * @return True if this authenticator can handle the request, false otherwise.
     */
    @Override
    public boolean canHandle(HttpServletRequest httpServletRequest) {

        /*
        App2App authenticates the user in one step depending on the app_auth_key,
        Hence it's mandatory to have the required parameter app_auth_key.
         */
        return StringUtils.isNotBlank(httpServletRequest.getParameter(
                App2AppAuthenticatorConstants.DEVICE_VERIFICATION_TOKEN_IDENTIFIER));
    }

    /**
     * Retrieves the context identifier(sessionDataKey in this case) from the HTTP servlet request.
     *
     * @param request The HTTP servlet request object from which to retrieve the context identifier.
     * @return The context identifier extracted from the request, typically representing session data key.
     */
    @Override
    public String getContextIdentifier(HttpServletRequest request) {

        return request.getParameter(App2AppAuthenticatorConstants.SESSION_DATA_KEY);
    }

    /**
     * Initiates the authentication request, but App2App authenticator does not support this operation.
     * Therefore, this method terminates the authentication process and throws an AuthenticationFailedException.
     *
     * @param request  The HTTP servlet request object.
     * @param response The HTTP servlet response object.
     * @param context  The authentication context.
     * @throws AuthenticationFailedException if this method is called
     */
    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 AuthenticationContext context)
            throws AuthenticationFailedException {

        /*
            App2App authenticator does not support initiating authentication request,
            Hence authentication process will be terminated.
         */
        log.error(App2AppAuthenticatorConstants.INITIALIZATION_ERROR_MESSAGE);
        throw new AuthenticationFailedException(
                App2AppAuthenticatorConstants.DEVICE_VERIFICATION_TOKEN_MISSING_ERROR_MESSAGE);
    }

    /**
     * Retrieves the public key associated with a device and user.
     *
     * @param deviceID    The identifier of the device for which the public key is requested.
     * @param authenticatedUser  the authenticated user for this request
     * @return            The public key associated with the specified device and user.
     * @throws UserStoreException                If an error occurs while accessing user store.
     * @throws PushDeviceHandlerServerException  If an error occurs on the server side of the push device handler.
     * @throws PushDeviceHandlerClientException  If an error occurs on the client side of the push device handler.
     */
    private String getPublicKeyByDeviceID(String deviceID, AuthenticatedUser authenticatedUser)
            throws UserStoreException, PushDeviceHandlerServerException, PushDeviceHandlerClientException,
            OpenBankingException {

        UserRealm userRealm = App2AppAuthUtils.getUserRealm(authenticatedUser);
        String userID = App2AppAuthUtils.getUserIdFromUsername(authenticatedUser.getUserName(), userRealm);
        return App2AppAuthUtils.getPublicKey(deviceID, userID, deviceHandler);
    }
}


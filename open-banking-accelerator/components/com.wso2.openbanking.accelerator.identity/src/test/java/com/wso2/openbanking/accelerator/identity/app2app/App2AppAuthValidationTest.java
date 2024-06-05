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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.wso2.openbanking.accelerator.common.exception.OpenBankingException;
import com.wso2.openbanking.accelerator.common.util.JWTUtils;
import com.wso2.openbanking.accelerator.identity.app2app.cache.JTICache;
import com.wso2.openbanking.accelerator.identity.app2app.exception.JWTValidationException;
import com.wso2.openbanking.accelerator.identity.app2app.model.DeviceVerificationToken;
import com.wso2.openbanking.accelerator.identity.app2app.testutils.App2AppUtilsTestJWTDataProvider;
import com.wso2.openbanking.accelerator.identity.app2app.utils.App2AppAuthUtils;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Date;

/**
 * Test class for unit testing App2AppAuthValidations.
 */
@PrepareForTest({JTICache.class, JWTUtils.class})
@PowerMockIgnore({"javax.net.ssl.*", "jdk.internal.reflect.*"})
public class App2AppAuthValidationTest {

    @Test(dataProviderClass = App2AppUtilsTestJWTDataProvider.class,
            dataProvider = "ValidJWTProvider")
    public void validationTest(String jwtString, String publicKey, String requestObject) throws ParseException,
            OpenBankingException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        //Mocking JTICache and JWTUtils
        PowerMockito.mockStatic(JTICache.class);
        PowerMockito.mockStatic(JWTUtils.class);
        Mockito.when(JTICache.getJtiDataFromCache(Mockito.anyString())).thenReturn(null);
        Mockito.when(JWTUtils.isValidSignature(Mockito.any(SignedJWT.class), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(JWTUtils.isValidExpiryTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        Mockito.when(JWTUtils.isValidNotValidBeforeTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        //Creating a new device verification token using signed jwt
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        DeviceVerificationToken deviceVerificationToken = new DeviceVerificationToken(signedJWT);
        deviceVerificationToken.setPublicKey(publicKey);
        deviceVerificationToken.setRequestObject(requestObject);
        // Call the method under test
        App2AppAuthUtils.validateToken(deviceVerificationToken);
    }

    @Test(expectedExceptions = JWTValidationException.class,
            dataProviderClass = App2AppUtilsTestJWTDataProvider.class,
            dataProvider = "ValidJWTProvider")
    public void validationTestJTIReplayed(String jwtString, String publicKey, String requestObject) throws
            ParseException, OpenBankingException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        //Mocking JTICache and JWTUtils
        PowerMockito.mockStatic(JTICache.class);
        PowerMockito.mockStatic(JWTUtils.class);
        Mockito.when(JTICache.getJtiDataFromCache(Mockito.anyString())).thenReturn("NotNullJTI");
        Mockito.when(JWTUtils.isValidSignature(Mockito.any(SignedJWT.class), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(JWTUtils.isValidExpiryTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        Mockito.when(JWTUtils.isValidNotValidBeforeTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        //Creating a new device verification token using signed jwt
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        DeviceVerificationToken deviceVerificationToken = new DeviceVerificationToken(signedJWT);
        deviceVerificationToken.setPublicKey(publicKey);
        deviceVerificationToken.setRequestObject(requestObject);
        // Call the method under test
        App2AppAuthUtils.validateToken(deviceVerificationToken);
    }

    @Test(expectedExceptions = JWTValidationException.class,
            dataProviderClass = App2AppUtilsTestJWTDataProvider.class,
            dataProvider = "ValidJWTProvider")
    public void validationTestJWTExpired(String jwtString, String publicKey, String requestObject) throws
            ParseException, OpenBankingException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        //Mocking JTICache and JWTUtils
        PowerMockito.mockStatic(JTICache.class);
        PowerMockito.mockStatic(JWTUtils.class);
        Mockito.when(JTICache.getJtiDataFromCache(Mockito.anyString())).thenReturn(null);
        Mockito.when(JWTUtils.isValidSignature(Mockito.any(SignedJWT.class), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(JWTUtils.isValidExpiryTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(false);
        Mockito.when(JWTUtils.isValidNotValidBeforeTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        //Creating a new device verification token using signed jwt
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        DeviceVerificationToken deviceVerificationToken = new DeviceVerificationToken(signedJWT);
        deviceVerificationToken.setPublicKey(publicKey);
        deviceVerificationToken.setRequestObject(requestObject);
        // Call the method under test
        App2AppAuthUtils.validateToken(deviceVerificationToken);
    }

    @Test(expectedExceptions = JWTValidationException.class,
            dataProviderClass = App2AppUtilsTestJWTDataProvider.class,
            dataProvider = "ValidJWTProvider")
    public void validationTestJWTNotActive(String jwtString, String publicKey, String requestObject) throws
            ParseException, OpenBankingException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        //Mocking JTICache and JWTUtils
        PowerMockito.mockStatic(JTICache.class);
        PowerMockito.mockStatic(JWTUtils.class);
        Mockito.when(JTICache.getJtiDataFromCache(Mockito.anyString())).thenReturn(null);
        Mockito.when(JWTUtils.isValidSignature(Mockito.any(SignedJWT.class), Mockito.anyString())).
                thenReturn(true);
        Mockito.when(JWTUtils.isValidExpiryTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        Mockito.when(JWTUtils.isValidNotValidBeforeTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(false);
        //Creating a new device verification token using signed jwt
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        DeviceVerificationToken deviceVerificationToken = new DeviceVerificationToken(signedJWT);
        deviceVerificationToken.setPublicKey(publicKey);
        deviceVerificationToken.setRequestObject(requestObject);
        // Call the method under test
        App2AppAuthUtils.validateToken(deviceVerificationToken);
    }

    @Test(expectedExceptions = JWTValidationException.class,
            dataProviderClass = App2AppUtilsTestJWTDataProvider.class,
            dataProvider = "invalidDigestProvider")
    public void validationTestInvalidDigest(String jwtString, String publicKey, String requestObject) throws
            ParseException, OpenBankingException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        //Mocking JTICache and JWTUtils
        PowerMockito.mockStatic(JTICache.class);
        PowerMockito.mockStatic(JWTUtils.class);
        Mockito.when(JTICache.getJtiDataFromCache(Mockito.anyString())).thenReturn(null);
        Mockito.when(JWTUtils.isValidSignature(Mockito.any(SignedJWT.class), Mockito.anyString())).
                thenReturn(true);
        Mockito.when(JWTUtils.isValidExpiryTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        Mockito.when(JWTUtils.isValidNotValidBeforeTime(Mockito.any(Date.class), Mockito.any(long.class)))
                .thenReturn(true);
        //Creating a new device verification token using signed jwt
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        DeviceVerificationToken deviceVerificationToken = new DeviceVerificationToken(signedJWT);
        deviceVerificationToken.setPublicKey(publicKey);
        deviceVerificationToken.setRequestObject(requestObject);
        // Call the method under test
        App2AppAuthUtils.validateToken(deviceVerificationToken);
    }
    @ObjectFactory
    public IObjectFactory getObjectFactory() {

        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }
}


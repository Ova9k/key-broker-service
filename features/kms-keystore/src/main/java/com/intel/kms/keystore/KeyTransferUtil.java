/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.kms.keystore;

import com.intel.dcsg.cpg.crypto.CryptographyException;
import com.intel.dcsg.cpg.crypto.RsaUtil;
import com.intel.dcsg.cpg.io.pem.Pem;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.kms.api.KeyManager;
import com.intel.kms.api.TransferKeyRequest;
import com.intel.kms.api.TransferKeyResponse;
import com.intel.kms.api.fault.NotTrusted;
import com.intel.kms.cipher.PublicKeyReport;
import com.intel.mtwilson.util.crypto.key2.CipherKeyAttributes;
import com.intel.mtwilson.util.validation.faults.Thrown;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.session.UnknownSessionException;

/**
 *
 * @author jbuhacoff
 */
public class KeyTransferUtil {
    final private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KeyTransferUtil.class);
    final private KeyManager keyManager;

    public KeyTransferUtil(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public Pem createPemFromTransferKeyResponse(TransferKeyResponse transferKeyResponse) {
        Pem pem = new Pem("ENCRYPTED SECRET KEY", transferKeyResponse.getKey());
        pem.getHeaders().put("Key-ID", transferKeyResponse.getDescriptor().getContent().getKeyId());
        return pem;
    }
    
    public TransferKeyResponse transferKeyWithRemoteAttestation(String keyId, String context, PublicKey recipientPublicKey, @Context HttpServletRequest request) throws InvalidKeyException, IOException {
        log.debug("transferKeyWithRemoteAttestation with keyId:{}", keyId);
        TransferKeyRequest transferKeyRequest = new TransferKeyRequest(keyId);
        if (recipientPublicKey != null) {
            PublicKeyReport publicKeyReport = new PublicKeyReport(recipientPublicKey);

            if (!publicKeyReport.isPermitted()) {
                throw new InvalidKeyException("Unsupported public key algorithm or key length");
            }
            log.debug("creating transferKeyRequest object");

            if (context != null && !context.isEmpty()) {
                log.debug("setting context intransferKeyRequest:{}", context);
                transferKeyRequest.set("context", context);
            }

            log.debug("Before preparing transferKeyRequest");

            String recipientAlgorithm = publicKeyReport.getAlgorithm();
            Integer recipientKeyBitLength = publicKeyReport.getKeyLength();

            transferKeyRequest.set("recipientPublicKey", recipientPublicKey);

            CipherKeyAttributes wrappingKeyAttributes = new CipherKeyAttributes();
            wrappingKeyAttributes.setAlgorithm(recipientAlgorithm); // for example "RSA"
            wrappingKeyAttributes.setKeyLength(recipientKeyBitLength); // for example, 3072
            wrappingKeyAttributes.setMode("ECB"); // standard for wrapping a key with a public key since it's only one block
            wrappingKeyAttributes.setPaddingMode("OAEP-TCPA"); // indicates use of OAEP with 'TCPA' as the padding parameter
            transferKeyRequest.set("recipientPublicKeyAttributes", wrappingKeyAttributes);
        }
        try {
            log.debug("Before Calling Actual Key Transfer");
            TransferKeyResponse transferKeyResponse = keyManager.transferKey(transferKeyRequest);
            log.debug("After Actual Key Transfer Call");
            return transferKeyResponse;
        } catch (UnauthorizedException | UnauthenticatedException | UnknownSessionException ex) {
            log.error("Exception In Actual Key Transfer Call ", ex);
            TransferKeyResponse transferKeyResponse = new TransferKeyResponse();
            transferKeyResponse.getHttpResponse().setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
            transferKeyResponse.getFaults().add(new NotTrusted("Remote attestation"));
            return transferKeyResponse;
        } catch (Exception ex) {
            log.error("Generic Exception In Actual Key Transfer Call", ex);
            TransferKeyResponse transferKeyResponse = new TransferKeyResponse();
            transferKeyResponse.getHttpResponse().setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
            transferKeyResponse.getFaults().add(new NotTrusted("Remote attestation"));
            return transferKeyResponse;
        }
    }
      

    
    public TransferKeyResponse transferKeyWithRemoteAttestation(@Context HttpServletRequest request, String keyId, String context, String host, PublicKey recipientPublicKey) {

        log.debug("transferKeyWithRemoteAttestation keyId {}, host {}", keyId, host);
        if (host == null || host.isEmpty()) {
            TransferKeyResponse transferKeyResponse = new TransferKeyResponse(null, null);
            transferKeyResponse.getHttpResponse().setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode());
            transferKeyResponse.getFaults().add(new NotTrusted("Recipient must be specified"));
            return transferKeyResponse;
        }

        try {
            // TODO:   call CIT Verifier to get remote attestation
            log.debug("Client is trusted, need to return key now");

            if (recipientPublicKey != null) {
                log.debug("recipientPublicKey in transferKeyWithRemoteAttestation:{}", recipientPublicKey.getEncoded());
            }
            log.debug("Before calling the transferKeyWithRemoteAttestation2");
            return transferKeyWithRemoteAttestation(keyId, context, recipientPublicKey, request);
        } catch (IOException | GeneralSecurityException e) {
            TransferKeyResponse transferKeyResponse = new TransferKeyResponse(null, null);
            transferKeyResponse.getHttpResponse().setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
            transferKeyResponse.getFaults().add(new Thrown(e));
            return transferKeyResponse;
        }
    }

    public static PublicKey getPublicKey(String key) throws CryptographyException, CertificateException {
        if( key.startsWith("-----BEGIN CERTIFICATE-----")) {
            return X509Util.decodePemCertificate(key).getPublicKey();
        }
        else if( key.startsWith("-----BEGIN PUBLIC KEY-----")) {
            return RsaUtil.decodePemPublicKey(key);
        }
        else {
            log.error("Envelope key in unrecognized format: {}", key);
            return null;
        }
    }
}

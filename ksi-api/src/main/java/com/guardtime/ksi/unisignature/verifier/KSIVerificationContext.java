/*
 * Copyright 2013-2016 Guardtime, Inc.
 *
 * This file is part of the Guardtime client SDK.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES, CONDITIONS, OR OTHER LICENSES OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * "Guardtime" and "KSI" are trademarks or registered trademarks of
 * Guardtime, Inc., and no license to trademarks is granted; Guardtime
 * reserves and retains all trademark rights.
 */

package com.guardtime.ksi.unisignature.verifier;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.hashing.DataHash;
import com.guardtime.ksi.pdu.*;
import com.guardtime.ksi.pdu.v1.PduV1Factory;
import com.guardtime.ksi.publication.PublicationData;
import com.guardtime.ksi.publication.PublicationRecord;
import com.guardtime.ksi.publication.PublicationsFile;
import com.guardtime.ksi.publication.inmemory.CertificateNotFoundException;
import com.guardtime.ksi.service.Future;
import com.guardtime.ksi.service.KSIProtocolException;
import com.guardtime.ksi.service.client.KSIExtenderClient;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.unisignature.*;
import com.guardtime.ksi.unisignature.inmemory.InMemoryKsiSignatureComponentFactory;
import com.guardtime.ksi.util.Util;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Verification context implementation.
 *
 * @see VerificationContext
 */
final class KSIVerificationContext implements VerificationContext {

    private PublicationsFile publicationsFile;
    private KSISignature signature;
    private PublicationData userPublication;
    private KSIExtenderClient extenderClient;
    private boolean extendingAllowed;
    private DataHash documentHash;
    private Map<Date, CalendarHashChain> extendedSignatures = new HashMap<Date, CalendarHashChain>();
    private CalendarHashChain calendarExtendedToHead;
    //TODO
    private PduFactory pduFactory = new PduV1Factory();
    private KSISignatureComponentFactory signatureComponentFactory = new InMemoryKsiSignatureComponentFactory();

    KSIVerificationContext(PublicationsFile publicationsFile, KSISignature signature, PublicationData userPublication, boolean extendingAllowed, KSIExtenderClient extenderClient, DataHash documentHash) {
        this.publicationsFile = publicationsFile;
        this.signature = signature;
        this.userPublication = userPublication;
        this.extendingAllowed = extendingAllowed;
        this.extenderClient = extenderClient;
        this.documentHash = documentHash;
    }

    public KSISignature getSignature() {
        return signature;
    }

    public CalendarHashChain getExtendedCalendarHashChain(Date publicationTime) throws KSIException {
        if (publicationTime == null) {
            return getExtendedCalendarHashChain();
        }
        if (!extendedSignatures.containsKey(publicationTime)) {
            extendedSignatures.put(publicationTime, extend(publicationTime));
        }
        return extendedSignatures.get(publicationTime);
    }

    public CalendarHashChain getExtendedCalendarHashChain() throws KSIException {
        if (calendarExtendedToHead == null) {
            calendarExtendedToHead = extend(null);
        }
        return calendarExtendedToHead;
    }

    public AggregationHashChain[] getAggregationHashChains() {
        return getSignature().getAggregationHashChains();
    }

    public CalendarHashChain getCalendarHashChain() {
        return getSignature().getCalendarHashChain();
    }

    public AggregationHashChain getLastAggregationHashChain() {
        AggregationHashChain[] chains = getAggregationHashChains();
        return chains[chains.length - 1];
    }

    public CalendarAuthenticationRecord getCalendarAuthenticationRecord() {
        return getSignature().getCalendarAuthenticationRecord();
    }

    public RFC3161Record getRfc3161Record() {
        return signature.getRfc3161Record();
    }

    public Certificate getCertificate(byte[] certificateId) {
        try {
            return publicationsFile.findCertificateById(certificateId);
        } catch (CertificateNotFoundException e) {
            return null;
        }
    }

    public PublicationRecord getPublicationRecord() {
        return getSignature().getPublicationRecord();
    }

    public PublicationData getUserProvidedPublication() {
        return userPublication;
    }

    public DataHash getDocumentHash() {
        return documentHash;
    }

    public boolean isExtendingAllowed() {
        return extendingAllowed;
    }

    public PublicationsFile getPublicationsFile() {
        return publicationsFile;
    }

    private CalendarHashChain extend(Date publicationTime) throws KSIException {
        KSIRequestContext context = new KSIRequestContext(extenderClient.getServiceCredentials(), Util.nextLong(), PduIdentifiers.getInstanceId(), PduIdentifiers.getInstanceId());
        ExtensionRequest extensionRequest = pduFactory.createExtensionRequest(context, getSignature().getAggregationTime(), publicationTime);
        Future<TLVElement> future = extenderClient.extend(new ByteArrayInputStream(extensionRequest.toByteArray()));
        try {
            TLVElement tlvElement = future.getResult();
            ExtensionResponse extensionResponse = pduFactory.readExtensionResponse(context, tlvElement);
            return signatureComponentFactory.createCalendarHashChain(extensionResponse.getPayload());
            //TODO probably we don't need to catch this
        } catch (com.guardtime.ksi.tlv.TLVParserException e) {
            throw new KSIProtocolException("Can't parse response message", e);
        }
    }

}

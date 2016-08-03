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
package com.guardtime.ksi.pdu.tlv;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.pdu.ExtensionResponse;
import com.guardtime.ksi.tlv.TLVElement;
import com.guardtime.ksi.tlv.TLVStructure;
import com.guardtime.ksi.unisignature.CalendarHashChain;

import java.util.Date;
import java.util.List;

class ExtensionResponsePayload extends TLVStructure implements ExtensionResponse {

    public static final int ELEMENT_TYPE = 0x0302;

    private static final int ELEMENT_TYPE_REQUEST_ID = 0x01;
    private static final int ELEMENT_TYPE_STATUS = 0x04;
    private static final int ELEMENT_TYPE_ERROR_MESSAGE = 0x05;
    private static final int ELEMENT_TYPE_LAST_TIME =  0x10;//0x12; TODO bug in gateway code

    private Long requestId;
    private Long status;
    private String errorMessage;
    private Date lastTime;
    private TLVElement hashChain;

    public ExtensionResponsePayload(TLVElement element) throws KSIException {
        super(element);
        List<TLVElement> children = element.getChildElements();
        for (TLVElement child : children) {
            switch (child.getType()) {
                case ELEMENT_TYPE_REQUEST_ID:
                    this.requestId = readOnce(child).getDecodedLong();
                    continue;
                case ELEMENT_TYPE_STATUS:
                    this.status = readOnce(child).getDecodedLong();
                    continue;
                case ELEMENT_TYPE_ERROR_MESSAGE:
                    this.errorMessage = readOnce(child).getDecodedString();
                    continue;
                case ELEMENT_TYPE_LAST_TIME:
                    this.lastTime = readOnce(child).getDecodedDate();
                    continue;
                case CalendarHashChain.ELEMENT_TYPE:
                    this.hashChain = readOnce(child);
                    continue;
                default:
                    verifyCriticalFlag(child);
            }
        }
    }

    public TLVElement getCalendarHashChainTlvElement() {
        return hashChain;
    }

    public Long getError() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public final Long getRequestId() {
        return requestId;
    }

    public Date getLastTime() {
        return lastTime;
    }

    @Override
    public int getElementType() {
        return ELEMENT_TYPE;
    }

    //TODO rename
    public TLVElement getPayload() {
        return hashChain;
    }
}

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

package com.guardtime.ksi.unisignature.verifier.rules;

import com.guardtime.ksi.exceptions.KSIException;
import com.guardtime.ksi.unisignature.verifier.VerificationContext;
import com.guardtime.ksi.unisignature.verifier.VerificationErrorCode;
import com.guardtime.ksi.unisignature.verifier.VerificationResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Verifies the index of the RFC3161 record.
 */
public class Rfc3161RecordIndexRule extends BaseRule {

    private static final Logger logger = LoggerFactory.getLogger(Rfc3161RecordIndexRule.class);

    public VerificationResultCode verifySignature(VerificationContext context) throws KSIException {
        if (context.getRfc3161Record() != null) {
            return verifyIndexes(context.getRfc3161Record().getChainIndex(), context.getAggregationHashChains()[0].getChainIndex());
        }
        return VerificationResultCode.OK;
    }

    private VerificationResultCode verifyIndexes(List<Long> rfc3161ChainIndex, List<Long> aggregationChainIndex) {
        if (rfc3161ChainIndex.size() != aggregationChainIndex.size()) {
            logger.info("Aggregation hash chain and RFC3161 chain index mismatch. Aggregation chain index length is {}, RFC3161 chain index length is {}", aggregationChainIndex.size(), rfc3161ChainIndex.size());
            return VerificationResultCode.FAIL;
        }
        for (int i = 0; i < rfc3161ChainIndex.size(); i++) {
            Long rfc3161index = rfc3161ChainIndex.get(i);
            Long aggregationIndex = aggregationChainIndex.get(i);
            if (!rfc3161index.equals(aggregationIndex)) {
                logger.info("Aggregation hash chain and RFC3161 chain index mismatch. At position {} aggregation index value is {} and RFC3161 index value is {}", i, aggregationIndex, rfc3161index);
                return VerificationResultCode.FAIL;
            }
        }
        return VerificationResultCode.OK;
    }

    public VerificationErrorCode getErrorCode() {
        return VerificationErrorCode.INT_12;
    }
}

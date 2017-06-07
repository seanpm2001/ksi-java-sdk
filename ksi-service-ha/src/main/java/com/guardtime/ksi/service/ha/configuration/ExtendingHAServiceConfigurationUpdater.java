/*
 * Copyright 2013-2017 Guardtime, Inc.
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
package com.guardtime.ksi.service.ha.configuration;

import com.guardtime.ksi.pdu.ExtenderConfiguration;
import com.guardtime.ksi.service.KSIExtendingService;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles configuration consolidation and listener updates for ExtendingHAService
 */
public class ExtendingHAServiceConfigurationUpdater extends AbstractConfigurationUpdater<ExtenderConfiguration> {

    private final List<SubServiceConfListener<ExtenderConfiguration>> subServiceConfListeners = new ArrayList<SubServiceConfListener<ExtenderConfiguration>>();
    private final List<KSIExtendingService> subservices;

    public ExtendingHAServiceConfigurationUpdater(List<KSIExtendingService> subservices) {
        this.subservices = subservices;
        for (KSIExtendingService subservice : subservices) {
            SubServiceConfListener<ExtenderConfiguration> listener = new SubServiceConfListener<ExtenderConfiguration>(subservice.toString(), new SubconfUpdateListener() {
                public void updated() {
                    recalculateConfiguration();
                }
            });
            subservice.registerExtenderConfigurationListener(listener);
            subServiceConfListeners.add(listener);
        }
    }

    protected ExtenderConfiguration consolidate(ExtenderConfiguration c1, ExtenderConfiguration c2) {
        boolean c1Exists = c1 != null;
        boolean c2Exists = c2 != null;
        if (c1Exists && c2Exists) return new ExtendingHAServiceConfiguration(c1, c2);
        if (c1Exists) return new ExtendingHAServiceConfiguration(c1);
        if (c2Exists) return new ExtendingHAServiceConfiguration(c2);
        return null;
    }

    List<SubServiceConfListener<ExtenderConfiguration>> getSubServiceConfListeners() {
        return subServiceConfListeners;
    }


    public void sendAggregationConfigurationRequest() {
        for (KSIExtendingService service : subservices) {
            service.sendExtenderConfigurationRequest();
        }
    }
}

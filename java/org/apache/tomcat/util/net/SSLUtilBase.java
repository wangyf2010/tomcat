/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.res.StringManager;

/**
 * Common base class for {@link SSLUtil} implementations.
 */
public abstract class SSLUtilBase implements SSLUtil {

    private static final StringManager sm = StringManager.getManager(SSLUtilBase.class);

    protected final SSLHostConfigCertificate certificate;

    private final String[] enabledProtocols;
    private final String[] enabledCiphers;


    protected SSLUtilBase(SSLHostConfigCertificate certificate) {
        this.certificate = certificate;
        SSLHostConfig sslHostConfig = certificate.getSSLHostConfig();

        // Calculate the enabled protocols
        Set<String> configuredProtocols = sslHostConfig.getProtocols();
        Set<String> implementedProtocols = getImplementedProtocols();
        List<String> enabledProtocols =
                getEnabled("protocols", true, configuredProtocols, implementedProtocols);
        this.enabledProtocols = enabledProtocols.toArray(new String[enabledProtocols.size()]);

        // Calculate the enabled ciphers
        List<String> configuredCiphers = sslHostConfig.getJsseCipherNames();
        Set<String> implementedCiphers = getImplementedCiphers();
        List<String> enabledCiphers =
                getEnabled("ciphers", false, configuredCiphers, implementedCiphers);
        this.enabledCiphers = enabledCiphers.toArray(new String[enabledCiphers.size()]);
    }


    private <T> List<T> getEnabled(String name, boolean warnOnSkip, Collection<T> configured,
            Collection<T> implemented) {

        List<T> enabled = new ArrayList<>();

        if (implemented.size() == 0) {
            // Unable to determine the list of available protocols. This will
            // have been logged previously.
            // Use the configuredProtocols and hope they work. If not, an error
            // will be generated when the list is used. Not ideal but no more
            // can be done at this point.
            enabled.addAll(configured);
        } else {
            enabled.addAll(implemented);
            enabled.retainAll(configured);

            if (enabled.isEmpty()) {
                // Don't use the defaults in this case. They may be less secure
                // than the configuration the user intended.
                // Force the failure of the connector
                throw new IllegalArgumentException(
                        sm.getString("sslUtilBase.noneSupported", name, configured));
            }
            if (getLog().isDebugEnabled()) {
                getLog().debug(sm.getString("sslUtilBase.active", name, enabled));
            }
            if (getLog().isDebugEnabled() || warnOnSkip) {
                if (enabled.size() != configured.size()) {
                    List<T> skipped = new ArrayList<>();
                    skipped.addAll(configured);
                    skipped.removeAll(enabled);
                    String msg = sm.getString("sslUtilBase.skipped", name, skipped);
                    if (warnOnSkip) {
                        getLog().warn(msg);
                    } else {
                        getLog().debug(msg);
                    }
                }
            }
        }

        return enabled;
    }

    @Override
    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    @Override
    public String[] getEnabledCiphers() {
        return enabledCiphers;
    }

    protected abstract Set<String> getImplementedProtocols();
    protected abstract Set<String> getImplementedCiphers();
    protected abstract Log getLog();
}

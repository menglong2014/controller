/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.shutdown.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.osgi.framework.Bundle;

import javax.management.JMX;
import javax.management.ObjectName;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.config.yang.shutdown.impl.ShutdownModuleFactory.NAME;

public class ShutdownTest extends AbstractConfigTest {
    private final ShutdownModuleFactory factory = new ShutdownModuleFactory();
    @Mock
    private Bundle mockedSysBundle;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ModuleFactoriesResolver factoriesResolver = new HardcodedModuleFactoriesResolver(mockedContext, factory);
        super.initConfigTransactionManagerImpl(factoriesResolver);
        doReturn(mockedSysBundle).when(mockedContext).getBundle(0);
        mockedContext.getBundle(0);
        doNothing().when(mockedSysBundle).stop();
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        // initialize default instance
        transaction.commit();
    }

    @Test
    public void testSingleton_invalidName() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        try {
            transaction.createModule(NAME, "foo");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Singleton enforcement failed. Expected instance name shutdown", e.getMessage());
        }
    }

    private static final ObjectName runtimeON = ObjectNameUtil.createRuntimeBeanName(NAME, NAME, Collections.<String, String>emptyMap());

    @Test
    public void testWithoutSecret() throws Exception {
        // test JMX rpc

        ShutdownRuntimeMXBean runtime = configRegistryClient.newMXBeanProxy(runtimeON, ShutdownRuntimeMXBean.class);
        try {
            runtime.shutdown("foo", 60000L, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid secret", e.getMessage());
        }
        runtime.shutdown("", 60000L, null);
        assertStopped();
    }


    @Test
    public void testWithSecret() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName on = transaction.lookupConfigBean(NAME, NAME);
        ShutdownModuleMXBean proxy = transaction.newMXBeanProxy(on, ShutdownModuleMXBean.class);
        String secret = "secret";
        proxy.setSecret(secret);
        transaction.commit();
        shutdownViaRuntimeJMX(secret);
        try {
            ShutdownRuntimeMXBean runtime = JMX.newMXBeanProxy(platformMBeanServer, runtimeON, ShutdownRuntimeMXBean.class);
            runtime.shutdown("foo", 60000L, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid secret", e.getMessage());
        }
    }

    private void shutdownViaRuntimeJMX(String secret) throws Exception {
        // test JMX rpc
        ShutdownRuntimeMXBean runtime = JMX.newMXBeanProxy(platformMBeanServer, runtimeON, ShutdownRuntimeMXBean.class);
        try {
            runtime.shutdown("", 60000L, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid secret", e.getMessage());
        }
        runtime.shutdown(secret, 60000L, null);
        assertStopped();
    }


    private void assertStopped() throws Exception {
        Thread.sleep(2000); // happens on another thread
        verify(mockedSysBundle).stop();
        verifyNoMoreInteractions(mockedSysBundle);
        reset(mockedSysBundle);
        doNothing().when(mockedSysBundle).stop();
    }
}

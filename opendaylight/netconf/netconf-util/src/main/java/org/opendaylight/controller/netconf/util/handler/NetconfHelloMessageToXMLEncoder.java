/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import java.nio.ByteBuffer;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Customized NetconfMessageToXMLEncoder that serializes additional header with
 * session metadata along with
 * {@link org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage}
 * . Used by netconf clients to send information about the user, ip address,
 * protocol etc.
 * <p/>
 * Hello message with header example:
 * <p/>
 *
 * <pre>
 * {@code
 * [tomas;10.0.0.0/10000;tcp;1000;1000;;/home/tomas;;]
 * <hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
 * <capabilities>
 * <capability>urn:ietf:params:netconf:base:1.0</capability>
 * </capabilities>
 * </hello>
 * }
 * </pre>
 */
public final class NetconfHelloMessageToXMLEncoder extends NetconfMessageToXMLEncoder {

    @Override
    protected ByteBuffer encodeMessage(NetconfMessage msg) {
        Preconditions.checkState(msg instanceof NetconfHelloMessage, "Netconf message of type %s expected, was %s",
                NetconfHelloMessage.class, msg.getClass());
        Optional<NetconfHelloMessageAdditionalHeader> headerOptional = ((NetconfHelloMessage) msg)
                .getAdditionalHeader();

        // If additional header present, serialize it along with netconf hello
        // message
        if (headerOptional.isPresent()) {
            byte[] bytesFromHeader = headerOptional.get().toFormattedString().getBytes(Charsets.UTF_8);
            byte[] bytesFromMessage = xmlToString(msg.getDocument()).getBytes(Charsets.UTF_8);

            ByteBuffer byteBuffer = ByteBuffer.allocate(bytesFromHeader.length + bytesFromMessage.length)
                    .put(bytesFromHeader).put(bytesFromMessage);
            byteBuffer.flip();
            return byteBuffer;
        }

        return super.encodeMessage(msg);
    }
}

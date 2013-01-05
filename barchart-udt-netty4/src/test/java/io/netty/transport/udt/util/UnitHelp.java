/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.transport.udt.util;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.barchart.udt.SocketUDT;
import com.barchart.udt.StatusUDT;

/**
 * unit test helper
 */
public final class UnitHelp {

    private static final Logger log = LoggerFactory.getLogger(UnitHelp.class);

    private static final ConcurrentMap<Integer, SocketUDT> //
    socketMap = new ConcurrentHashMap<Integer, SocketUDT>();

    public static void clear(final IntBuffer buffer) {
        for (int index = 0; index < buffer.capacity(); index++) {
            buffer.put(index, 0);
        }
    }

    /**
     * measure ping time to host
     */
    public static long ping(final String host) throws Exception {
        final String name = System.getProperty("os.name").toLowerCase();

        final String command;
        if (name.contains("linux")) {
            command = "ping -c 1 " + host;
        } else if (name.contains("mac os x")) {
            command = "ping -c 1 " + host;
        } else if (name.contains("windows")) {
            command = "ping -n 1 " + host;
        } else {
            throw new Exception("unknown platform");
        }

        final long timeStart = System.currentTimeMillis();

        process(command);

        final long timeFinish = System.currentTimeMillis();

        final long timeDiff = timeFinish - timeStart;

        return timeDiff;
    }

    public static void process(final String command) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(command.split("\\s"));
        final Process process = builder.start();
        process.waitFor();
    }

    /**
     * @return newly allocated address or null for failure
     */
    public static synchronized InetSocketAddress findLocalAddress(
            final String host) {
        ServerSocket socket = null;
        try {
            final InetAddress address = InetAddress.getByName(host);
            socket = new ServerSocket(0, 3, address);
            return (InetSocketAddress) socket.getLocalSocketAddress();
        } catch (final Exception e) {
            log.error("failed to find addess");
            return null;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (final Exception e) {
                    log.error("failed to close socket");
                }
            }
        }
    }

    public static InetSocketAddress hostedSocketAddress(final String host)
            throws Exception {
        for (int k = 0; k < 10; k++) {
            final InetSocketAddress address = findLocalAddress(host);
            if (address == null) {
                Thread.sleep(500);
                continue;
            }
            return address;
        }
        throw new Exception("failed to allocate address");
    }

    /**
     * allocate available local address / port or throw exception
     */
    public static InetSocketAddress localSocketAddress() throws Exception {
        return hostedSocketAddress("localhost");
    }

    public static void logBuffer(final String title, final IntBuffer buffer) {
        for (int index = 0; index < buffer.capacity(); index++) {
            final int value = buffer.get(index);
            if (value == 0) {
                continue;
            }
            log.info(String.format("%s [id: 0x%08x]", title, value));
        }
    }

    public static void logClassPath() {
        final String classPath = System.getProperty("java.class.path");
        final String[] entries = classPath.split(File.pathSeparator);
        final StringBuilder text = new StringBuilder(1024);
        for (final String item : entries) {
            text.append("\n\t");
            text.append(item);
        }
        log.info("\n\t[java.class.path]{}", text);
    }

    public static void logLibraryPath() {
        final String classPath = System.getProperty("java.library.path");
        final String[] entries = classPath.split(File.pathSeparator);
        final StringBuilder text = new StringBuilder(1024);
        for (final String item : entries) {
            text.append("\n\t");
            text.append(item);
        }
        log.info("\n\t[java.library.path]{}", text);
    }

    public static void logOsArch() {
        final StringBuilder text = new StringBuilder(1024);
        text.append("\n\t");
        text.append(System.getProperty("os.name"));
        text.append("\n\t");
        text.append(System.getProperty("os.arch"));
        log.info("\n\t[os/arch]{}", text);
    }

    public static void logSet(final Set<?> set) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final TreeSet<?> treeSet = new TreeSet(set);
        for (final Object item : treeSet) {
            log.info("-> {}", item);
        }
    }

    public static String property(final String name) {
        final String value = System.getProperty(name);
        if (value == null) {
            log.error("property '{}' not defined; terminating", name);
            System.exit(1);
        }
        return value;
    }

    public static int[] randomIntArray(final int length, final int range) {
        final int[] array = new int[length];
        final Random generator = new Random(0);
        for (int i = 0; i < array.length; i++) {
            array[i] = generator.nextInt(range);
        }
        return array;
    }

    public static String randomString() {
        return "" + System.currentTimeMillis();
    }

    public static String randomSuffix(final String name) {
        return name + "-" + System.currentTimeMillis();
    }

    public static void socketAwait(final SocketUDT socket,
            final StatusUDT status) throws Exception {
        while (true) {
            if (socket.status() == status) {
                return;
            } else {
                Thread.sleep(50);
            }
        }
    }

    public static Set<Integer> socketIndexSet(final IntBuffer buffer) {
        final Set<Integer> set = new HashSet<Integer>();
        while (buffer.hasRemaining()) {
            set.add(buffer.get());
        }
        return set;
    }

    public static boolean socketPresent(final SocketUDT socket,
            final IntBuffer buffer) {
        for (int index = 0; index < buffer.capacity(); index++) {
            if (buffer.get(index) == socket.id()) {
                return true;
            }
        }
        return false;
    }

    private UnitHelp() {
    }

}

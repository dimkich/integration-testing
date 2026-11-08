package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.NowSetter;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@link NowSetter} implementation that controls time via libfaketime IPC.
 * <p>
 * Sends timestamp commands to a libfaketime server over a TCP socket.
 * The server must be running with faketime in daemon mode to accept
 * dynamic time updates during test execution. The daemon acknowledges every update
 * only after atomically replacing the shared timestamp file, so a successful
 * {@link #setNow(ZonedDateTime)} guarantees that all instrumented containers already
 * observe the new time.
 * </p>
 *
 * @see NowSetter
 */
public class LibFakeTimeNowSetter implements NowSetter {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
    /** Expected daemon acknowledgement after the timestamp file has been replaced. */
    private static final String ACK = "OK";
    /** How long to wait for the daemon acknowledgement; the daemon only renames a file. */
    private static final int ACK_TIMEOUT_MS = 1_000;

    private static Socket socket;
    private static OutputStream socketStream;
    private static BufferedReader socketReader;

    /**
     * Establishes the TCP connection to the libfaketime daemon.
     *
     * @param host libfaketime daemon host
     * @param port libfaketime daemon port
     */
    @SneakyThrows
    public static void setUp(String host, int port) {
        Socket newSocket = new Socket(host, port);
        newSocket.setTcpNoDelay(true);
        newSocket.setSoTimeout(ACK_TIMEOUT_MS);
        socket = newSocket;
        socketStream = newSocket.getOutputStream();
        socketReader = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * Closes the connection to the libfaketime daemon and releases resources.
     *
     * @throws IOException if closing the socket fails
     */
    public static void tearDown() throws IOException {
        socketReader = null;
        socketStream = null;
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    /**
     * Sends the given instant to the libfaketime daemon as the new "now" value and waits
     * until the daemon confirms that the shared timestamp file has been replaced.
     * The date-time is converted to UTC and formatted for the faketime protocol.
     * No-op if the connection has not been established via {@link #setUp(String, int)}.
     *
     * @param dateTime the moment in time to set as the current time
     * @throws IllegalStateException if the daemon does not acknowledge the update
     */
    @Override
    public void setNow(ZonedDateTime dateTime) {
        if (socketStream == null) {
            return;
        }
        String line = dateTime.withZoneSameInstant(ZoneOffset.UTC).format(FORMATTER) + " x0";
        applyTimestamp(line);
    }

    private static void applyTimestamp(String line) {
        try {
            socketStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            socketStream.flush();
            String ack = socketReader.readLine();
            if (ack == null) {
                throw new IllegalStateException("libfaketime-server closed the connection before acknowledging ["
                        + line + "]");
            }
            if (!ACK.equals(ack.trim())) {
                throw new IllegalStateException("libfaketime-server failed to apply [" + line + "]: " + ack);
            }
        } catch (SocketTimeoutException e) {
            throw new IllegalStateException("libfaketime-server did not acknowledge the time update ["
                    + line + "] within " + ACK_TIMEOUT_MS + " ms; the running image is outdated or unresponsive",
                    e);
        } catch (IOException e) {
            throw new IllegalStateException("Faketime IPC error while applying [" + line + "]", e);
        }
    }
}

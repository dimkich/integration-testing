package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.NowSetter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@link NowSetter} implementation that controls time via libfaketime IPC.
 * <p>
 * Sends timestamp commands to a libfaketime server over a TCP socket.
 * The server must be running with faketime in daemon mode to accept
 * dynamic time updates during test execution.
 * </p>
 *
 * @see NowSetter
 */
@Slf4j
public class LibFakeTimeNowSetter implements NowSetter {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
    private static OutputStream socketStream;

    /**
     * Establishes the TCP connection to the libfaketime daemon.
     *
     * @param host libfaketime daemon host
     * @param port libfaketime daemon port
     */
    @SneakyThrows
    public static void setUp(String host, int port) {
        Socket socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        socketStream = socket.getOutputStream();
    }

    /**
     * Closes the connection to the libfaketime daemon and releases resources.
     *
     * @throws IOException if closing the stream fails
     */
    public static void tearDown() throws IOException {
        if (socketStream != null) {
            socketStream.close();
            socketStream = null;
        }
    }

    /**
     * Sends the given instant to the libfaketime daemon as the new "now" value.
     * The date-time is converted to UTC and formatted for the faketime protocol.
     * No-op if the connection has not been established via {@link #setUp(String, int)}.
     *
     * @param dateTime the moment in time to set as the current time
     */
    @Override
    public void setNow(ZonedDateTime dateTime) {
        if (socketStream == null) {
            return;
        }
        try {
            String timestamp = dateTime.withZoneSameInstant(ZoneOffset.UTC).format(FORMATTER) + " x0\n";
            socketStream.write(timestamp.getBytes(StandardCharsets.UTF_8));
            socketStream.flush();
        } catch (Exception e) {
            log.error("Faketime IPC write error", e);
        }
    }
}

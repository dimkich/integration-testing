package io.github.dimkich.integration.testing.date.time;

import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Configures Testcontainers with libfaketime for deterministic time control.
 * <p>
 * Tracks which containers should receive libfaketime instrumentation based on image name
 * patterns. When {@link LibFakeTimeAdvice} invokes {@link #onEnter}, this class:
 * <ul>
 *   <li>Starts a shared libfaketime-server container (dimkich/libfaketime-server) if needed</li>
 *   <li>Detects the container OS (linux vs alpine) and architecture (x64 vs arm64)</li>
 *   <li>Mounts the appropriate libfaketime shared library from classpath resources</li>
 *   <li>Configures LD_PRELOAD, FAKETIME_TIMESTAMP_FILE, TZ, and related env vars</li>
 *   <li>Shares IPC with the faketime server for dynamic time updates via {@link LibFakeTimeNowSetter}</li>
 * </ul>
 * Supported resource paths: {@code libfaketime/linux_x64.so}, {@code linux_arm64.so},
 * {@code alpine_x64.so}, {@code alpine_arm64.so}.
 * </p>
 *
 * @see LibFakeTimeSetUp
 * @see LibFakeTimeAdvice
 * @see LibFakeTimeNowSetter
 */
@Slf4j
public class LibFakeTimeTracker {
    private static final String FAKETIME_RC = "/dev/shm/faketime.rc";
    private static final String CONTAINER_LIB_PATH = "/tmp/libfaketime.so";
    @SuppressWarnings("OctalInteger")
    private static final int EXEC_PERMISSIONS = 0755;

    private static final List<Pattern> includePatterns = new ArrayList<>();
    private static GenericContainer<?> libFakeTimeServerContainer;
    private static final Object lock = new Object();

    @SuppressWarnings("try")
    private static GenericContainer<?> createFakeTimeServerContainer() {
        return new GenericContainer<>("dimkich/libfaketime-server:latest")
                .withExposedPorts(9999);
    }

    /**
     * Registers regex patterns for container image names to include in libfaketime instrumentation.
     * <p>
     * Only containers whose image name matches at least one pattern will receive libfaketime.
     * Clears any previously registered patterns before applying the new ones.
     * </p>
     *
     * @param patterns regex patterns for image names (e.g. "postgres.*", "redis.*"),
     *                 or {@code null} to exclude all containers
     */
    public static void setUp(String[] patterns) {
        includePatterns.clear();
        if (patterns != null) {
            for (String p : patterns) {
                includePatterns.add(Pattern.compile(p));
            }
        }
    }

    /**
     * Releases all libfaketime resources.
     * <p>
     * Clears include patterns, closes the connection to the libfaketime daemon via
     * {@link LibFakeTimeNowSetter#tearDown()}, and stops the libfaketime-server container.
     * </p>
     *
     * @throws IOException if cleanup fails
     */
    public static void tearDown() throws IOException {
        includePatterns.clear();
        LibFakeTimeNowSetter.tearDown();
        if (libFakeTimeServerContainer != null) {
            libFakeTimeServerContainer.stop();
            libFakeTimeServerContainer = null;
        }
    }

    /**
     * Configures libfaketime on the given container if its image matches the include patterns.
     * <p>
     * Called by {@link LibFakeTimeAdvice} before {@link GenericContainer#start()}. If the image
     * name matches, starts the shared libfaketime-server if needed, selects the appropriate
     * libfaketime binary (linux/alpine, x64/arm64), mounts it, and sets LD_PRELOAD and
     * environment variables. Containers that do not match the patterns are left unchanged.
     * </p>
     *
     * @param container the Testcontainers {@code GenericContainer} about to be started
     * @throws IllegalStateException if no matching libfaketime binary is found in classpath
     */
    public static void onEnter(GenericContainer<?> container) {
        String imageName = container.getDockerImageName();
        if (includePatterns.stream().noneMatch(p -> p.matcher(imageName).matches())) {
            return;
        }

        synchronized (lock) {
            if (libFakeTimeServerContainer == null) {
                try {
                    libFakeTimeServerContainer = createFakeTimeServerContainer();
                    libFakeTimeServerContainer.start();
                    LibFakeTimeNowSetter.setUp(libFakeTimeServerContainer.getHost(),
                            libFakeTimeServerContainer.getMappedPort(9999));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to start libfaketime-server", e);
                }
            }
        }

        InspectImageResponse inspect;
        @Cleanup var imgCmd = container.getDockerClient()
                .inspectImageCmd(imageName);
        inspect = imgCmd.exec();

        String arch = inspect.getArch();
        String imageArch = arch != null ? arch.toLowerCase() : "";
        String osArch = imageArch.contains("arm") || imageArch.contains("aarch") ? "arm64" : "x64";

        String type = detectOsType(container);

        String resource = "libfaketime/" + type + "_" + osArch + ".so";

        if (LibFakeTimeTracker.class.getClassLoader().getResource(resource) == null) {
            log.error("FAILED to find libfaketime binary for container {}. Expected path: {}", imageName, resource);
            log.error("Supported combinations: linux_x64, linux_arm64, alpine_x64, alpine_arm64");
            throw new IllegalStateException("Missing libfaketime binary: " + resource);
        }
        try {
            container.withCreateContainerCmdModifier(cmd ->
                    Objects.requireNonNull(cmd.getHostConfig()).withIpcMode("container:" + libFakeTimeServerContainer.getContainerId())
            );
            container.withCopyFileToContainer(
                    MountableFile.forClasspathResource(resource, EXEC_PERMISSIONS),
                    CONTAINER_LIB_PATH
            );
            container.withEnv("LD_PRELOAD", CONTAINER_LIB_PATH);
            container.withEnv("FAKETIME_TIMESTAMP_FILE", FAKETIME_RC);
            container.withEnv("TZ", "UTC");
            container.withEnv("FAKETIME_NO_CACHE", "1");
            container.withEnv("DONT_FAKE_MONOTONIC", "1");
            log.debug("Applying libfaketime to container: {} (OS: {}, Arch: {})", imageName, type, osArch);
        } catch (Exception e) {
            log.error("Failed to configure libfaketime for container {}", imageName, e);
            throw e;
        }
    }

    /**
     * Detects the OS type (linux or alpine) for choosing the correct libfaketime binary.
     */
    private static String detectOsType(GenericContainer<?> container) {
        String imageName = container.getDockerImageName().toLowerCase();
        if (imageName.contains("alpine") || imageName.contains("musl")) {
            return "alpine";
        }
        if (imageName.contains("debian") || imageName.contains("ubuntu") || imageName.contains("centos")) {
            return "linux";
        }
        return detectByFileSystem(container);
    }

    /**
     * Fallback detection of alpine vs glibc linux by probing for musl loader in the image.
     */
    private static String detectByFileSystem(GenericContainer<?> container) {
        String imageName = container.getDockerImageName();
        String tempId;
        try {
            @Cleanup var createCmd = container.getDockerClient().createContainerCmd(imageName);
            tempId = createCmd.exec().getId();
        } catch (Exception e) {
            return "linux";
        }
        try {
            @Cleanup CopyArchiveFromContainerCmd copyCmd = container.getDockerClient()
                    .copyArchiveFromContainerCmd(tempId, "/lib/ld-musl-x86_64.so.1");
            @Cleanup InputStream is = copyCmd.exec();
            return "alpine";
        } catch (Exception e1) {
            try {
                @Cleanup CopyArchiveFromContainerCmd copyCmd2 = container.getDockerClient()
                        .copyArchiveFromContainerCmd(tempId, "/lib/ld-musl-aarch64.so.1");
                @Cleanup InputStream is2 = copyCmd2.exec();
                return "alpine";
            } catch (Exception e2) {
                return "linux";
            } finally {
                try {
                    @Cleanup var rmCmd = container.getDockerClient().removeContainerCmd(tempId);
                    rmCmd.withForce(true).exec();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
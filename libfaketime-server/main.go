package main

import (
	"bufio"
	"fmt"
	"net"
	"os"
	"strings"
)

const (
	shmPath    = "/dev/shm/faketime.rc"
	shmTmpPath = "/dev/shm/faketime.rc.tmp"
)

func main() {
	fmt.Printf("libfaketime-server: initializing shared memory at %s...\n", shmPath)
	if err := os.WriteFile(shmPath, []byte("+0 x1"), 0666); err != nil {
		fmt.Fprintf(os.Stderr, "FATAL: failed to write initial file: %v\n", err)
		os.Exit(1)
	}

	ln, err := net.Listen("tcp", ":9999")
	if err != nil {
		fmt.Fprintf(os.Stderr, "FATAL: failed to start listener on :9999: %v\n", err)
		os.Exit(1)
	}
	fmt.Println("libfaketime-server: ready and listening on :9999")

	for {
		conn, err := ln.Accept()
		if err != nil {
			fmt.Fprintf(os.Stderr, "ERROR: accept error: %v\n", err)
			continue
		}

		fmt.Printf("libfaketime-server: client connected (%s)\n", conn.RemoteAddr())
		handleConnection(conn)
		fmt.Println("libfaketime-server: client disconnected, waiting for next connection...")
	}
}

func handleConnection(c net.Conn) {
	defer c.Close()
	scanner := bufio.NewScanner(c)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}

		if err := os.WriteFile(shmTmpPath, []byte(line), 0666); err != nil {
			fmt.Fprintf(os.Stderr, "ERROR: failed to write tmp file: %v\n", err)
			continue
		}

		if err := os.Rename(shmTmpPath, shmPath); err != nil {
			fmt.Fprintf(os.Stderr, "ERROR: failed to rename file: %v\n", err)
			continue
		}
	}

	if err := scanner.Err(); err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: connection read error: %v\n", err)
	}
}

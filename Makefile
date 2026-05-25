SERVER_DIR      := libfaketime-server
DOCKER_SRC_DIR  := integration-testing/src/main/docker
JAVA_RES_DIR    := integration-testing/src/main/resources/libfaketime
REPO_URL        := https://github.com/wolfcw/libfaketime.git
SERVER_IMAGE    := dimkich/libfaketime-server:latest

RAW_SHA := $(shell docker run --rm alpine/git ls-remote $(REPO_URL) HEAD)
REMOTE_SHA := $(strip $(firstword $(RAW_SHA)))

fix_path = $(subst /,\,$1)

.PHONY: all build-libfaketime-server build-libfaketime-libs clean prepare-dirs check-sha

all: build-libfaketime-server build-libfaketime-libs

check-sha:
ifeq ($(REMOTE_SHA),)
	$(error "ERROR: Could not fetch SHA from $(REPO_URL). Check your internet connection.")
endif

build-libfaketime-server:
	@echo === Checking Go server image ===
	docker buildx build --platform linux/amd64,linux/arm64 -t $(SERVER_IMAGE) --push $(SERVER_DIR)

build-libfaketime-libs: prepare-dirs check-sha build-glibc-x64 build-musl-x64 build-glibc-arm64 build-musl-arm64 save-version

build-glibc-x64:
	@echo --- Building linux_x64.so (MT version, SHA: $(REMOTE_SHA)) ---
	docker build --platform linux/amd64 \
		--build-arg FAKETIME_SHA=$(REMOTE_SHA) \
		-t ft-glibc-x64 -f $(DOCKER_SRC_DIR)/Dockerfile.glibc .
	docker create --name tmp_x64 ft-glibc-x64
	docker cp tmp_x64:/build/src/libfaketimeMT.so.1 $(call fix_path,$(JAVA_RES_DIR)/linux_x64.so)
	docker rm -f tmp_x64

build-musl-x64:
	@echo --- Building alpine_x64.so (MT version, SHA: $(REMOTE_SHA)) ---
	docker build --platform linux/amd64 \
		--build-arg FAKETIME_SHA=$(REMOTE_SHA) \
		-t ft-musl-x64 -f $(DOCKER_SRC_DIR)/Dockerfile.musl .
	docker create --name tmp_musl_x64 ft-musl-x64
	docker cp tmp_musl_x64:/build/src/libfaketimeMT.so.1 $(call fix_path,$(JAVA_RES_DIR)/alpine_x64.so)
	docker rm -f tmp_musl_x64

build-glibc-arm64:
	@echo --- Building linux_arm64.so (MT version, SHA: $(REMOTE_SHA)) ---
	docker build --platform linux/arm64 \
		--build-arg FAKETIME_SHA=$(REMOTE_SHA) \
		-t ft-glibc-arm -f $(DOCKER_SRC_DIR)/Dockerfile.glibc .
	docker create --name tmp_arm ft-glibc-arm
	docker cp tmp_arm:/build/src/libfaketimeMT.so.1 $(call fix_path,$(JAVA_RES_DIR)/linux_arm64.so)
	docker rm -f tmp_arm

build-musl-arm64:
	@echo --- Building alpine_arm64.so (MT version, SHA: $(REMOTE_SHA)) ---
	docker build --platform linux/arm64 \
		--build-arg FAKETIME_SHA=$(REMOTE_SHA) \
		-t ft-musl-arm -f $(DOCKER_SRC_DIR)/Dockerfile.musl .
	docker create --name tmp_musl_arm ft-musl-arm
	docker cp tmp_musl_arm:/build/src/libfaketimeMT.so.1 $(call fix_path,$(JAVA_RES_DIR)/alpine_arm64.so)
	docker rm -f tmp_musl_arm

save-version:
	@echo $(REMOTE_SHA) > $(call fix_path,$(JAVA_RES_DIR)/version.txt)

prepare-dirs:
	@if not exist "$(call fix_path,$(JAVA_RES_DIR))" mkdir "$(call fix_path,$(JAVA_RES_DIR))"

clean:
	@if exist "$(call fix_path,$(JAVA_RES_DIR))" del /Q "$(call fix_path,$(JAVA_RES_DIR))\*.so" 2>nul || exit 0
	@if exist "$(call fix_path,$(JAVA_RES_DIR)\version.txt)" del /Q "$(call fix_path,$(JAVA_RES_DIR)\version.txt)" 2>nul || exit 0
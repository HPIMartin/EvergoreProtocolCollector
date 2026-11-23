# ADAPT: the base image; pin it by digest where a fresh machine must get exactly the verified image
# (dev-environment.md). Toolchains come as pinned features in devcontainer.json. A digest pins the
# base and a lock file the features; an apt layer and the features' own downloads still take the
# current build, so pin them by version where it matters.
FROM mcr.microsoft.com/devcontainers/base:bookworm

# MODULE acceptance
RUN apt-get update \
	&& DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends firefox-esr \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/*
# /MODULE acceptance

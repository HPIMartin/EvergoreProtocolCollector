# Devcontainer base. Pinned by digest so a fresh machine gets exactly the image this setup was
# verified against; the tag stays readable and lets Dependabot bump tag + digest together.
FROM mcr.microsoft.com/devcontainers/base:bookworm@sha256:86165cfc170e9b2aa8df90b847127eea97b08eb9987021e6e6ec6c3a96545d7c

# CLI tools the daily workflow needs. Baked into an image layer, not installed in postCreate, so a
# container rebuild replays the layer instead of re-downloading ~103 MB (84 packages):
#   bc              - shell arithmetic in hooks and ad-hoc scripts
#   firefox-esr     - the browser Browser.DOCKER drives; Selenium Manager fetches the matching
#                     geckodriver itself into ~/.cache/selenium, so no driver binary is needed
#   git-filter-repo - the history rewrite F1 hands to the author
#   sqlite3         - inspecting database/temp.sqlite
RUN apt-get update \
	&& DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
		bc \
		firefox-esr \
		git-filter-repo \
		sqlite3 \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/*

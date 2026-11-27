ARG BASE=base-image-required-pass-build-arg-base
FROM ${BASE}
LABEL devcontainer.metadata="[]"
# MODULE volumes
ENV AGENT_WORKTREES=""
# /MODULE volumes

USER root
RUN --mount=type=bind,source=provision,target=/tmp/provision sh /tmp/provision

USER vscode
ENV PATH=/home/vscode/.local/bin:${PATH}
ENV DISABLE_AUTOUPDATER=1
RUN --mount=type=bind,source=install-claude-code,target=/tmp/install-claude-code sh /tmp/install-claude-code

WORKDIR /workspaces
CMD ["sleep", "infinity"]

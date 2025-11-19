FROM jenkins/jenkins:lts-jdk17

USER root

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl git jq tzdata tini gosu \
  && rm -rf /var/lib/apt/lists/*

# - configuration-as-code: JCasC 핵심
# - matrix-auth: (선택) 권한전략 확장 (원하면 logged-in only로 유지해도 OK)
RUN jenkins-plugin-cli --plugins \
  "configuration-as-code:latest" \
  "matrix-auth:latest"

# JCasC 기본 경로 (JENKINS_HOME 초기화 시 복사됨)
RUN mkdir -p /usr/share/jenkins/ref/casc
COPY casc/jenkins.yaml /usr/share/jenkins/ref/casc/jenkins.yaml

# 컨테이너 기본값으로 CASC 경로 지정 (compose에서 override 가능)
ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc/jenkins.yaml

# Compose v2 버전 최신으로 변경해야댐
ARG COMPOSE_VERSION=v2.40.2
RUN ARCH="$(uname -m)"; \
    case "$ARCH" in \
      x86_64|amd64) BIN=docker-compose-linux-x86_64 ;; \
      aarch64|arm64) BIN=docker-compose-linux-aarch64 ;; \
      *) echo "Unsupported arch: $ARCH" && exit 1 ;; \
    esac; \
    mkdir -p /usr/local/lib/docker/cli-plugins && \
    curl -fsSL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/${BIN}" \
      -o /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ >/etc/timezone

# docker.sock GID 동기화 + 권한 정리용 엔트리포인트
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# 시그널/좀비프로세스 처리용 tini로 래핑
ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/docker-entrypoint.sh"]
#!/bin/sh
set -eu

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
MVNW_REPOURL="$(sed -n 's/^distributionUrl=//p' "$WRAPPER_PROPERTIES")"
MVNW_VERSION="$(printf '%s\n' "$MVNW_REPOURL" | sed -n 's#.*apache-maven-\([0-9.]*\)-bin.*#\1#p')"
MVNW_DIST="$MAVEN_USER_HOME/wrapper/dists/apache-maven-$MVNW_VERSION"
MVNW_ARCHIVE="$MVNW_DIST/apache-maven-$MVNW_VERSION-bin.tar.gz"
MVNW_HOME="$MVNW_DIST/apache-maven-$MVNW_VERSION"

if [ -z "$MVNW_VERSION" ]; then
  echo "Unable to determine Maven version from $WRAPPER_PROPERTIES" >&2
  exit 1
fi

if [ ! -x "$MVNW_HOME/bin/mvn" ]; then
  mkdir -p "$MVNW_DIST"
  if [ ! -f "$MVNW_ARCHIVE" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$MVNW_REPOURL" -o "$MVNW_ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$MVNW_REPOURL" -O "$MVNW_ARCHIVE"
    else
      echo "curl or wget is required to download Maven" >&2
      exit 1
    fi
  fi
  tar -xzf "$MVNW_ARCHIVE" -C "$MVNW_DIST"
fi

exec "$MVNW_HOME/bin/mvn" "$@"

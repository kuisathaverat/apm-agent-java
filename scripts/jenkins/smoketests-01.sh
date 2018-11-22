#!/usr/bin/env bash

set -euxo pipefail

./mvnw -q -Dmaven.javadoc.skip=true -am -amd -pl apm-agent-plugins -P integration-test-only verify

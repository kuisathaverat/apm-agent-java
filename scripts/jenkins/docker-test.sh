#!/bin/bash
set -ex
export GOPATH=$WORKSPACE
eval "$(gvm $GO_VERSION)"
go get -v -u github.com/jstemmer/go-junit-report
go get -v -u github.com/axw/gocov/gocov
go get -v -u github.com/matm/gocov-html
go get -v -u github.com/axw/gocov/...
go get -v -u github.com/AlekSi/gocov-xml

go get -v -t ./... || echo "KO"

export COV_FILE="build/coverage.cov"
export OUT_FILE="build/test-report.out"
mkdir -p build

./scripts/docker-compose-testing up -d --build
./scripts/docker-compose-testing run -T --rm go-agent-tests make coverage > ${COV_FILE}
cat ${OUT_FILE} | go-junit-report > build/apm-agent-go-junit.xml
gocov convert "${COV_FILE}" | gocov-html > build/coverage-apm-agent-go-report.html
gocov convert "${COV_FILE}" | gocov-xml > build/coverage-apm-agent-go-report.xml


!#/bin/bash
set -e
export GOPATH=$WORKSPACE
eval "$(gvm $GO_VERSION)"
go get -v -u github.com/jstemmer/go-junit-report
go get -v -u github.com/axw/gocov/gocov
go get -v -u gopkg.in/matm/v1/gocov-html
go get -v -u github.com/axw/gocov/...
go get -v -u github.com/AlekSi/gocov-xml

go get -v -t ./...

export COV_DIR="build/coverage"
export OUT_FILE="build/test-report.out"
mkdir -p build
go test -race ${GOPACKAGES} -v 2>&1 | tee ${OUT_FILE}
cat ${OUT_FILE} | go-junit-report > build/apm-agent-go-junit.xml
for i in "full.cov" "integration.cov" "system.cov" "unit.cov"
do
  name=$(basename ${i} .cov)
  [ -f "${COV_DIR}/${i}" ] && gocov convert "${COV_DIR}/${i}" | gocov-html > build/coverage-${name}-report.html
  [ -f "${COV_DIR}/${i}" ] && gocov convert "${COV_DIR}/${i}" | gocov-xml > build/coverage-${name}-report.xml
done
exit 0

make test | $GOPATH/bin/go-junit-report &gt; apm-agent-go-junit.xml

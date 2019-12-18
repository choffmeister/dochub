#!/bin/bash -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

cd "${DIR}"
sbt scalafmtAll test:scalafmtSbt
sbt scalafmtCheckAll test:scalafmtSbtCheck
sbt test
sbt packageSite

cd "${DIR}/web"
yarn install
yarn lint-fix
yarn lint
yarn test --watchAll=false

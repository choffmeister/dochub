# dochub

```bash
# start external services
docker-compose up -d

# development
sbt ~reStart
yarn --cwd web start
./src/example/publish.sh http localhost:8080 {api-key}

# testing
sbt test
sbt scalafmt test:scalafmt
sbt scalafmtCheck test:scalafmtCheck
yarn --cwd web test

# packaging
sbt docker:publishLocal
sbt docker:publish

# documentation
sbt previewSite
sbt ghpagesPushSite
```

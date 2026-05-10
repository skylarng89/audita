#!/bin/bash

./gradlew sonar \
  -Dsonar.projectKey=audita-api \
  -Dsonar.projectName='audita-api' \
  -Dsonar.host.url=http://localhost:7040 \
  -Dsonar.token=sqa_4abc72195d5e9ea5aa5b311329edae9d575d951a \
  -Dsonar.scm.provider=git
#!/bin/bash

echo "Building project and executing tests for base-repo"
./gradlew -Ptravis clean check jacocoTestReport

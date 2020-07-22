#!/bin/bash

echo "Building project and executing tests"
./gradlew -Ptravis clean check jacocoTestReport

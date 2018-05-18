#!/bin/bash

echo "Building service-base library"
cd libraries/service-base 
./gradlew clean check install
cd ../../
echo "Building project and executing tests"
./gradlew clean check jacocoTestReport

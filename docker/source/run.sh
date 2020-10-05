#!/bin/bash

cd /base-repo
java -cp ".:./config:./base-repo.jar" -Dloader.path=file://`pwd`/base-repo.jar,./lib/,. -jar base-repo.jar

#!/bin/bash

ROOT_DIR=`pwd`
java -cp ${ROOT_DIR}/nesc-frontend/target/nesc-frontend-1.0-SNAPSHOT-jar-with-dependencies.jar pl.edu.mimuw.nesc.Main "$@"

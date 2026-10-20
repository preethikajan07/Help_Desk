#!/bin/bash
# Compile and run the HelpDesk terminal app
mkdir -p out
javac -d out src/helpdesk/*.java && java -cp out helpdesk.Main

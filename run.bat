@echo off
if not exist out mkdir out
javac -d out src\helpdesk\*.java && java -cp out helpdesk.Main

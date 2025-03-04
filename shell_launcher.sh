#!/bin/bash
rm beans -rf
mkdir beans
javac -cp jars/* -s src/ -d beans/ src/supportGUI/*\.java src/algorithms/*\.java src/characteristics/*\.java src/helpers/*\.java src/homebots/*\.java src/commun/*\.java
java -cp jars/*:beans/ supportGUI.Viewer

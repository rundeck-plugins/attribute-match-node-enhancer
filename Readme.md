Attribute Match Node Enhancer
==========

This is a plugin for Rundeck 3.0.15+ that implements some Node Enhancer plugins.

[![Build Status](https://travis-ci.org/rundeck-plugins/attribute-match-node-enhancer.svg?branch=master)](https://travis-ci.org/rundeck-plugins/attribute-match-node-enhancer)

## Download

[releases](https://github.com/rundeck-plugins/attribute-match-node-enhancer/releases/latest)

## Plugins

1. Icon Node Enhancer

Set the icon of a node based on existing attribute

2. Attribute Match Enhancer

Add some attributes when other attributes match a pattern.

# How to

## Build

Build the project with Gradle

    ./gradlew build

## Test

Test the project with Gradle

    ./gradlew check

## Release

Release the project.

    ./gradlew release

## Version

Get current version from axion-release plugin

    ./gradlew currentVersion

## increment minor version

Bump minor version

    ./gradlew markNextVersion -Prelease.incrementer=incrementMinor

## increment major version

Bump major version

    ./gradlew markNextVersion -Prelease.incrementer=incrementMajor
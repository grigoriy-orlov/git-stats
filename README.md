# git-stats

## Goal

Process git project for output statistics about added/deleted lines per author per language.

## Requirements

Java > 1.7

## Usage

```
java -jar ./target/gitstats-<version>-jar-with-dependencies.jar <path-to-git-dir>
```

Output to stdout:
 - author1:
  - language1: 
    - added: n
    - deleted: m
  - language2: 
    - added: k
    - deleted: l
 - ...

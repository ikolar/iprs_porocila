#! /bin/bash

where=$(dirname $0 | xargs readlink -f)

srcdir="$where/src"
bindir="$where/bin"

mkdir -p "$srcdir" "$bindir"

echo javac -cp $(ls -1 lib/*.jar | tr '\n' ';')"." -d bin/ $@

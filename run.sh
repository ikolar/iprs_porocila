#! /bin/bash

where=$(dirname $0 | xargs readlink -f)

bindir="$where/bin"

java -cp $(ls -1 "$where"/lib/*.jar | tr '\n' ':')"$bindir:." $@

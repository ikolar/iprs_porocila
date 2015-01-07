#! /bin/bash

IFS=$'\n'

for h in $(find POROCILA2/*/ -name "*.html" | fgrep -v "merged.html"); do

	caseno=$(cat "$h" | sed -re '/<(meta|title)/d' |
		sed -nre 's@.*(06..[^/]{,5})/(20[01].)(/[0-9]+)?.*@\1/\2@1p' | head -1 | sed -re 's/[^0-9\/-]//g;' | tr -dc "[:alnum:][:space:][:punct:]" |
		sed -re 's@^/+@@;')
	casedate=$(sed -nre 's/<meta name="modified" content="([^T]*)T.*/\1/p' "$h"  | head -1)
	casetype=$(dirname "$h" | xargs basename | sed -re 's/-.*//')
	nadzornik=$(basename "$h" | sed -re 's/\.\././g; s/[_-]?([0-9]+[\._-]?)*.docx?.html$//; s/.*[_,-]\s*//;')
	
	echo -e "${nadzornik}\t$casetype\t$casedate\t${caseno}\t$h"
done

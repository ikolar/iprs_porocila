#! /bin/bash

where=$(dirname $0 | xargs readlink -f)
report="$where/report"
IFS=$'\n'

# create list of nadzorniki from file names
# add those on the authorative list (Jelena just returned and she has no docs, bust must be included etc)
nadzorniki=$(mktemp.exe --tmpdir nadzorniki.XXXX)
nadzorniki2=$(mktemp.exe --tmpdir nadzorniki.XXXX)
cut -f 1 "$report" | sed -re 's/\s*//g; /^$/d' > "$nadzorniki2"
cut -f 1 "$where/nadzorniki" | sed -re '/^\s*$/d; /#/d' >> "$nadzorniki2"
sort -u "$nadzorniki2" > "$nadzorniki"
rm -f "$nadzorniki2"

types=$(mktemp.exe --tmpdir types.XXXX)
cut -f 2 "$report" | sort -u > "$types"

start_mon=8
stop_mon=12
year=2014
#$(date +"%Y")

# report files (for accountability)
accepted="SPREJETE DATOTEKE, PO NADZORNIKIH\n"
ignored="NESPREJETE DATOTEKE\n"

# print table for each type
# rows = months
# cols = nadzorniki
# cells = documents of each type by each nadzornik in each month

log=$(mktemp --tmpdir documents.XXXX)

# log for files that skip the filters (ie nadzornik wasn't recognized)
invalid=$(mktemp --tmpdir invalid.XXXX)
invalid_query="^("$(egrep -v "#" "$where/nadzorniki" | cut -f 1 | tr '\n' '|' | sed -re s'/\|*$//')")"

for t in $(cat "$types"); do
	nice_t="${t}" # todo
	
	# header
	echo -ne $(printf "%s" "$t")"\tskupaj\t"
	for m in $(seq $start_mon $stop_mon); do
		echo -ne $(date --date="${year}-${m}-01" "+%b")"\t"
	done
	echo
	
	for n in $(cat "$nadzorniki"); do
		# ignoriraj vodjo nadzornikov, namestnike
		if [[ $(egrep -i "^\s*${n}\s*\t" "$where/nadzorniki.ignore") ]]; then
			echo >&2 "INFO: ingoring nadzornik ${n} due to entry in nadzorniki.ignore file"
		fi
		nice_nadzornik=$(egrep -i "^${n}" "$where/nadzorniki" | head -1 | cut -f 2)
		if [[ -z "$nice_nadzornik" ]]; then
#			echo >&2 "WARN: unknown nadzornik: $n"
			continue
		fi
		echo "$nice_nadzornik ($n):" >> "$log"
		
		echo -n $(printf "%s" "$nice_nadzornik")
		echo -ne "\t"

		s=""
		total=0		

		echo "${nice_t}" >> "$log"
		for m in $(seq $start_mon $stop_mon); do
			month_query=$(printf "${year}-%02d" $m);
			query="^${n}\s*.${t}.${month_query}"
			num=$(egrep -ci "$query" "$report")

			sed -nre "/$query/Ip" "$report" | cut -f 4,3 >> "$log"
			let "total += $num"
			s="${s}${num}\t"
		done
		s="${total}\t${s}"
		echo -e $s

		echo >> "$log"
	done
	echo

done

echo "Neupoštevane datoteke:" > "$invalid" 
for m in $(seq $start_mon $stop_mon); do
	month_query=$(printf "${year}-%02d-" $m);
	sed -nre "/\t${month_query}/p" "$report" | sed -re "/$invalid_query/d" "$report" | cut -f 1,4,3 >> "$invalid"
done


cat "$log"
cat "$invalid"
# rm -f "$log"
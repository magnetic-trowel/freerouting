#!/bin/bash
# usage: measure.sh <jar> <board.dsn> <outdir> <xmx> <histo-interval-s|0> [extra args...]
# Runs one headless routing job, logs GC, and (if interval>0) takes GC.class_histogram samples
# (each forces a full GC, so the histogram total is the live heap at that moment). Keeps the
# histogram with the largest total as peak_histo.txt.
JAR=$1; DSN=$2; OUT=$3; XMX=$4; IV=$5; shift 5
mkdir -p "$OUT"; rm -f "$OUT"/histo_*.txt
start=$(date +%s.%N)
# JVMEXTRA may hold several JVM options, so it is word-split on purpose.
# shellcheck disable=SC2086
java -Xmx"$XMX" $JVMEXTRA -Xlog:gc:file="$OUT/gc.log" -XX:+ExitOnOutOfMemoryError -jar "$JAR" -de "$DSN" -do "$OUT/out.ses" \
  --gui.enabled=false --logging.file.enabled=false --logging.console.level=INFO --user_data_path="$OUT/ud" \
  "$@" > "$OUT/run.log" 2>&1 &
pid=$!
best=0; n=0
if [ "$IV" != "0" ]; then
  while kill -0 $pid 2>/dev/null; do
    sleep "$IV"
    full=$(/usr/lib/jvm/java-25-openjdk-amd64/bin/jcmd $pid GC.class_histogram 2>/dev/null)
    tot=$(echo "$full" | awk '/^Total/{print $3}')
    h=$(echo "$full" | head -45; echo "$full" | tail -1)
    if [ -n "$tot" ]; then
      n=$((n+1)); echo "$(date +%s) $tot" >> "$OUT/histo_series.txt"
      if [ "$tot" -gt "$best" ]; then best=$tot; echo "$h" > "$OUT/peak_histo.txt"; fi
    fi
  done
fi
wait $pid; rc=$?
end=$(date +%s.%N)
wall=$(echo "$end - $start" | bc)
final=$(grep -o "Auto-routing stage completed.*" "$OUT/run.log" | tail -1)
maxafter=$(grep -oE "[0-9]+M->[0-9]+M" "$OUT/gc.log" | sed 's/.*->//; s/M//' | sort -n | tail -1)
sha=$(sha1sum "$OUT/out.ses" 2>/dev/null | cut -c1-12)
echo "rc=$rc wall=${wall%.*} xmx=$XMX histo_peak_MB=$((best/1048576)) samples=$n gc_max_after_MB=$maxafter ses=$sha :: $final" | tee "$OUT/result.txt"

# Get durations
cat $1 | grep 'in [0-9]' | sed 's/.*in \(.*\) ms)/\1/g'


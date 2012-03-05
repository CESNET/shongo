# Get durations
cat $1 | grep 'in [0-9]' | sed 's/.*in \(.*\) ms)/\1/g'

# New line
echo ""

# Get java percentage and memory usage
cat $1 | grep 'java' | sed 's/.*| \+\(.*\)% | \+\(.*\) kB |.*/\1 \2/g'

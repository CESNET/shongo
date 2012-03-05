# Get started durations
cat $1 | grep '\[AGENT:STARTED\]\[in [0-9]' | sed 's/.*in \(.*\) ms\]/\1/g'

# New line
echo ""

# Get stopped durations
cat $1 | grep '\[AGENT:STOPPED\]\[in [0-9]' | sed 's/.*in \(.*\) ms\]/\1/g'

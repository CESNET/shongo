# Get durations
cat $1 | grep 'Normal Hello[0-9]\+\]' | sed 's/.*in \(.*\) ms)/\1/g'

# New line
echo ""

# Get durations
cat $1 | grep 'Killed Hello[0-9]\+\]' | sed 's/.*in \(.*\) ms)/\1/g'

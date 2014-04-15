#!/bin/bash

HOST=localhost

# list users
bin/client_cli.sh src --connect $HOST --root --scripting --cmd "list-referenced-users" \
    | sed '/userId/{N;s/\n//;}' \
    | grep userId \
    | sed 's/.*"userId" : "\(.\+\)".*"description" : "\(.\+\)".*/\1;\2/g' \
    > __tmp_referenced_users

# list user information
for user_id in $(cat __tmp_referenced_users | sed "s/\([0-9]\+\);.*/\1/g")
do
    if [[ $user_id -ne 0 ]] ; then
        result=$(bin/client_cli.sh src --connect $HOST --root --scripting --cmd "get-user $user_id" \
            | tr -d '\n' \
            | grep "\[ " \
            | sed 's/.*"First Name" : "\([^"]\+\)".*"Last Name" : "\([^"]\+\)".*"Original ID" : "\([^"]\+\)".*"Email" : \[ *"\([^"]\+\)".*/\3;\1 \2;\4/g')
        if [[ -z "$result" ]]
        then
            result="<not-exist>; "
        fi
        echo "$user_id;$result"
    fi
done > __tmp_user_info

# join user information
join -t ";" __tmp_user_info __tmp_referenced_users | column -t -s ";"

# delete temporary files
rm __tmp_user_info __tmp_referenced_users

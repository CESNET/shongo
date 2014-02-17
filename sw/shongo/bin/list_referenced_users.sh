#!/bin/bash

# list users
bin/client_cli.sh src --connect localhost --root --scripting --cmd "list-referenced-users" \
    | sed '/userId/{N;s/\n//;}' \
    | grep userId \
    | sed 's/.*"userId" : "\(.\+\)".*"description" : "\(.\+\)".*/\1;\2/g' \
    > __tmp_referenced_users

# list user information
for user_id in $(cat __tmp_referenced_users | sed "s/\([0-9]\+\);.*/\1/g")
do
    if [[ $user_id -ne 0 ]] ; then
        echo -n "$user_id;"
        bin/client_cli.sh src --connect localhost --root --scripting --cmd "get-user $user_id" \
            | tr -d '\n' \
            | grep "\[ " \
            | sed 's/.*"First Name" : "\([^"]\+\)".*"Last Name" : "\([^"]\+\)".*"Email" : "\([^"]\+\)".*/\1 \2;\3/g'
    fi
done > __tmp_user_info

# join user information
join -t ";" __tmp_user_info __tmp_referenced_users | column -t -s ";"

# delete temporary files
rm __tmp_user_info __tmp_referenced_users
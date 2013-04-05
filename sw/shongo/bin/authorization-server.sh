#!/bin/bash

URL=https://shongo-auth-dev.cesnet.cz

function request
{
    curl -X $1 --header "Content-Type: application/json" --header "Authorization: id=testclient;secret=12345" "$URL/authz/rest/$2" 2> /dev/null \
        | sed 's/.*httpStatus.*"detail"\:"\(.*\)"}/ERROR: \1\n/g'
}

function list
{
    param="-n"
    result="out(this.id, this.user_id, this.resource_id, this.role_id)"

    OPTIND=0
    while getopts "i" option; do
      case $option in
        i)
            param=""
            result="return this.id"
            ;;
      esac
    done
    shift $(($OPTIND - 1))

    if [ $1 ]; then
        query="?resource_id=$1"
    fi


    request GET "acl$query" | jsawk 'return this._embedded.acls' | jsawk $param "$result"
}

function delete
{
    OPTIND=0
    while getopts "r:" option; do
      case $option in
        r)
            for id in $(request GET "acl?resource_id=$OPTARG" | jsawk 'return this._embedded.acls' | jsawk -n "out(this.id)"); do
                request DELETE "acl/$id"
            done;
            exit
            ;;
      esac
    done
    shift $(($OPTIND - 1))

    request DELETE "acl/$1"
}

case $1 in
    list)
        shift
        list $@
        ;;
    delete)
        shift
        delete $@
        ;;
    *)
        echo "Unknown command '$1'."
        ;;
esac
#!/bin/bash
#
# Run Shongo controller application.
#

cd `dirname $0`/../
VERSION=`cat pom.xml | grep '<shongo.version>' | sed -e 's/.\+>\(.\+\)<.\+/\1/g'`

case $1 in
    reservation-requests)
        shift
        hash jsawk 2>/dev/null || {
            echo >&2 "Jsawk is required but it's not installed.";
            echo >&2 "You can download it here [https://github.com/micha/jsawk].";
            exit 1;
        }
        for id in $(bin/client_cli.sh --connect $1 --root --scripting --cmd "list-reservation-requests" | jsawk -n "out(this.id)"); do

            # Get reservation request id only if it has owner
            id=$(bin/client_cli.sh --connect $1 --root --scripting --cmd "list-acl -entity $id -role OWNER" | jsawk 'RS=RS.concat(this.entity)' -a 'return RS[0]')
            if [ -n "$id" ]; then
                echo
                bin/client_cli.sh --connect $1 --root --scripting --cmd "get-reservation-request $id" | sed "s/^{$/\${id} = create-reservation-request {/g" | grep -v "        \"id\""
                bin/client_cli.sh --connect $1 --root --scripting --cmd "list-acl -entity $id" | jsawk -n "out('\ncreate-acl ' + this.user + ' \${id} ' + this.role)"
            fi
        done;
        echo
        ;;
    *)
        java -jar controller/target/controller-$VERSION.jar "$@"
        ;;
esac

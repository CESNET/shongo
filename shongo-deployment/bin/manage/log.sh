
#
# Manage Shongo
#
#   "manage.sh login <host>" List user login to client-web on given <host>
#

function login
{
    if [[ -z $1 ]]; then
        echo "Argument <host> required."
        exit
    fi

    case $1 in
        shongo-dev)
            host="shongo-dev.cesnet.cz"
            user="shongo"
            log="/home/shongo/shongo/shongo-deployment/log/shongo-client-web.log"
            ;;
        meetings)
            host="meetings.cesnet.cz"
            user="root"
            log="/root/shongo/shongo-deployment/log/shongo-client-web.log"
            ;;
        *)
            echo "Unknown host '$1'."
            exit
            ;;
    esac

    ssh $host -l $user "cat $log" \
        | grep --no-group-separator -B 1 authenticated \
        | sed '/INFO.*/{N; s/INFO.*\([0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}\):.*id: \(.\+\),.*name: \(.\+\)).*/\1: \3 (\2)/g}'
}

case $1 in
    login)
        shift
        login $@
        ;;
    *)
        echo "Unknown command '$1'."
        ;;
esac
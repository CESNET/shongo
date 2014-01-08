window.formatUsers = function(users, emptyText, maximumCount) {
    var content = "";
    for (var index = 0; index < users.length; index++) {
        var user = users[index];
        if (content != '') {
            content += ', ';
        }
        content += user.fullName;
        if (maximumCount != null && index > maximumCount) {
            content += ', ...';
            break;
        }
    }
    if (content == "") {
        content = emptyText;
    }
    return content;
};
package cz.cesnet.shongo.controller.authorization;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class UserIdSet {
    private Set<String> userIds = new HashSet<String>();

    private boolean everyone = false;

    public UserIdSet() {}

    public UserIdSet(boolean everyone) {
        this.everyone = everyone;
    }

    public UserIdSet(String userId) {
        this.userIds.add(userId);
    }

    public UserIdSet(Set<String> userIds) {
        this.userIds.addAll(userIds);
    }

    public boolean isEveryone() {
        return everyone;
    }

    public void setEveryone(boolean everyone) {
        this.everyone = everyone;
    }


    public void add(String userId) {
        userIds.add(userId);
    }

    public void remove(String userId) {
        userIds.remove(userId);
    }

    public void addAll(Set<String> userIds) {
        this.userIds.addAll(userIds);
    }

    public void addAll(UserIdSet userIds) {
        this.userIds.addAll(userIds.userIds);
    }

    public boolean contains(String userId) {
        if (everyone) {
            return true;
        }
        return userIds.contains(userId);
    }

    public int size() {
        return this.userIds.size();
    }

    public void clear() {
        userIds.clear();
    }
}

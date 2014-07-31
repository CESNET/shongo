package cz.cesnet.shongo.controller.authorization;

import java.util.Collection;
import java.util.Collections;
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
        if (userIds.isEveryone()) {
            setEveryone(true);
        }
        this.userIds.addAll(userIds.userIds);
    }

    public boolean contains(String userId) {
        if (everyone) {
            return true;
        }
        return userIds.contains(userId);
    }



    public int size() {
        if (everyone) {
            return Integer.MAX_VALUE;
        }
        return this.userIds.size();
    }

    public void clear() {
        userIds.clear();
    }


    public Set<String> getUserIds() {
        if (this.everyone) {
            throw new IllegalArgumentException("Cannot get all user-ids for group everyone.");
        }

        return Collections.unmodifiableSet(this.userIds);
    }

    public void retainAll(UserIdSet userIds) {
        if (!userIds.isEveryone()) {
            if (everyone) {
                clear();
                setEveryone(false);
                addAll(userIds);
            }
            else {
                this.userIds.retainAll(userIds.getUserIds());
            }
        }
    }
}

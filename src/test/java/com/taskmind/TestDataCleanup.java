package com.taskmind;

import com.taskmind.infrastructure.db.ProjectEntity;
import com.taskmind.infrastructure.db.ProjectMembershipEntity;
import com.taskmind.infrastructure.db.TaskEntity;
import com.taskmind.infrastructure.db.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TestDataCleanup {

    @Transactional
    public void clearAll() {
        ProjectMembershipEntity.deleteAll();
        TaskEntity.deleteAll();
        ProjectEntity.deleteAll();
        UserEntity.deleteAll();
    }
}

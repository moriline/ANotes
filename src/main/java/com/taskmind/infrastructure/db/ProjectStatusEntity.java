package com.taskmind.infrastructure.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "projectStatuses")
public class ProjectStatusEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statusId")
    public Integer id;

    @Column(nullable = false)
    public Integer projectId;

    @Column(nullable = false)
    public String statusName;

    public String statusColor;
    public Integer statusOrder;
    public boolean isDefault;
    public boolean isClosed;
}

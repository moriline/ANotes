package com.taskmind.infrastructure.db;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "activityLog")
public class ActivityLogEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activityId")
    public Integer id;

    @Column(nullable = false)
    public Integer projectId;

    /** null у событий уровня проекта: добавление участника, создание проекта. */
    public Integer taskId;

    public Integer userId;

    @Column(nullable = false)
    public String actionType;

    /** JSON-объект с подробностями события; в схеме это VARCHAR(2000). */
    public String actionDetails;

    /** PUBLIC | INTERNAL | SYSTEM. Хранится строкой; см. {@link com.taskmind.domain.model.Visibility}. */
    @Column(nullable = false)
    public String visibility = "PUBLIC";

    public Long createdAt;
}

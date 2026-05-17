package com.taskmind.api.dto;

public class FindTasksRequest {
    public Integer projectId;
    public String titleSearch;
    public Integer assignedUserId;
    public Integer statusId;
    public Boolean isArchived;
}

package com.complipilot.backend.organization.dto;

import java.util.List;

public record SeedDemoUsersResponse(
        int createdUsers,
        int createdMemberships,
        int updatedMemberships,
        List<OrganizationMemberResponse> members
) {
}

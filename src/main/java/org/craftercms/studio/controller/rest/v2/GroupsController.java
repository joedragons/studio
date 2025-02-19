/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.studio.controller.rest.v2;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.SqlSort;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.exception.security.*;
import org.craftercms.studio.api.v2.dal.Group;
import org.craftercms.studio.api.v2.dal.User;
import org.craftercms.studio.api.v2.exception.OrganizationNotFoundException;
import org.craftercms.studio.api.v2.service.security.GroupService;
import org.craftercms.studio.controller.rest.ValidationUtils;
import org.craftercms.studio.model.rest.*;
import org.craftercms.studio.model.rest.groups.UpdateGroupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import java.beans.ConstructorProperties;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElse;
import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.USERNAME;
import static org.craftercms.studio.api.v1.constant.StudioConstants.DEFAULT_ORGANIZATION_ID;
import static org.craftercms.studio.controller.rest.v2.RequestConstants.*;
import static org.craftercms.studio.controller.rest.v2.RequestMappingConstants.*;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.*;
import static org.craftercms.studio.model.rest.ApiResponse.*;

@Validated
@RequestMapping(API_2 + GROUPS)
@RestController
public class GroupsController {

    private static final Logger logger = LoggerFactory.getLogger(GroupsController.class);

    private final GroupService groupService;

    @ConstructorProperties({"groupService"})
    public GroupsController(final GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * Get groups API
     *
     * @param keyword keyword parameter
     * @param offset  offset parameter
     * @param limit   limit parameter
     * @param sort    sort parameter
     * @return Response containing list of groups
     */
    @GetMapping
    public PaginatedResultList<Group> getAllGroups(
            @RequestParam(value = REQUEST_PARAM_KEYWORD, required = false) String keyword,
            @PositiveOrZero @RequestParam(value = REQUEST_PARAM_OFFSET, required = false, defaultValue = "0") int offset,
            @PositiveOrZero @RequestParam(value = REQUEST_PARAM_LIMIT, required = false, defaultValue = "10") int limit,
            @SqlSort(columns = GROUP_SORT_COLUMNS) @RequestParam(value = REQUEST_PARAM_SORT, required = false,
                    defaultValue = "group_name asc") String sort)
            throws ServiceLayerException, OrganizationNotFoundException {
        int total = groupService.getAllGroupsTotal(DEFAULT_ORGANIZATION_ID, keyword);
        List<Group> groups = groupService.getAllGroups(DEFAULT_ORGANIZATION_ID, keyword, offset, limit, sort);

        PaginatedResultList<Group> result = new PaginatedResultList<>();
        result.setTotal(total);
        result.setOffset(offset);
        result.setLimit(CollectionUtils.isEmpty(groups) ? 0 : groups.size());
        result.setResponse(OK);
        result.setEntities(RESULT_KEY_GROUPS, groups);
        return result;
    }

    /**
     * Create group API
     *
     * @param group Group to create
     * @return Response object
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResultOne<Group> createGroup(@Valid @RequestBody Group group)
            throws GroupAlreadyExistsException, ServiceLayerException, AuthenticationException {
        Group newGroup =
                groupService.createGroup(DEFAULT_ORGANIZATION_ID, group.getGroupName(), group.getGroupDescription(), false);
        ResultOne<Group> result = new ResultOne<>();
        result.setResponse(CREATED);
        result.setEntity(RESULT_KEY_GROUP, newGroup);
        return result;
    }

    /**
     * Update group API
     *
     * @param updateRequest {@link UpdateGroupRequest} to update
     * @return Response object
     */
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResultOne<Group> updateGroup(@Valid @RequestBody UpdateGroupRequest updateRequest)
            throws ServiceLayerException, GroupNotFoundException, AuthenticationException, GroupExternallyManagedException {
        Group group = buildGroup(updateRequest);
        Group updatedGroup = groupService.updateGroup(DEFAULT_ORGANIZATION_ID, group);

        ResultOne<Group> result = new ResultOne<>();
        result.setResponse(OK);
        result.setEntity(RESULT_KEY_GROUP, updatedGroup);
        return result;
    }

    private Group buildGroup(final UpdateGroupRequest updateRequest) {
        Group group = new Group();
        group.setId(updateRequest.getId());
        group.setGroupDescription(updateRequest.getGroupDescription());
        return group;
    }

    /**
     * Delete group API
     *
     * @param groupIds Group identifier
     * @return Response object
     */
    @DeleteMapping
    public Result deleteGroups(@RequestParam(REQUEST_PARAM_ID) List<Long> groupIds)
            throws ServiceLayerException, GroupNotFoundException, AuthenticationException, GroupExternallyManagedException {
        groupService.deleteGroup(groupIds);
        Result result = new Result();
        result.setResponse(DELETED);
        return result;
    }

    /**
     * Get group API
     *
     * @param groupId Group identifier
     * @return Response containing requested group
     */
    @GetMapping(PATH_PARAM_ID)
    public ResultOne<Group> getGroup(@PathVariable(REQUEST_PARAM_ID) int groupId)
            throws ServiceLayerException, GroupNotFoundException {
        Group group = groupService.getGroup(groupId);
        ResultOne<Group> result = new ResultOne<>();
        result.setResponse(OK);
        result.setEntity(RESULT_KEY_GROUP, group);
        return result;
    }

    /**
     * Get group members API
     *
     * @param groupId Group identifier
     * @param offset  Result set offset
     * @param limit   Result set limit
     * @param sort    Sort order
     * @return Response containing list od users
     */
    @GetMapping(PATH_PARAM_ID + MEMBERS)
    public PaginatedResultList<User> getGroupMembers(
            @PathVariable(REQUEST_PARAM_ID) int groupId,
            @PositiveOrZero @RequestParam(value = REQUEST_PARAM_OFFSET, required = false, defaultValue = "0") int offset,
            @PositiveOrZero @RequestParam(value = REQUEST_PARAM_LIMIT, required = false, defaultValue = "10") int limit,
            @SqlSort(columns = USER_SORT_COLUMNS) @RequestParam(value = REQUEST_PARAM_SORT, required = false,
                    defaultValue = "id asc") String sort)
            throws ServiceLayerException, GroupNotFoundException {

        int total = groupService.getGroupMembersTotal(groupId);
        List<User> users = groupService.getGroupMembers(groupId, offset, limit, sort);

        PaginatedResultList<User> result = new PaginatedResultList<>();
        result.setResponse(OK);
        result.setTotal(total);
        result.setOffset(offset);
        result.setLimit(limit);
        result.setEntities(RESULT_KEY_USERS, users);
        return result;
    }

    /**
     * Add group members API
     *
     * @param groupId         Group identifiers
     * @param addGroupMembers Add members request body (json representation)
     * @return Response object
     */
    @PostMapping(value = PATH_PARAM_ID + MEMBERS, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResultList<User> addGroupMembers(@PathVariable(REQUEST_PARAM_ID) int groupId,
                                            @RequestBody AddGroupMembers addGroupMembers)
            throws ServiceLayerException, UserNotFoundException, GroupNotFoundException, AuthenticationException {

        ValidationUtils.validateAddGroupMembers(addGroupMembers);

        List<User> addedUsers = groupService.addGroupMembers(groupId, addGroupMembers.getIds(),
                addGroupMembers.getUsernames());

        ResultList<User> result = new ResultList<>();
        result.setResponse(OK);
        result.setEntities(RESULT_KEY_USERS, addedUsers);
        return result;
    }

    /**
     * Remove group members API
     *
     * @param groupId   Group identifier
     * @param userIds   List of user identifiers
     * @param usernames List of usernames
     * @return Response object
     */
    @DeleteMapping(PATH_PARAM_ID + MEMBERS)
    public Result removeGroupMembers(
            @PathVariable(REQUEST_PARAM_ID) int groupId,
            @RequestParam(value = REQUEST_PARAM_USER_ID, required = false) List<Long> userIds,
            @RequestParam(value = REQUEST_PARAM_USERNAME, required = false) List<@NotBlank @EsapiValidatedParam(type = USERNAME) String> usernames)
            throws ServiceLayerException, UserNotFoundException, GroupNotFoundException, AuthenticationException {

        ValidationUtils.validateAnyListNonEmpty(userIds, usernames);

        groupService.removeGroupMembers(groupId,
                requireNonNullElse(userIds, emptyList()),
                requireNonNullElse(usernames, emptyList()));

        Result result = new Result();
        result.setResponse(DELETED);
        return result;
    }

}

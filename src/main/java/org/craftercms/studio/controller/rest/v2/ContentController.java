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
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidExistingContentPath;
import org.craftercms.commons.validation.annotations.param.ValidSiteId;
import org.craftercms.commons.validation.annotations.param.ValidateSecurePathParam;
import org.craftercms.studio.api.v1.exception.ContentNotFoundException;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.exception.SiteNotFoundException;
import org.craftercms.studio.api.v1.exception.security.AuthenticationException;
import org.craftercms.studio.api.v1.exception.security.UserNotFoundException;
import org.craftercms.studio.api.v1.service.deployment.DeploymentException;
import org.craftercms.studio.api.v2.dal.QuickCreateItem;
import org.craftercms.studio.api.v2.exception.content.ContentAlreadyUnlockedException;
import org.craftercms.studio.api.v2.service.clipboard.ClipboardService;
import org.craftercms.studio.api.v2.service.content.ContentService;
import org.craftercms.studio.api.v2.service.dependency.DependencyService;
import org.craftercms.studio.api.v2.service.workflow.WorkflowService;
import org.craftercms.studio.model.rest.ResponseBody;
import org.craftercms.studio.model.rest.Result;
import org.craftercms.studio.model.rest.ResultList;
import org.craftercms.studio.model.rest.ResultOne;
import org.craftercms.studio.model.rest.clipboard.DuplicateRequest;
import org.craftercms.studio.model.rest.clipboard.PasteRequest;
import org.craftercms.studio.model.rest.content.*;
import org.dom4j.Document;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.beans.ConstructorProperties;
import java.util.*;
import java.util.stream.Collectors;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.ALPHANUMERIC;
import static org.craftercms.studio.api.v1.constant.StudioConstants.FILE_SEPARATOR;
import static org.craftercms.studio.api.v1.constant.StudioConstants.INDEX_FILE;
import static org.craftercms.studio.controller.rest.v2.RequestConstants.*;
import static org.craftercms.studio.controller.rest.v2.RequestMappingConstants.*;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.*;
import static org.craftercms.studio.model.rest.ApiResponse.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Validated
@RestController
@RequestMapping(API_2 + CONTENT)
public class ContentController {

    private final ContentService contentService;
    private final DependencyService dependencyService;
    private final WorkflowService workflowService;

    //TODO: Migrate logic to new content service
    private final ClipboardService clipboardService;

    @ConstructorProperties({"contentService", "dependencyService", "clipboardService", "workflowService"})
    public ContentController(ContentService contentService, DependencyService dependencyService, ClipboardService clipboardService,
                             WorkflowService workflowService) {
        this.contentService = contentService;
        this.dependencyService = dependencyService;
        this.clipboardService = clipboardService;
        this.workflowService = workflowService;
    }

    @Valid
    @GetMapping(LIST_QUICK_CREATE_CONTENT)
    public ResponseBody listQuickCreateContent(@ValidSiteId @RequestParam(name = "siteId") String siteId)
            throws SiteNotFoundException {
        List<QuickCreateItem> items = contentService.getQuickCreatableContentTypes(siteId);

        ResponseBody responseBody = new ResponseBody();
        ResultList<QuickCreateItem> result = new ResultList<>();
        result.setResponse(OK);
        result.setEntities(RESULT_KEY_ITEMS, items);
        responseBody.setResult(result);
        return responseBody;
    }

    @Valid
    @PostMapping(GET_DELETE_PACKAGE)
    public ResponseBody getDeletePackage(@RequestBody @Valid GetDeletePackageRequestBody request) throws SiteNotFoundException {
        List<String> childItems = contentService.getChildItems(request.getSiteId(), request.getPaths());
        List<String> dependentItems = dependencyService.getDependentItems(request.getSiteId(), request.getPaths());
        ResponseBody responseBody = new ResponseBody();
        ResultOne<Map<String, List<String>>> result = new ResultOne<>();
        result.setResponse(OK);
        Map<String, List<String>> items = new HashMap<>();
        items.put(RESULT_KEY_CHILD_ITEMS, childItems);
        items.put(RESULT_KEY_DEPENDENT_ITEMS, dependentItems);
        result.setEntity(RESULT_KEY_ITEMS, items);
        responseBody.setResult(result);
        return responseBody;
    }

    @Valid
    @PostMapping(value = DELETE, consumes = APPLICATION_JSON_VALUE)
    public ResponseBody delete(@RequestBody @Validated DeleteRequestBody deleteRequestBody)
            throws UserNotFoundException, ServiceLayerException, DeploymentException {
        workflowService.delete(deleteRequestBody.getSiteId(), deleteRequestBody.getItems(),
                deleteRequestBody.getOptionalDependencies(), deleteRequestBody.getComment());

        var responseBody = new ResponseBody();
        var result = new Result();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @Valid
    @PostMapping(value = GET_CHILDREN_BY_PATH, produces = APPLICATION_JSON_VALUE)
    public ResponseBody getChildrenByPath(@RequestBody @Valid GetChildrenByPathRequestBody request)
            throws ServiceLayerException, UserNotFoundException {
        GetChildrenResult result = contentService.getChildrenByPath(
                request.getSiteId(), request.getPath(), request.getLocaleCode(), request.getKeyword(),
                request.getSystemTypes(), request.getExcludes(), request.getSortStrategy(), request.getOrder(),
                request.getOffset(), request.getLimit());
        ResponseBody responseBody = new ResponseBody();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @GetMapping(value = GET_DESCRIPTOR, produces = APPLICATION_JSON_VALUE)
    public ResultOne<String> getDescriptor(@NotEmpty @ValidSiteId @RequestParam String siteId,
                                           @ValidExistingContentPath @ValidateSecurePathParam @RequestParam String path,
                                           @RequestParam(required = false, defaultValue = "false") boolean flatten) throws
            ContentNotFoundException, SiteNotFoundException {
        Document descriptor = contentService.getItemDescriptor(siteId, path, flatten);
        var result = new ResultOne<String>();
        result.setResponse(OK);
        result.setEntity(RESULT_KEY_XML, descriptor.asXML());
        return result;
    }

    @Valid
    @PostMapping(value = PASTE_ITEMS, produces = APPLICATION_JSON_VALUE)
    public ResponseBody pasteItems(@Valid @RequestBody PasteRequest request) throws Exception {
        var result = new ResultList<String>();
        result.setResponse(OK);
        result.setEntities(RESULT_KEY_ITEMS,
                clipboardService.pasteItems(request.getSiteId(), request.getOperation(),
                        request.getTargetPath(), request.getItem()));

        var response = new ResponseBody();
        response.setResult(result);

        return response;
    }

    @Valid
    @PostMapping(value = DUPLICATE_ITEM, produces = APPLICATION_JSON_VALUE)
    public ResponseBody duplicateItem(@Valid @RequestBody DuplicateRequest request) throws Exception {
        var result = new ResultOne<String>();
        result.setResponse(OK);
        result.setEntity(RESULT_KEY_ITEM,
                clipboardService.duplicateItem(request.getSiteId(), request.getPath()));

        var response = new ResponseBody();
        response.setResult(result);

        return response;
    }

    @Valid
    @GetMapping(value = ITEM_BY_PATH, produces = APPLICATION_JSON_VALUE)
    public ResponseBody getItemByPath(@ValidSiteId
                                      @RequestParam(value = REQUEST_PARAM_SITEID) String siteId,
                                      @ValidExistingContentPath
                                      @RequestParam(value = REQUEST_PARAM_PATH) String path,
                                      @RequestParam(value = REQUEST_PARAM_PREFER_CONTENT, required = false,
                                              defaultValue = "false") boolean preferContent)
            throws ServiceLayerException, UserNotFoundException {
        DetailedItem detailedItem = contentService.getItemByPath(siteId, path, preferContent);
        ResponseBody responseBody = new ResponseBody();
        ResultOne<DetailedItem> result = new ResultOne<>();
        result.setEntity(RESULT_KEY_ITEM, detailedItem);
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping(value = SANDBOX_ITEMS_BY_PATH, produces = APPLICATION_JSON_VALUE)
    public ResponseBody getSandboxItemsByPath(@RequestBody @Valid GetSandboxItemsByPathRequestBody request)
            throws ServiceLayerException, UserNotFoundException {
        String siteId = request.getSiteId();
        Collection<String> missing = Collections.emptyList();
        List<String> paths = request.getPaths();
        boolean preferContent = request.isPreferContent();
        List<SandboxItem> sandboxItems = contentService.getSandboxItemsByPath(siteId, paths, preferContent);
        ResponseBody responseBody = new ResponseBody();

        if (CollectionUtils.isEmpty(sandboxItems) || paths.size() != sandboxItems.size()) {
            List<String> found = sandboxItems.stream().map(SandboxItem::getPath).collect(Collectors.toList());
            if (preferContent) {
                found.addAll(sandboxItems.stream().map(si -> StringUtils.replace(si.getPath(),
                        FILE_SEPARATOR + INDEX_FILE, "")).collect(Collectors.toList()));
            }
            missing = CollectionUtils.subtract(paths, found);
        }

        GetSandboxItemsByPathResult result = new GetSandboxItemsByPathResult();
        result.setEntities(RESULT_KEY_ITEMS, sandboxItems);
        result.setMissingItems(missing);
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @Valid
    @PostMapping(ITEM_LOCK_BY_PATH)
    public ResponseBody itemLockByPath(@RequestBody @Valid LockItemByPathRequest request)
            throws UserNotFoundException, ServiceLayerException {
        contentService.lockContent(request.getSiteId(), request.getPath());
        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @Valid
    @PostMapping(ITEM_UNLOCK_BY_PATH)
    public ResponseBody itemUnlockByPath(@RequestBody @Valid UnlockItemByPathRequest request)
            throws ContentNotFoundException, ContentAlreadyUnlockedException, SiteNotFoundException {
        contentService.unlockContent(request.getSiteId(), request.getPath());
        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @Valid
    @GetMapping(GET_CONTENT_BY_COMMIT_ID)
    public ResponseEntity<Resource> getContentByCommitId(@ValidSiteId @RequestParam(value = REQUEST_PARAM_SITEID) String siteId,
                                                         @ValidExistingContentPath @RequestParam(value = REQUEST_PARAM_PATH) String path,
                                                         @EsapiValidatedParam(type = ALPHANUMERIC) @RequestParam(value = REQUEST_PARAM_COMMIT_ID) String commitId)
            throws ServiceLayerException, UserNotFoundException {
        DetailedItem item = contentService.getItemByPath(siteId, path, true);
        Resource resource = contentService.getContentByCommitId(siteId, path, commitId).orElseThrow();

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(item.getMimeType()))
                .body(resource);
    }

    @PostMapping(value = RENAME, consumes = APPLICATION_JSON_VALUE)
    public ResponseBody rename(@Valid @RequestBody RenameRequestBody renameRequestBody)
            throws AuthenticationException, UserNotFoundException, ServiceLayerException, DeploymentException {
        contentService.renameContent(renameRequestBody.getSiteId(), renameRequestBody.getPath(), renameRequestBody.getName());

        var responseBody = new ResponseBody();
        var result = new Result();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }
}

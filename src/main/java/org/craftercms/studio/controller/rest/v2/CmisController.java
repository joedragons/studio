/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidateNoTagsParam;
import org.craftercms.commons.validation.annotations.param.ValidateObjectParam;
import org.craftercms.commons.validation.annotations.param.ValidateParams;
import org.craftercms.studio.api.v1.exception.*;
import org.craftercms.studio.api.v1.exception.security.UserNotFoundException;
import org.craftercms.studio.api.v2.dal.CmisContentItem;
import org.craftercms.studio.api.v2.exception.InvalidParametersException;
import org.craftercms.studio.api.v2.exception.configuration.ConfigurationException;
import org.craftercms.studio.api.v2.service.cmis.CmisService;
import org.craftercms.studio.impl.v2.utils.PaginationUtils;
import org.craftercms.studio.model.rest.ResponseBody;
import org.craftercms.studio.model.rest.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.HTTPURI;
import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.SITE_ID;
import static org.craftercms.studio.controller.rest.v2.RequestConstants.*;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.RESULT_KEY_ITEM;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.RESULT_KEY_ITEMS;
import static org.craftercms.studio.model.rest.ApiResponse.OK;

@RestController
public class CmisController {

    protected CmisService cmisService;

    @ValidateParams
    @GetMapping("/api/2/cmis/list")
    public ResponseBody list(@EsapiValidatedParam(type = SITE_ID) @RequestParam(value = "siteId") String siteId,
                             @ValidateNoTagsParam @RequestParam(value = "cmisRepoId") String cmisRepoId,
                             @EsapiValidatedParam(type = HTTPURI) @RequestParam(value = "path", required = false, defaultValue = StringUtils.EMPTY) String path,
                             @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                             @RequestParam(value = "limit", required = false, defaultValue = "10") int limit)
            throws CmisRepositoryNotFoundException, CmisTimeoutException, CmisUnavailableException, ConfigurationException, SiteNotFoundException {
        List<CmisContentItem> cmisContentItems = cmisService.list(siteId, cmisRepoId, path);
        List<CmisContentItem> paginatedItems =
                PaginationUtils.paginate(cmisContentItems, offset, limit, StringUtils.EMPTY);

        ResponseBody responseBody = new ResponseBody();
        PaginatedResultList<CmisContentItem> result = new PaginatedResultList<>();
        result.setTotal(cmisContentItems.size());
        result.setOffset(offset);
        result.setLimit(paginatedItems.size());
        result.setResponse(OK);
        responseBody.setResult(result);
        result.setEntities(RESULT_KEY_ITEMS, paginatedItems);
        return responseBody;
    }

    @ValidateParams
    @GetMapping("/api/2/cmis/search")
    public ResponseBody search(@EsapiValidatedParam(type = SITE_ID) @RequestParam(value = "siteId") String siteId,
                               @ValidateNoTagsParam @RequestParam(value = "cmisRepoId") String cmisRepoId,
                               @ValidateNoTagsParam @RequestParam(value = "searchTerm") String searchTerm,
                               @EsapiValidatedParam(type = HTTPURI) @RequestParam(value = "path", required = false, defaultValue = StringUtils.EMPTY) String path,
                               @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
                               @RequestParam(value = "limit", required = false, defaultValue = "10") int limit)
            throws CmisRepositoryNotFoundException, CmisTimeoutException, CmisUnavailableException, ConfigurationException, SiteNotFoundException {
        List<CmisContentItem> cmisContentItems = cmisService.search(siteId, cmisRepoId, searchTerm, path);
        List<CmisContentItem> paginatedItems =
                PaginationUtils.paginate(cmisContentItems, offset, limit, StringUtils.EMPTY);

        ResponseBody responseBody = new ResponseBody();
        PaginatedResultList<CmisContentItem> result = new PaginatedResultList<>();
        result.setTotal(cmisContentItems.size());
        result.setOffset(offset);
        result.setLimit(paginatedItems.size());
        result.setResponse(OK);
        responseBody.setResult(result);
        result.setEntities(RESULT_KEY_ITEMS, paginatedItems);
        return responseBody;
    }

    @ValidateParams
    @PostMapping("/api/2/cmis/clone")
    public ResponseBody cloneContent(@ValidateObjectParam @RequestBody CmisCloneRequest cmisCloneRequest)
            throws CmisUnavailableException, CmisTimeoutException, CmisRepositoryNotFoundException,
            ServiceLayerException, CmisPathNotFoundException, UserNotFoundException {
        cmisService.cloneContent(cmisCloneRequest.getSiteId(), cmisCloneRequest.getCmisRepoId(),
                cmisCloneRequest.getCmisPath(), cmisCloneRequest.getStudioPath());

        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping(value = "/api/2/cmis/upload")
    public ResponseBody uploadContent(HttpServletRequest httpServletRequest)
            throws IOException, CmisUnavailableException, CmisPathNotFoundException, CmisTimeoutException,
            CmisRepositoryNotFoundException, FileUploadException, InvalidParametersException, ConfigurationException, SiteNotFoundException {
        ServletFileUpload servletFileUpload = new ServletFileUpload();
        FileItemIterator itemIterator = servletFileUpload.getItemIterator(httpServletRequest);
        String filename;
        String siteId = StringUtils.EMPTY;
        String cmisRepoId = StringUtils.EMPTY;
        String cmisPath = StringUtils.EMPTY;
        CmisUploadItem cmisUploadItem = new CmisUploadItem();
        while (itemIterator.hasNext()) {
            FileItemStream item = itemIterator.next();
            String name = item.getFieldName();
            try (InputStream stream = item.openStream()) {
                if (item.isFormField()) {
                    switch (name) {
                        case REQUEST_PARAM_SITEID:
                            siteId = Streams.asString(stream);
                            break;
                        case REQUEST_PARAM_CMIS_REPO_ID:
                            cmisRepoId = Streams.asString(stream);
                            break;
                        case REQUEST_PARAM_CMIS_PATH:
                            cmisPath = Streams.asString(stream);
                            break;
                        default:
                            // Unknown parameter, just skip it...
                            break;
                    }
                } else {
                    filename = item.getName();
                    if (StringUtils.isEmpty(siteId)) {
                        throw new InvalidParametersException("Invalid siteId");
                    }
                    if (StringUtils.isEmpty(cmisRepoId)) {
                        throw new InvalidParametersException("Invalid cmisRepoId");
                    }
                    if (StringUtils.isEmpty(cmisPath)) {
                        throw new InvalidParametersException("Invalid cmisPath");
                    }
                    cmisUploadItem = cmisService.uploadContent(siteId, cmisRepoId, cmisPath, filename, stream);
                }
            }
        }
        ResponseBody responseBody = new ResponseBody();
        ResultOne<CmisUploadItem> result = new ResultOne<>();
        result.setResponse(OK);
        result.setEntity(RESULT_KEY_ITEM, cmisUploadItem);
        responseBody.setResult(result);
        return responseBody;
    }

    public void setCmisService(CmisService cmisService) {
        this.cmisService = cmisService;
    }
}

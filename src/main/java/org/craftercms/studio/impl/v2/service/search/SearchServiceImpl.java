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

package org.craftercms.studio.impl.v2.service.search;

import org.craftercms.commons.security.permissions.DefaultPermission;
import org.craftercms.commons.security.permissions.annotations.HasPermission;
import org.craftercms.commons.security.permissions.annotations.ProtectedResourceId;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.exception.security.AuthenticationException;
import org.craftercms.studio.api.v1.service.security.SecurityService;
import org.craftercms.studio.api.v1.service.site.SiteService;
import org.craftercms.studio.api.v2.service.search.SearchService;
import org.craftercms.studio.api.v2.service.search.internal.SearchServiceInternal;
import org.craftercms.studio.model.search.SearchParams;
import org.craftercms.studio.model.search.SearchResult;

import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.craftercms.studio.permissions.PermissionResolverImpl.SITE_ID_RESOURCE_ID;
import static org.craftercms.studio.permissions.StudioPermissionsConstants.PERMISSION_CONTENT_SEARCH;

/**
 * Default implementation for {@link SearchService}
 *
 * @author joseross
 */
public class SearchServiceImpl implements SearchService {

    /**
     * The security service
     */
    protected final SecurityService securityService;

    /**
     * The internal search service
     */
    protected final SearchServiceInternal searchServiceInternal;

    protected final SiteService siteService;

    @ConstructorProperties({"securityService", "searchServiceInternal", "siteService"})
    public SearchServiceImpl(final SecurityService securityService, final SearchServiceInternal searchServiceInternal, final SiteService siteService) {
        this.securityService = securityService;
        this.searchServiceInternal = searchServiceInternal;
        this.siteService = siteService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @HasPermission(type = DefaultPermission.class, action = PERMISSION_CONTENT_SEARCH)
    public SearchResult search(@ProtectedResourceId(SITE_ID_RESOURCE_ID) final String siteId, final SearchParams params)
            throws AuthenticationException, ServiceLayerException {
        siteService.checkSiteExists(siteId);
        String user = securityService.getCurrentUser();
        if (isEmpty(user)) {
            throw new AuthenticationException("User is not authenticated");
        }
        // TODO: Get allowed paths from the security service
        List<String> allowedPaths = Collections.emptyList();
        return searchServiceInternal.search(siteId, allowedPaths, params);
    }

}

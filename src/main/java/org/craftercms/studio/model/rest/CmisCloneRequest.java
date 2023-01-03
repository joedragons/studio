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

package org.craftercms.studio.model.rest;

import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidateNoTagsParam;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.HTTPURI;
import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.SITE_ID;

public class CmisCloneRequest {
    @EsapiValidatedParam(type = SITE_ID)
    private String siteId;
    @ValidateNoTagsParam
    private String cmisRepoId;
    @EsapiValidatedParam(type = HTTPURI)
    private String cmisPath;
    @EsapiValidatedParam(type = HTTPURI)
    private String studioPath;

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getCmisRepoId() {
        return cmisRepoId;
    }

    public void setCmisRepoId(String cmisRepoId) {
        this.cmisRepoId = cmisRepoId;
    }

    public String getCmisPath() {
        return cmisPath;
    }

    public void setCmisPath(String cmisPath) {
        this.cmisPath = cmisPath;
    }

    public String getStudioPath() {
        return studioPath;
    }

    public void setStudioPath(String studioPath) {
        this.studioPath = studioPath;
    }
}

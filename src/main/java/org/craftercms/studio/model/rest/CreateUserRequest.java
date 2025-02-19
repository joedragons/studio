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

package org.craftercms.studio.model.rest;

import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.EMAIL;
import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.USERNAME;

/**
 * Holds the parameters for a Create User request
 */
public class CreateUserRequest {

    @NotBlank
    @Size(min = 5, max = 255)
    @EsapiValidatedParam(type = USERNAME)
    private String username;
    @NotBlank
    private String password;
    @Size(max = 32)
    private String firstName;
    @Size(max = 32)
    private String lastName;
    @EsapiValidatedParam(type = EMAIL)
    private String email;
    private boolean externallyManaged;
    private boolean enabled;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public boolean isExternallyManaged() {
        return externallyManaged;
    }

    public void setExternallyManaged(final boolean externallyManaged) {
        this.externallyManaged = externallyManaged;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}

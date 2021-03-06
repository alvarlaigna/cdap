/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.security.spi.authorization;

import co.cask.cdap.api.common.HttpErrorStatusProvider;
import co.cask.cdap.proto.security.Role;

import java.net.HttpURLConnection;

/**
 * Exception thrown when an {@link Role} already exists, this is deprecated please use {@link AlreadyExistsException}
 */
@Deprecated
public class RoleAlreadyExistsException extends Exception implements HttpErrorStatusProvider {
  public RoleAlreadyExistsException(Role role) {
    super(String.format("%s already exists.", role));
  }

  @Override
  public int getStatusCode() {
    return HttpURLConnection.HTTP_CONFLICT;
  }
}

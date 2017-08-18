/*
 * Copyright © 2014-2017 Cask Data, Inc.
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

package co.cask.cdap.internal.app.deploy.pipeline;

import co.cask.cdap.api.app.ApplicationSpecification;
import co.cask.cdap.data2.transaction.stream.StreamAdmin;
import co.cask.cdap.pipeline.AbstractStage;
import co.cask.cdap.proto.id.KerberosPrincipalId;
import co.cask.cdap.proto.id.NamespaceId;
import co.cask.cdap.security.impersonation.OwnerAdmin;
import co.cask.cdap.security.spi.authentication.AuthenticationContext;
import com.google.common.reflect.TypeToken;

/**
 * This {@link co.cask.cdap.pipeline.Stage} is responsible for automatic creation of any new streams specified by the
 * application. Additionally, it will enable exploration of those streams if exploration is enabled.
 */
public class CreateStreamsStage extends AbstractStage<ApplicationDeployable> {
  private final StreamCreator streamCreator;
  private final OwnerAdmin ownerAdmin;
  private final AuthenticationContext authenticationContext;

  public CreateStreamsStage(StreamAdmin streamAdmin, OwnerAdmin ownerAdmin,
                            AuthenticationContext authenticationContext) {
    super(TypeToken.of(ApplicationDeployable.class));
    this.streamCreator = new StreamCreator(streamAdmin);
    this.ownerAdmin = ownerAdmin;
    this.authenticationContext = authenticationContext;
  }

  /**
   * Receives an input containing application specification and location
   * and verifies both.
   *
   * @param input An instance of {@link ApplicationDeployable}
   */
  @Override
  public void process(ApplicationDeployable input) throws Exception {
    // create stream instances
    ApplicationSpecification specification = input.getSpecification();
    NamespaceId namespaceId = input.getApplicationId().getParent();
    KerberosPrincipalId ownerPrincipal = input.getOwnerPrincipal();
    // set the requesting user to be the app owner if it is present, otherwise set it to namespace owner
    String requestingUser =
      ownerPrincipal != null ? ownerPrincipal.getPrincipal() : ownerAdmin.getImpersonationPrincipal(namespaceId);
    // if namespace owner is still null set it to the current user who is making the call
    requestingUser = requestingUser == null ? authenticationContext.getPrincipal().getName() : requestingUser;
    streamCreator.createStreams(namespaceId, specification.getStreams().values(), ownerPrincipal, requestingUser);

    // Emit the input to next stage.
    emit(input);
  }
}

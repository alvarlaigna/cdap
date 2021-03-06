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

package co.cask.cdap.internal.app.services;

import co.cask.cdap.AllProgramsApp;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.dataset.lib.ObjectMappedTable;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.test.AppJarHelper;
import co.cask.cdap.internal.AppFabricTestHelper;
import co.cask.cdap.proto.Id;
import co.cask.cdap.proto.ProgramType;
import co.cask.cdap.proto.id.ApplicationId;
import co.cask.cdap.proto.id.EntityId;
import co.cask.cdap.proto.id.NamespaceId;
import co.cask.cdap.proto.security.Action;
import co.cask.cdap.proto.security.Principal;
import co.cask.cdap.security.authorization.AuthorizerInstantiator;
import co.cask.cdap.security.authorization.InMemoryAuthorizer;
import co.cask.cdap.security.spi.authentication.SecurityRequestContext;
import co.cask.cdap.security.spi.authorization.Authorizer;
import co.cask.cdap.security.spi.authorization.UnauthorizedException;
import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.twill.filesystem.LocalLocationFactory;
import org.apache.twill.filesystem.Location;
import org.apache.twill.filesystem.LocationFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Test authorization for ProgramLifeCycleService
 */
public class ProgramLifecycleServiceAuthorizationTest {

  @ClassRule
  public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();
  private static final Principal ALICE = new Principal("alice", Principal.PrincipalType.USER);

  private static CConfiguration cConf;
  private static Authorizer authorizer;
  private static AppFabricServer appFabricServer;
  private static ProgramLifecycleService programLifecycleService;

  @BeforeClass
  public static void setup() throws Exception {
    cConf = createCConf();
    Injector injector = AppFabricTestHelper.getInjector(cConf);
    authorizer = injector.getInstance(AuthorizerInstantiator.class).get();
    appFabricServer = injector.getInstance(AppFabricServer.class);
    appFabricServer.startAndWait();
    programLifecycleService = injector.getInstance(ProgramLifecycleService.class);
  }

  @Test
  public void testProgramList() throws Exception {
    SecurityRequestContext.setUserId(ALICE.getName());
    ApplicationId applicationId = NamespaceId.DEFAULT.app(AllProgramsApp.NAME);
    Map<EntityId, Set<Action>> neededPrivileges = ImmutableMap.<EntityId, Set<Action>>builder()
      .put(applicationId, EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.artifact(AllProgramsApp.class.getSimpleName(), "1.0-SNAPSHOT"), EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.dataset(AllProgramsApp.DATASET_NAME), EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.dataset(AllProgramsApp.DATASET_NAME2), EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.dataset(AllProgramsApp.DATASET_NAME3), EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.dataset(AllProgramsApp.DS_WITH_SCHEMA_NAME), EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.stream(AllProgramsApp.STREAM_NAME), EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.datasetType(KeyValueTable.class.getName()), EnumSet.of(Action.ADMIN))
      .put(NamespaceId.DEFAULT.datasetType(ObjectMappedTable.class.getName()), EnumSet.of(Action.ADMIN))
      .build();
    setUpPrivilegesAndExpectFailedDeploy(neededPrivileges);

    // now we should be able to deploy
    AppFabricTestHelper.deployApplication(Id.Namespace.DEFAULT, AllProgramsApp.class, null, cConf);

    // no auto grant now, the list will be empty for all program types
    for (ProgramType type : ProgramType.values()) {
      if (!type.equals(ProgramType.CUSTOM_ACTION) && !type.equals(ProgramType.WEBAPP)) {
        Assert.assertTrue(programLifecycleService.list(NamespaceId.DEFAULT, type).isEmpty());
      }
    }

    // no auto grant now, need to have privileges on the program to be able to see the programs
    authorizer.grant(applicationId.program(ProgramType.FLOW, AllProgramsApp.NoOpFlow.NAME), ALICE,
                     Collections.singleton(Action.EXECUTE));
    authorizer.grant(applicationId.program(ProgramType.SERVICE, AllProgramsApp.NoOpService.NAME), ALICE,
                     Collections.singleton(Action.EXECUTE));
    authorizer.grant(applicationId.program(ProgramType.WORKER, AllProgramsApp.NoOpWorker.NAME), ALICE,
                     Collections.singleton(Action.EXECUTE));
    authorizer.grant(applicationId.program(ProgramType.SPARK, AllProgramsApp.NoOpSpark.NAME), ALICE,
                     Collections.singleton(Action.EXECUTE));
    authorizer.grant(applicationId.program(ProgramType.MAPREDUCE, AllProgramsApp.NoOpMR.NAME), ALICE,
                     Collections.singleton(Action.EXECUTE));
    authorizer.grant(applicationId.program(ProgramType.MAPREDUCE, AllProgramsApp.NoOpMR2.NAME), ALICE,
                     Collections.singleton(Action.EXECUTE));
    authorizer.grant(applicationId.program(ProgramType.WORKFLOW, AllProgramsApp.NoOpWorkflow.NAME), ALICE,
                     Collections.singleton(Action.EXECUTE));

    for (ProgramType type : ProgramType.values()) {
      if (!type.equals(ProgramType.CUSTOM_ACTION) && !type.equals(ProgramType.WEBAPP)) {
        Assert.assertFalse(programLifecycleService.list(NamespaceId.DEFAULT, type).isEmpty());
        SecurityRequestContext.setUserId("bob");
        Assert.assertTrue(programLifecycleService.list(NamespaceId.DEFAULT, type).isEmpty());
        SecurityRequestContext.setUserId("alice");
      }
    }
  }

  @AfterClass
  public static void tearDown() {
    appFabricServer.stopAndWait();
  }

  private static CConfiguration createCConf() throws IOException {
    CConfiguration cConf = CConfiguration.create();
    cConf.setBoolean(Constants.Security.ENABLED, true);
    cConf.setBoolean(Constants.Security.Authorization.ENABLED, true);
    // we only want to test authorization, but we don't specify principal/keytab, so disable kerberos
    cConf.setBoolean(Constants.Security.KERBEROS_ENABLED, false);
    cConf.setInt(Constants.Security.Authorization.CACHE_MAX_ENTRIES, 0);
    LocationFactory locationFactory = new LocalLocationFactory(new File(TEMPORARY_FOLDER.newFolder().toURI()));
    Location authorizerJar = AppJarHelper.createDeploymentJar(locationFactory, InMemoryAuthorizer.class);
    cConf.set(Constants.Security.Authorization.EXTENSION_JAR_PATH, authorizerJar.toURI().getPath());
    // this is needed since now DefaultAuthorizationEnforcer expects this non-null
    cConf.set(Constants.Security.CFG_CDAP_MASTER_KRB_PRINCIPAL, UserGroupInformation.getLoginUser().getShortUserName());
    return cConf;
  }

  private void setUpPrivilegesAndExpectFailedDeploy(Map<EntityId, Set<Action>> neededPrivileges) throws Exception {
    int count = 0;
    for (Map.Entry<EntityId, Set<Action>> privilege : neededPrivileges.entrySet()) {
      authorizer.grant(privilege.getKey(), ALICE, privilege.getValue());
      count++;
      if (count < neededPrivileges.size()) {
        try {
          AppFabricTestHelper.deployApplication(Id.Namespace.DEFAULT, AllProgramsApp.class, null, cConf);
          Assert.fail();
        } catch (Exception e) {
          // expected
        }
      }
    }
  }
}

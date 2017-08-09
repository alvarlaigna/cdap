/*
 * Copyright © 2015 Cask Data, Inc.
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

package co.cask.cdap.api.workflow;

import co.cask.cdap.api.workflow.condition.ConditionSpecification;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents the CONDITION node in the {@link Workflow}.
 */
public class WorkflowConditionNode extends WorkflowNode {
  private final List<WorkflowNode> ifBranch;
  private final List<WorkflowNode> elseBranch;
  private final String predicateClassName;
  private final ConditionSpecification conditionSpecification;

  public WorkflowConditionNode(String nodeId, String predicateClassName, List<WorkflowNode> ifBranch,
                               List<WorkflowNode> elseBranch) {
    this(nodeId, ifBranch, elseBranch, predicateClassName, null);
  }

  public WorkflowConditionNode(String nodeId, ConditionSpecification conditionSpecification,
                               List<WorkflowNode> ifBranch, List<WorkflowNode> elseBranch) {
    this(nodeId, ifBranch, elseBranch, null, conditionSpecification);
  }

  private WorkflowConditionNode(String nodeId, List<WorkflowNode> ifBranch,
                                List<WorkflowNode> elseBranch,
                                @Nullable String predicateClassName,
                                @Nullable ConditionSpecification conditionSpecification) {
    super(nodeId, WorkflowNodeType.CONDITION);
    this.ifBranch = ifBranch;
    this.elseBranch = elseBranch;
    this.predicateClassName = predicateClassName;
    this.conditionSpecification = conditionSpecification;
  }

  public List<WorkflowNode> getIfBranch() {
    return ifBranch;
  }

  public List<WorkflowNode> getElseBranch() {
    return elseBranch;
  }

  @Nullable
  public String getPredicateClassName() {
    return predicateClassName;
  }

  @Nullable
  public ConditionSpecification getConditionSpecification() {
    return conditionSpecification;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("WorkflowConditionNode{");
    sb.append("nodeId=").append(nodeId);
    sb.append(", predicateClassName=").append(predicateClassName);
    sb.append(", conditionSpecification=").append(conditionSpecification);
    sb.append(", ifBranch=").append(ifBranch);
    sb.append(", elseBranch=").append(elseBranch);
    sb.append('}');
    return sb.toString();
  }
}

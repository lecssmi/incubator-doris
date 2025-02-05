// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.rules.exploration.join;

import org.apache.doris.nereids.operators.plans.logical.LogicalJoin;
import org.apache.doris.nereids.rules.Rule;
import org.apache.doris.nereids.rules.RuleType;
import org.apache.doris.nereids.rules.exploration.OneExplorationRuleFactory;
import org.apache.doris.nereids.trees.plans.GroupPlan;
import org.apache.doris.nereids.trees.plans.Plan;
import org.apache.doris.nereids.trees.plans.logical.LogicalBinaryPlan;


/**
 * Rule for busy-tree, exchange the children node.
 */
public class JoinExchange extends OneExplorationRuleFactory {
    /*
     *        topJoin                      newTopJoin
     *        /      \                      /      \
     *   leftJoin  rightJoin   -->   newLeftJoin newRightJoin
     *    /    \    /    \            /    \        /    \
     *   A      B  C      D          A      C      B      D
     */
    @Override
    public Rule<Plan> build() {
        return innerLogicalJoin(innerLogicalJoin(), innerLogicalJoin()).then(topJoin -> {
            LogicalBinaryPlan<LogicalJoin, GroupPlan, GroupPlan> leftJoin = topJoin.left();
            LogicalBinaryPlan<LogicalJoin, GroupPlan, GroupPlan> rightJoin = topJoin.right();

            GroupPlan a = leftJoin.left();
            GroupPlan b = leftJoin.right();
            GroupPlan c = rightJoin.left();
            GroupPlan d = rightJoin.right();

            Plan newLeftJoin = plan(
                    new LogicalJoin(leftJoin.operator.getJoinType(), leftJoin.operator.getCondition()),
                    a, c
            );
            Plan newRightJoin = plan(
                    new LogicalJoin(rightJoin.operator.getJoinType(), rightJoin.operator.getCondition()),
                    b, d
            );
            Plan newTopJoin = plan(
                    new LogicalJoin(topJoin.operator.getJoinType(), topJoin.operator.getCondition()),
                    newLeftJoin, newRightJoin
            );
            return newTopJoin;
        }).toRule(RuleType.LOGICAL_JOIN_EXCHANGE);
    }
}

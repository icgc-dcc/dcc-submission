package org.icgc.dcc.validation.visitor;

import java.util.Set;

import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.InternalFlowPlanningVisitor;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.plan.InternalPlanElement;
import org.icgc.dcc.validation.plan.PlanElement;

public class InternalRestrictionPlanningVisitor extends InternalFlowPlanningVisitor {

  private final Set<RestrictionType> restrictionTypes;

  public InternalRestrictionPlanningVisitor(Set<RestrictionType> restrictionTypes) {
    this.restrictionTypes = restrictionTypes;
  }

  @Override
  public void visit(Restriction restriction) {
    for(RestrictionType type : restrictionTypes) {
      if(type.builds(restriction.getType())) {
        PlanElement element = type.build(getCurrentField(), restriction);
        if(element.phase() == getPhase()) {
          collect((InternalPlanElement) element);
        }
      }
    }
  }

}

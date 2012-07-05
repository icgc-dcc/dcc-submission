package org.icgc.dcc.validation.visitor;

import java.util.Set;

import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.InternalFlowPlanningVisitor;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlanElement;
import org.icgc.dcc.validation.RestrictionType;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class InternalRestrictionPlanningVisitor extends InternalFlowPlanningVisitor {

  private final Set<RestrictionType> restrictionTypes;

  public InternalRestrictionPlanningVisitor(Set<RestrictionType> restrictionTypes) {
    this.restrictionTypes = Sets.filter(restrictionTypes, new Predicate<RestrictionType>() {

      @Override
      public boolean apply(RestrictionType input) {
        return input.flow() == FlowType.INTERNAL;
      }

    });
  }

  @Override
  public void visit(Restriction restriction) {
    for(RestrictionType type : restrictionTypes) {
      if(type.builds(restriction.getType())) {
        PlanElement element = type.build(getCurrentField(), restriction);
        collect((InternalPlanElement) element);
      }
    }
  }

}

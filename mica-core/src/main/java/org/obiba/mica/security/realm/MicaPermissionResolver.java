package org.obiba.mica.security.realm;

import eu.flatwhite.shiro.spatial.SingleSpaceRelationProvider;
import eu.flatwhite.shiro.spatial.SingleSpaceResolver;
import eu.flatwhite.shiro.spatial.SpatialPermissionResolver;
import eu.flatwhite.shiro.spatial.finite.NodeRelationProvider;
import eu.flatwhite.shiro.spatial.finite.NodeResolver;

public class MicaPermissionResolver extends SpatialPermissionResolver {

  public MicaPermissionResolver() {
    super(new SingleSpaceResolver(/*new SpatialRealm.RestSpace()*/null), new NodeResolver(),
        new SingleSpaceRelationProvider(new NodeRelationProvider()));
  }

}


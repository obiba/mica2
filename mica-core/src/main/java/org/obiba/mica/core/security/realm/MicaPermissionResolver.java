package org.obiba.mica.core.security.realm;

import org.springframework.stereotype.Component;

import eu.flatwhite.shiro.spatial.SingleSpaceRelationProvider;
import eu.flatwhite.shiro.spatial.SingleSpaceResolver;
import eu.flatwhite.shiro.spatial.SpatialPermissionResolver;
import eu.flatwhite.shiro.spatial.finite.NodeRelationProvider;
import eu.flatwhite.shiro.spatial.finite.NodeResolver;
import eu.flatwhite.shiro.spatial.finite.NodeSpace;

@Component
public class MicaPermissionResolver extends SpatialPermissionResolver {

  public MicaPermissionResolver() {
    super(new SingleSpaceResolver(new NodeSpace()), new NodeResolver(),
        new SingleSpaceRelationProvider(new NodeRelationProvider()));
  }

}


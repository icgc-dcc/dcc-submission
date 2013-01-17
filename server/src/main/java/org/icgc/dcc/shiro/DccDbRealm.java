package org.icgc.dcc.shiro;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.UserService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.User;

import com.google.common.collect.Lists;

public class DccDbRealm extends AuthorizingRealm implements DccRealm {

  private final UserService users;

  private final ProjectService projects;

  public DccDbRealm(UserService users, ProjectService projects) {
    this.users = users;
    this.projects = projects;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String username = (String) principals.getPrimaryPrincipal();
    User user = users.getUser(username);
    SimpleAuthorizationInfo sai = new SimpleAuthorizationInfo(new HashSet<String>(user.getRoles()));
    Set<String> stringPermissions = new HashSet<String>();
    for(Project project : projects.getProjects()) {
      if(project.hasUser(user.getName()) || CollectionUtils.containsAny(project.getGroups(), user.getRoles())) {
        stringPermissions.add(AuthorizationPrivileges.projectViewPrivilege(project.getKey()));
      }
    }
    sai.addStringPermissions(stringPermissions);

    return sai;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    // Authorization is currently only performed via the Shiro INI file, so this is not populated
    return null;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return false;
  }

  @Override
  public Collection<String> getRoles(String username) {
    User user = users.getUser(username);
    if(user == null) {
      return Lists.newArrayList();
    }
    return user.getRoles();
  }

}

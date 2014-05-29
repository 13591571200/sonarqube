/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.qualityprofile;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.ServerComponent;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class QProfileOperations implements ServerComponent {

  public static final String PROFILE_PROPERTY_PREFIX = "sonar.profile.";

  private final MyBatis myBatis;
  private final QualityProfileDao dao;
  private final ActiveRuleDao activeRuleDao;
  private final PropertiesDao propertiesDao;
  private final QProfileRepositoryExporter exporter;
  private final PreviewCache dryRunCache;
  private final QProfileLookup profileLookup;
  private final ProfilesManager profilesManager;

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, PropertiesDao propertiesDao,
                            QProfileRepositoryExporter exporter, PreviewCache dryRunCache, QProfileLookup profileLookup,
                            ProfilesManager profilesManager) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.propertiesDao = propertiesDao;
    this.exporter = exporter;
    this.dryRunCache = dryRunCache;
    this.profileLookup = profileLookup;
    this.profilesManager = profilesManager;
  }

  public QProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin, UserSession userSession) {
    DbSession session = myBatis.openSession(false);
    try {
      QProfile profile = newProfile(name, language, true, userSession, session);

      QProfileResult result = new QProfileResult();
      result.setProfile(profile);

      for (Map.Entry<String, String> entry : xmlProfilesByPlugin.entrySet()) {
        result.add(exporter.importXml(profile, entry.getKey(), entry.getValue(), session));
      }
      dryRunCache.reportGlobalModification(session);
      session.commit();
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QProfile newProfile(String name, String language, boolean failIfAlreadyExists, UserSession userSession, DbSession session) {
    return newProfile(name, language, null, failIfAlreadyExists, userSession, session);
  }

  public QProfile newProfile(String name, String language, @Nullable String parent, boolean failIfAlreadyExists, UserSession userSession, DbSession session) {
    checkPermission(userSession);
    if (failIfAlreadyExists) {
      checkNotAlreadyExists(name, language, session);
    }
    QualityProfileDto dto = new QualityProfileDto().setName(name).setLanguage(language).setParent(parent).setVersion(1).setUsed(false);
    dao.insert(session, dto);
    return QProfile.from(dto);
  }

  public void renameProfile(int profileId, String newName, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      QualityProfileDto profileDto = findNotNull(profileId, session);
      String oldName = profileDto.getName();

      QProfile profile = QProfile.from(profileDto);
      if (!oldName.equals(newName)) {
        checkNotAlreadyExists(newName, profile.language(), session);
      }
      profileDto.setName(newName);
      dao.update(session, profileDto);

      List<QProfile> children = profileLookup.children(profile, session);
      for (QProfile child : children) {
        dao.update(session, child.setParent(newName).toDto());
      }
      propertiesDao.updateProperties(PROFILE_PROPERTY_PREFIX + profile.language(), oldName, newName, session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteProfile(int profileId, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      QualityProfileDto profile = findNotNull(profileId, session);
      QProfile qProfile = QProfile.from(profile);
      if (!profileLookup.isDeletable(QProfile.from(profile), session)) {
        throw new BadRequestException("This profile can not be deleted");
      } else {
        deleteProfile(qProfile, session);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Delete profile without checking permission or that profile is existing or that profile can be deleted (is not defined as default, has no children, etc.)
   */
  public void deleteProfile(QProfile profile, DbSession session) {
    activeRuleDao.removeParamByProfile(session, profile);
    activeRuleDao.deleteByProfile(session, profile);
    dao.delete(profile.id(), session);
    propertiesDao.deleteProjectProperties(PROFILE_PROPERTY_PREFIX + profile.language(), profile.name(), session);
    //esActiveRule.deleteActiveRulesFromProfile(profile.id());
    dryRunCache.reportGlobalModification(session);
  }

  public void setDefaultProfile(int profileId, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      QualityProfileDto qualityProfile = findNotNull(profileId, session);
      propertiesDao.setProperty(new PropertyDto().setKey(PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()));
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateParentProfile(int profileId, @Nullable Integer parentId, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      QualityProfileDto profile = findNotNull(profileId, session);
      QualityProfileDto parentProfile = null;
      if (parentId != null) {
        parentProfile = findNotNull(parentId, session);
      }
      if (isCycle(profile, parentProfile, session)) {
        throw new BadRequestException("Please do not select a child profile as parent.");
      }
      String newParentName = parentProfile != null ? parentProfile.getName() : null;
      // Modification of inheritance has to be done before setting new parent name in order to be able to disable rules from old parent
      ProfilesManager.RuleInheritanceActions actions = profilesManager.profileParentChanged(profile.getId(), newParentName, userSession.name());
      profile.setParent(newParentName);
      dao.update(session, profile);
      session.commit();

      //esActiveRule.deleteActiveRules(actions.idsToDelete());
      //esActiveRule.bulkIndexActiveRuleIds(actions.idsToIndex(), session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void copyProfile(int profileId, String copyProfileName, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      QualityProfileDto profileDto = findNotNull(profileId, session);
      checkNotAlreadyExists(copyProfileName, profileDto.getLanguage(), session);
      int copyProfileId = profilesManager.copyProfile(profileId, copyProfileName);

      // Cannot reuse same session as hibernate as create active rules in another session
//      esActiveRule.bulkIndexProfile(copyProfileId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  boolean isCycle(QualityProfileDto childProfile, @Nullable QualityProfileDto parentProfile, DbSession session) {
    QualityProfileDto currentParent = parentProfile;
    while (currentParent != null) {
      if (childProfile.getName().equals(currentParent.getName())) {
        return true;
      }
      currentParent = getParent(currentParent, session);
    }
    return false;
  }

  @CheckForNull
  private QualityProfileDto getParent(QualityProfileDto profile, DbSession session) {
    if (profile.getParent() != null) {
      return dao.selectParent(profile.getId(), session);
    }
    return null;
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private QualityProfileDto findNotNull(int profileId, DbSession session) {
    QualityProfileDto profile = dao.selectById(profileId, session);
    QProfileValidations.checkProfileIsNotNull(profile);
    return profile;
  }

  private void checkNotAlreadyExists(String name, String language, DbSession session) {
    if (dao.selectByNameAndLanguage(name, language, session) != null) {
      throw BadRequestException.ofL10n("quality_profiles.profile_x_already_exists", name);
    }
  }

}

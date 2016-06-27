/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.portlet.social;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.json.JSONException;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.social.webui.space.UISpaceSearch;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * UI component to list all spaces that is associated with current logged-in
 * user: public spaces to join, invitation spaces to accept or deny to join, his
 * spaces in which he is a member or manager to access or manage.
 * 
 * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
 * @since Aug 18, 2011
 * @since 1.2.2
 */
@ComponentConfig(template = "app:/groovy/social/portlet/relatedSpacesPortlet/UIManageRelatedSpaces.gtmpl", events = {
    @EventConfig(listeners = UIManageRelatedSpaces.RelateSpaceActionListener.class),
    @EventConfig(listeners = UIManageRelatedSpaces.UnRelateSpaceActionListener.class),
    @EventConfig(listeners = UIManageRelatedSpaces.SearchRelatedActionListener.class),
    @EventConfig(listeners = UIManageRelatedSpaces.SearchUnRelatedActionListener.class),
    @EventConfig(listeners = UIManageRelatedSpaces.LoadMoreSpaceActionListener.class) })
public class UIManageRelatedSpaces extends UIContainer {

  public static final String  SEARCH_ALL              = "All";

  private static final String SPACE_SEARCH            = "SpaceSearch";

  private static final Log    LOG                     = ExoLogger.getLogger(UIManageRelatedSpaces.class);

  private static final String SPACE_DELETED_INFO      = "UIManageAllSpaces.msg.DeletedInfo";

  private static final String MEMBERSHIP_REMOVED_INFO = "UIManageAllSpaces.msg.MemberShipRemovedInfo";

  private static final String MSG_WARNING_LEAVE_SPACE = "UIManageAllSpaces.msg.warning_leave_space";

  private static final String INVITATION_REVOKED_INFO = "UIManageAllSpaces.msg.RevokedInfo";

  private static final String spacePath               = "production/soc:spaces/soc:";

  private static final String ALL_SPACES_STATUS       = "all_spaces";

  private final Integer       SPACES_PER_PAGE         = 20;

  private SpaceService        spaceService            = null;

  private String              userId                  = null;

  private List<Space>         spaces;                                                                    // for
                                                                                                         // search
                                                                                                         // result

  private UISpaceSearch       uiSpaceSearch           = null;

  private boolean             loadAtEnd               = false;

  private boolean             hasUpdatedSpace         = false;

  private int                 currentLoadIndex;

  private boolean             enableLoadNext;

  private int                 loadingCapacity;

  private String              spaceNameSearch;

  private List<Space>         relatedSpacesList;

  private List<Space>         unRelatedSpacesList;

  private List<Space>         allSpacesList;

  private ListAccess<Space>   spacesListAccess;

  private int                 spacesNum;

  private String              selectedChar            = null;

  /**
   * Constructor to initialize iterator.
   *
   * @throws Exception if an exception occurred
   */
  public UIManageRelatedSpaces() throws Exception {
    uiSpaceSearch = createUIComponent(UISpaceSearch.class, null, "UISpaceSearch");
    uiSpaceSearch.setTypeOfRelation(ALL_SPACES_STATUS);
    addChild(uiSpaceSearch);
    init();
  }

  /**
   * Gets type of one given space of current user.
   *
   * @param space the space Object
   * @return possible object is
   *     {@link String }
   */
  protected static String getTypeOfSpace(Space space) {
    String currentUserId = Utils.getOwnerIdentity().getRemoteId();
    SpaceService spaceService = Utils.getSpaceService();

    if (spaceService.isInvitedUser(space, currentUserId)) { // Received
      return TypeOfSpace.INVITED.toString();
    } else if (spaceService.isPendingUser(space, currentUserId)) { // Sent
      return TypeOfSpace.SENT.toString();
    } else if (spaceService.isMember(space, currentUserId)) { // Member
      if (spaceService.isManager(space, currentUserId)) {
        return TypeOfSpace.MANAGER.toString(); // Manager
      }
      return TypeOfSpace.MEMBER.toString(); // Is member
    }

    return TypeOfSpace.NONE.toString(); // No relationship with this space.
  }

  private static Session getSession() throws RepositoryException {
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = sessionProvider.getSession("social", manageableRepository);
    return session;
  }

  private static void addRelatedSpaces(Space spaceID1, Space spaceID2) throws JSONException, IOException {
    try {
      Node spaceNode1 = getSession().getRootNode().getNode(spacePath + spaceID1.getPrettyName());
      Node spaceNode2 = getSession().getRootNode().getNode(spacePath + spaceID2.getPrettyName());

      RelationsService relateService = WCMCoreUtils.getService(RelationsService.class);
      relateService.addRelation(spaceNode1, spaceNode2.getPath(), "social");
      relateService.addRelation(spaceNode2, spaceNode1.getPath(), "social");

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private static void removeRelatedSpaces(Space spaceID1, Space spaceID2) throws JSONException, IOException {
    try {
      Node spaceNode1 = getSession().getRootNode().getNode(spacePath + spaceID1.getPrettyName());
      Node spaceNode2 = getSession().getRootNode().getNode(spacePath + spaceID2.getPrettyName());

      RelationsService relateService = WCMCoreUtils.getService(RelationsService.class);
      relateService.removeRelation(spaceNode1, spaceNode2.getPath());
      relateService.removeRelation(spaceNode2, spaceNode1.getPath());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Inits at the first loading.
   */
  public void init() {
    try {
      setHasUpdatedSpace(false);
      setLoadAtEnd(false);
      enableLoadNext = false;
      currentLoadIndex = 0;
      loadingCapacity = SPACES_PER_PAGE;
      relatedSpacesList = new ArrayList<Space>();
      unRelatedSpacesList = new ArrayList<Space>();
      allSpacesList = new ArrayList<Space>();
      setSpacesList(loadSpaces(currentLoadIndex, loadingCapacity));
      setSelectedChar(SEARCH_ALL);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Sets loading capacity.
   *
   * @param loadingCapacity
   *     allowed object is
   *     {@link int }
   */
  public void setLoadingCapacity(int loadingCapacity) {
    this.loadingCapacity = loadingCapacity;
  }

  /**
   * Gets flag to display LoadNext button or not.
   *
   * @return the enableLoadNext
   */
  public boolean isEnableLoadNext() {
    return enableLoadNext;
  }

  /**
   * Sets flag to display LoadNext button or not.
   *
   * @param enableLoadNext the enableLoadNext to set
   */
  public void setEnableLoadNext(boolean enableLoadNext) {
    this.enableLoadNext = enableLoadNext;
  }

  /**
   * Gets flags to clarify that load at the last space or not.
   *
   * @return the loadAtEnd
   */
  public boolean isLoadAtEnd() {
    return loadAtEnd;
  }

  /**
   * Sets flags to clarify that load at the last space or not.
   *
   * @param loadAtEnd the loadAtEnd to set
   */
  public void setLoadAtEnd(boolean loadAtEnd) {
    this.loadAtEnd = loadAtEnd;
  }

  /**
   * Gets information that clarify one space is updated or not.
   *
   * @return the hasUpdatedSpace
   */
  public boolean isHasUpdatedSpace() {
    return hasUpdatedSpace;
  }

  /**
   * Sets information that clarify one space is updated or not.
   *
   * @param hasUpdatedSpace the hasUpdatedSpace to set
   */
  public void setHasUpdatedSpace(boolean hasUpdatedSpace) {
    this.hasUpdatedSpace = hasUpdatedSpace;
  }

  /**
   * Gets list of related space.
   *
   * @return the spacesList
   * possible object is
   *     {@link Space }
   * @throws Exception
   */
  public List<Space> getRelatedSpacesList() throws Exception {
    updateRelatedUnrelatedSpaces();
    return this.relatedSpacesList;
  }

  /**
   * Sets list of related spaces.
   *
   * @param spacesList the spacesList to set
   */
  public void setRelatedSpacesList(List<Space> spacesList) {
    this.relatedSpacesList = spacesList;
  }

  /**
   * Gets list of unRelated space.
   *
   * @return the spacesList
   * @throws Exception if an exception occurred
   */
  public List<Space> getUnRelatedSpacesList() throws Exception {
    updateRelatedUnrelatedSpaces();
    setEnableLoadNext((this.unRelatedSpacesList.size() >= SPACES_PER_PAGE) && (this.unRelatedSpacesList.size() < getSpacesNum()));

    return this.unRelatedSpacesList;
  }

  /**
   * Sets list of all type of space.
   *
   * @param spacesList the spacesList to set
   */
  public void setSpacesList(List<Space> spacesList) {
    this.allSpacesList = spacesList;
  }

  /**
   * Sets list of unRelated spaces.
   *
   * @param allSpacesList
   *     allowed object is
   *     {@link Space }
   * @param relatedSpacesList
   *     allowed object is
   *     {@link Space }
   */
  public void setUnRelatedSpacesList(List<Space> allSpacesList, List<Space> relatedSpacesList) {
    this.unRelatedSpacesList.clear();
    for (Space aSpace : allSpacesList) {
      int i = 0;
      for (Space rSpace : relatedSpacesList) {
        if (rSpace.getId() == aSpace.getId()) {
          break;
        }
        i++;
      }

      if (i == relatedSpacesList.size()) {
        this.unRelatedSpacesList.add(aSpace);
      }
    }
  }

  /**
   * Gets number of spaces for displaying.
   *
   * @return the spacesNum
   */
  public int getSpacesNum() {
    return spacesNum;
  }

  /**
   * Sets number of spaces for displaying.
   *
   * @param spacesNum the spacesNum to set
   */
  public void setSpacesNum(int spacesNum) {
    this.spacesNum = spacesNum;
  }

  /**
   * Gets selected character.
   *
   * @return Character is selected.
   */
  public String getSelectedChar() {
    return selectedChar;
  }

  /**
   * Sets selected character.
   *
   * @param selectedChar A {@code String}
   */
  public void setSelectedChar(String selectedChar) {
    this.selectedChar = selectedChar;
  }

  /**
   * Gets name of searched space.
   *
   * @return the spaceNameSearch
   */
  public String getSpaceNameSearch() {
    return spaceNameSearch;
  }

  /**
   * Sets name of searched space.
   *
   * @param spaceNameSearch the spaceNameSearch to set
   */
  public void setSpaceNameSearch(String spaceNameSearch) {
    this.spaceNameSearch = spaceNameSearch;
  }

  /**
   * Gets spaces with ListAccess type.
   *
   * @return the spacesListAccess
   */
  public ListAccess<Space> getSpacesListAccess() {
    return spacesListAccess;
  }

  /**
   * Sets spaces with ListAccess type.
   *
   * @param spacesListAccess the spacesListAccess to set
   */
  public void setSpacesListAccess(ListAccess<Space> spacesListAccess) {
    this.spacesListAccess = spacesListAccess;
  }

  /**
   * Loads more space.
   *
   * @throws Exception Exception if an exception occurred
   */
  public void loadNext() throws Exception {
    currentLoadIndex += loadingCapacity;
    if (currentLoadIndex <= getSpacesNum()) {
      this.unRelatedSpacesList.addAll(new ArrayList<Space>(Arrays.asList(getSpacesListAccess().load(currentLoadIndex,
                                                                                                    loadingCapacity))));
    }
  }

  /**
   * Loads space when searching.
   *
   * @throws Exception Exception if an exception occurred
   */
  public void loadSearch() throws Exception {
    currentLoadIndex = 0;
    setSpacesList(loadSpaces(currentLoadIndex, loadingCapacity));
    updateRelatedUnrelatedSpaces();
  }

  protected boolean isSuperUser(Space space) {
    String currentUserId = Utils.getOwnerIdentity().getRemoteId();
    SpaceService spaceService = Utils.getSpaceService();

    return spaceService.hasSettingPermission(space, currentUserId);
  }

  private void updateRelatedUnrelatedSpaces() throws SpaceException, IOException, JSONException {
    setRelatedSpacesList(getRelatedSpaces(getCurrentSpace().getPrettyName()));
    setUnRelatedSpacesList(allSpacesList, relatedSpacesList);
  }

  private List<Space> loadSpaces(int index, int length) throws Exception {
    String charSearch = getSelectedChar();
    String searchCondition = uiSpaceSearch.getSpaceNameSearch();
    String userId = Util.getPortalRequestContext().getRemoteUser();

    if (SEARCH_ALL.equals(charSearch) || (charSearch == null && searchCondition == null)) {
      setSpacesListAccess(getSpaceService().getVisibleSpacesWithListAccess(userId, null));
    } else if (searchCondition != null) {
      setSpacesListAccess(getSpaceService().getVisibleSpacesWithListAccess(userId, new SpaceFilter(searchCondition)));
    } else if (charSearch != null) {
      setSpacesListAccess(getSpaceService().getVisibleSpacesWithListAccess(userId, new SpaceFilter(charSearch.charAt(0))));
    }

    ArrayList<Space> spaceList = filterSelfSpace(getSpacesListAccess(), index, length);

    setSpacesNum(spaceList.size());
    uiSpaceSearch.setSpaceNum(getSpacesNum());
    return spaceList;
  }

  private ArrayList<Space> filterSelfSpace(ListAccess<Space> spacesListAccess, int index, int length) throws Exception {

    ArrayList<Space> spaceList = new ArrayList<Space>();
    Space[] spaces = spacesListAccess.load(index, length);
    for (Space aSpace : spaces) {
      if (aSpace.getId() != getCurrentSpace().getId()) {
        spaceList.add(aSpace);
      }
    }
    return spaceList;
  }

  /**
   * Checks if the remote user has edit permission of a space.
   *
   * @param space allowed object is
   *     {@link Space }
   * @return true or false
   * @throws Exception if an exception occurred
   */
  public boolean hasEditPermission(Space space) throws Exception {
    return spaceService.hasSettingPermission(space, getUserId());
  }

  /**
   * Check if the remote user has access permission.
   *
   * @param space
   * @return
   *     possible object is
   *     {@link Space }
   * @throws Exception if an exception occurred
   */
  protected boolean hasAccessPermission(Space space) throws Exception {
    return spaceService.hasAccessPermission(space, getUserId());
  }

  /**
   * Gets image source url.
   *
   * @param space
   *     allowed object is
   *     {@link Space }
   * @return image source url
   * @throws Exception if an exception occurred
   */
  public String getImageSource(Space space) throws Exception {
    return space.getAvatarUrl();
  }

  /**
   * Gets space list.
   *
   * @return space list
   */
  public List<Space> getSpaces() {
    return spaces;
  }

  /**
   * Sets space lists.
   *
   * @param spaces
   *     allowed object is
   *     {@link Space }
   */
  public void setSpaces(List<Space> spaces) {
    this.spaces = spaces;
  }

  /**
   * Gets current remote user.
   *
   * @return remote user
   */
  private String getUserId() {
    if (userId == null) {
      userId = Util.getPortalRequestContext().getRemoteUser();
    }
    return userId;
  }

  /**
   * Gets spaceService.
   *
   * @return spaceService
   * @see org.exoplatform.social.core.space.spi.SpaceService
   */
  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = getApplicationComponent(SpaceService.class);
    }
    return spaceService;
  }

  /**
   * gets space, space identified by the url.
   *
   * @return space
   *     possible object is
   *     {@link Space }
   * @throws org.exoplatform.social.core.space.SpaceException if an exception occurred
   */
  public Space getCurrentSpace() throws SpaceException {
    String spaceUrl = Utils.getSpaceUrlByContext();
    return getSpaceService().getSpaceByUrl(spaceUrl);
  }

  private List<Space> getRelatedSpaces(String spaceID1) throws IOException, JSONException {
    List<Space> spaceList = new ArrayList<Space>();

    try {
      Node node = getSession().getRootNode().getNode(spacePath + spaceID1);

      RelationsService relateService = WCMCoreUtils.getService(RelationsService.class);

      List<Node> relations = relateService.getRelations(node, SessionProvider.createSystemProvider());
      for (Node relation : relations) {
        Space currentSpace = getSpaceService().getSpaceByPrettyName(relation.getProperty("soc:name").getValue().getString());
        if (null != currentSpace) {
          spaceList.add(currentSpace);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return spaceList;
  }

  public enum TypeOfSpace {
    INVITED, SENT, NONE, MEMBER, MANAGER
  }

  /**
   * Listeners loading more space action.
   *
   * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
   * @since Aug 18, 2011
   */
  static public class LoadMoreSpaceActionListener extends EventListener<UIManageRelatedSpaces> {
    public void execute(Event<UIManageRelatedSpaces> event) throws Exception {
      UIManageRelatedSpaces uiManageRelatedSpaces = event.getSource();
      if (uiManageRelatedSpaces.currentLoadIndex > uiManageRelatedSpaces.spacesNum) {
        return;
      }
      uiManageRelatedSpaces.loadNext();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManageRelatedSpaces);
    }
  }

  /**
   * Listens event that broadcast from UISpaceSearch.
   *
   * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
   * @since Aug 19, 2011
   */
  static public class SearchRelatedActionListener extends EventListener<UIManageRelatedSpaces> {
    @Override
    public void execute(Event<UIManageRelatedSpaces> event) throws Exception {
      UIManageRelatedSpaces uiManageRelatedSpaces = event.getSource();
      WebuiRequestContext ctx = event.getRequestContext();
      String charSearch = ctx.getRequestParameter(OBJECTID);

      if (charSearch == null) {
        uiManageRelatedSpaces.setSelectedChar(null);
      } else {
        ResourceBundle resApp = ctx.getApplicationResourceBundle();
        String defaultSpaceNameAndDesc = resApp.getString(uiManageRelatedSpaces.getId() + ".label.DefaultSpaceNameAndDesc");
        ((UIFormStringInput) uiManageRelatedSpaces.uiSpaceSearch.getUIStringInput(SPACE_SEARCH)).setValue(defaultSpaceNameAndDesc);
        uiManageRelatedSpaces.setSelectedChar(charSearch);
        uiManageRelatedSpaces.uiSpaceSearch.setSpaceNameSearch(null);
      }

      uiManageRelatedSpaces.loadSearch();
      uiManageRelatedSpaces.setLoadAtEnd(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManageRelatedSpaces);
    }
  }

  /**
   * Listens event that broadcast from UISpaceSearch.
   *
   * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
   * @since Aug 19, 2011
   */
  static public class SearchUnRelatedActionListener extends EventListener<UIManageRelatedSpaces> {
    @Override
    public void execute(Event<UIManageRelatedSpaces> event) throws Exception {
      UIManageRelatedSpaces uiManageRelatedSpaces = event.getSource();
      WebuiRequestContext ctx = event.getRequestContext();
      String charSearch = ctx.getRequestParameter(OBJECTID);

      if (charSearch == null) {
        uiManageRelatedSpaces.setSelectedChar(null);
      } else {
        ResourceBundle resApp = ctx.getApplicationResourceBundle();
        String defaultSpaceNameAndDesc = resApp.getString(uiManageRelatedSpaces.getId() + ".label.DefaultSpaceNameAndDesc");
        ((UIFormStringInput) uiManageRelatedSpaces.uiSpaceSearch.getUIStringInput(SPACE_SEARCH)).setValue(defaultSpaceNameAndDesc);
        uiManageRelatedSpaces.setSelectedChar(charSearch);
        uiManageRelatedSpaces.uiSpaceSearch.setSpaceNameSearch(null);
      }

      uiManageRelatedSpaces.loadSearch();
      uiManageRelatedSpaces.setLoadAtEnd(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManageRelatedSpaces);
    }
  }

  /**
   * This action is triggered when user click on RelateSpace.
   * The leaving space will remove that user in the space.
   * If that user is the only leader -> can't not leave that space.
   */
  static public class RelateSpaceActionListener extends EventListener<UIManageRelatedSpaces> {
    public void execute(Event<UIManageRelatedSpaces> event) throws Exception {
      UIManageRelatedSpaces uiManageRelatedCommunities = event.getSource();
      String curentSpaceId = uiManageRelatedCommunities.getCurrentSpace().getId();
      SpaceService spaceService = uiManageRelatedCommunities.getSpaceService();
      WebuiRequestContext ctx = event.getRequestContext();
      UIApplication uiApp = ctx.getUIApplication();
      String spaceId = ctx.getRequestParameter(OBJECTID);
      addRelatedSpaces(spaceService.getSpaceById(curentSpaceId), spaceService.getSpaceById(spaceId));
      uiManageRelatedCommunities.hasUpdatedSpace = true;

      uiManageRelatedCommunities.updateRelatedUnrelatedSpaces();
    }
  }

  /**
   * This action is triggered when user click on UnRelateSpace.
   * The leaving space will remove that user in the space.
   * If that user is the only leader -> can't not leave that space.
   */
  static public class UnRelateSpaceActionListener extends EventListener<UIManageRelatedSpaces> {
    public void execute(Event<UIManageRelatedSpaces> event) throws Exception {
      UIManageRelatedSpaces uiManageRelatedCommunities = event.getSource();
      String curentSpaceId = uiManageRelatedCommunities.getCurrentSpace().getId();
      SpaceService spaceService = uiManageRelatedCommunities.getSpaceService();
      spaceService.getSpaceByDisplayName("");
      WebuiRequestContext ctx = event.getRequestContext();
      UIApplication uiApp = ctx.getUIApplication();
      String spaceId = ctx.getRequestParameter(OBJECTID);
      removeRelatedSpaces(spaceService.getSpaceById(curentSpaceId), spaceService.getSpaceById(spaceId));
      uiManageRelatedCommunities.hasUpdatedSpace = true;

      uiManageRelatedCommunities.updateRelatedUnrelatedSpaces();
    }
  }
}

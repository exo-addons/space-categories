package org.exoplatform.social.webui.space;


import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.MissingResourceException;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 18, 2006
 * 2:28:18 PM
 */
@ComponentConfig(template = "war:groovy/social/webui/space/UISpaceCategoriesAddedList.gtmpl",
        events = {
                @EventConfig(listeners = UISpaceCategoriesAddedList.DeleteActionListener.class,
                        confirm = "UICategoriesAddedList.msg.confirm-delete") })
public class UISpaceCategoriesAddedList extends UIContainer implements UISelectable {

    private UIPageIterator uiPageIterator_;

    private static final Log LOG = ExoLogger.getLogger(UISpaceCategoriesAddedList.class.getName());

    public UISpaceCategoriesAddedList() throws Exception {
        uiPageIterator_ = addChild(UIPageIterator.class, null, "CategoriesAddedList");
    }

    public void init() throws Exception {
        LazyPageList<Object> objPageList = new LazyPageList<Object>((ListAccess<Object>) new ListAccessImpl<Object>(Object.class, getListCategories()), 10);
        uiPageIterator_.setPageList(objPageList);
        getUIPageIterator().setCurrentPage(1);
    }

    public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

    public List<Object> getListCategories() throws Exception {
        UISpaceCategory uiSpaceCategory = getAncestorOfType(UISpaceCategory.class);
        return NodeLocation.getNodeListByLocationList(uiSpaceCategory.getCategories());
    }

    public void updateGrid(int currentPage) throws Exception {
        UISpaceCategory uiSpaceCategory = getAncestorOfType(UISpaceCategory.class);
        ListAccess<Object> categoryList = new ListAccessImpl<Object>(Object.class,
                NodeLocation.getLocationsByNodeList(uiSpaceCategory.getCategories()));
        LazyPageList<Object> objPageList = new LazyPageList<Object>(categoryList, 10);
        uiPageIterator_.setPageList(objPageList);
        if (currentPage > getUIPageIterator().getAvailablePage())
            getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
        else
            getUIPageIterator().setCurrentPage(currentPage);
    }

    String displayCategory(Node node, List<Node> taxonomyTrees) {
        try {
            for (Node taxonomyTree : taxonomyTrees) {
                if (node.getPath().contains(taxonomyTree.getPath())) {
                    return getCategoryLabel(node.getPath().replace(taxonomyTree.getPath(), taxonomyTree.getName()));
                }
            }
        } catch (RepositoryException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unexpected error when ");
            }
        }
        return "";
    }

    private String getCategoryLabel(String resource) {
        String[] taxonomyPathSplit = resource.split("/");
        StringBuilder buildlabel;
        StringBuilder buildPathlabel = new StringBuilder();
        for (int i = 0; i < taxonomyPathSplit.length; i++) {
            buildlabel = new StringBuilder("eXoTaxonomies");
            for (int j = 0; j <= i; j++) {
                buildlabel.append(".").append(taxonomyPathSplit[j]);
            }
            try {
                buildPathlabel.append(Utils.getResourceBundle(buildlabel.append(".label").toString())).append("/");
            } catch (MissingResourceException me) {
                buildPathlabel.append(taxonomyPathSplit[i]).append("/");
            }
        }
        return buildPathlabel.substring(0, buildPathlabel.length() - 1);
    }

    @SuppressWarnings("unused")
    public void doSelect(String selectField, Object value) throws Exception {
        UISpaceCategory uiSpaceCategory = getAncestorOfType(UISpaceCategory.class);
        UIOneTaxonomySelector uiOneTaxonomySelector = uiSpaceCategory.getChild(UIOneTaxonomySelector.class);
        String rootTaxonomyName = uiOneTaxonomySelector.getRootTaxonomyName();
        TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
        try {
            Node currentNode = uiSpaceCategory.getCurrentNode();
            if (rootTaxonomyName.equals(value)) {
                taxonomyService.addCategory(currentNode, rootTaxonomyName, "");
            } else {
                String[] arrayCategoryPath = String.valueOf(value.toString()).split("/");
                StringBuffer categoryPath = new StringBuffer().append("/");
                for(int i = 1; i < arrayCategoryPath.length; i++ ) {
                    categoryPath.append(arrayCategoryPath[i]);
                    categoryPath.append("/");
                }
                taxonomyService.addCategory(currentNode, rootTaxonomyName, categoryPath.toString());
            }
            uiSpaceCategory.getCurrentNode().save() ;
            uiSpaceCategory.getSocialSession().save() ;
            updateGrid(1) ;

            NodeLocation location = NodeLocation.getNodeLocationByNode(currentNode);
            WCMComposer composer = WCMCoreUtils.getService(WCMComposer.class);

        } catch (AccessDeniedException accessDeniedException) {
            throw new MessageException(new ApplicationMessage("AccessControlException.msg",
                    null,
                    ApplicationMessage.WARNING));
        } catch (ItemExistsException item) {
            throw new MessageException(new ApplicationMessage("UICategoriesAddedList.msg.ItemExistsException",
                    null,
                    ApplicationMessage.WARNING));
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unexpected error", e);
            }
            JCRExceptionManager.process(getAncestorOfType(UIApplication.class), e);
        }
    }

    static public class DeleteActionListener extends EventListener<UISpaceCategoriesAddedList> {
        public void execute(Event<UISpaceCategoriesAddedList> event) throws Exception {
            UISpaceCategoriesAddedList uiAddedList = event.getSource();
            UIContainer uiManager = uiAddedList.getParent();
            UIApplication uiApp = uiAddedList.getAncestorOfType(UIApplication.class);
            String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
            UISpaceCategory uiSpaceCategory = uiAddedList.getAncestorOfType(UISpaceCategory.class);
            Node currentNode = uiSpaceCategory.getCurrentNode();
            TaxonomyService taxonomyService =
                    uiAddedList.getApplicationComponent(TaxonomyService.class);

            try {
                List<Node> listNode = uiSpaceCategory.getAllTaxonomyTrees();
                for(Node itemNode : listNode) {
                    if(nodePath.contains(itemNode.getPath())) {
                        taxonomyService.removeCategory(currentNode, itemNode.getName(),
                                nodePath.substring(itemNode.getPath().length()));
                        break;
                    }
                }
                uiAddedList.updateGrid(uiAddedList.getUIPageIterator().getCurrentPage());
            } catch(AccessDeniedException ace) {
                throw new MessageException(new ApplicationMessage("UICategoriesAddedList.msg.access-denied",
                        null, ApplicationMessage.WARNING)) ;
            } catch(Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Unexpected error", e);
                }
                JCRExceptionManager.process(uiApp, e);
            }
            //uiManager.setRenderedChild("UISpaceCategoriesAddedList");
        }
    }
}

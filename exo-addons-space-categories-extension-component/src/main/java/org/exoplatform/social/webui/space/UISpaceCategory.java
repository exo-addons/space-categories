package org.exoplatform.social.webui.space;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIContainer;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 18, 2006
 * 2:12:26 PM
 */
@ComponentConfigs(
        {
                @ComponentConfig(
                        template = "war:groovy/social/webui/space/UISpaceCategory.gtmpl"
                ),
                @ComponentConfig(
                        type =UIOneTaxonomySelector.class, id = "uiOneTaxonomySelector",
                        template = "classpath:groovy/ecm/webui/UIOneTaxonomySelector.gtmpl"
                ),
                @ComponentConfig(
                        type =UISpaceCategoriesAddedList.class, id = "uiSpaceCategoriesAddedList",
                        template = "war:groovy/social/webui/space/UISpaceCategoriesAddedList.gtmpl",events={@EventConfig(listeners = UISpaceCategoriesAddedList.DeleteActionListener.class,
                        confirm = "UICategoriesAddedList.msg.confirm-delete")}
                ),
                @ComponentConfig(
                        type = UIBreadcumbs.class, id="BreadcumbOneTaxonomy",
                        template="system:/groovy/webui/core/UIBreadcumbs.gtmpl", events={@EventConfig(listeners={UIBreadcumbs.SelectPathActionListener.class})}
                )
        }
)

public class UISpaceCategory extends UIContainer implements UISelectable {

    /** The space service */
    private SpaceService spaceService;

    /** The taxonomies field. */
    private static final String FIELD_TAXONOMY = "categories";

    private static final String SPACE_PATH = "production/soc:spaces/soc:";

    private  UISpaceCategoriesAddedList uiSpaceCategoriesAddedList;

    /** The list taxonomy. */
    private List<String> listTaxonomy = new ArrayList<String>();

    /** The list taxonomy name. */
    private List<String> listTaxonomyName = new ArrayList<String>();

    private HashMap<String, List<String>> mapTaxonomies = new HashMap<String, List<String>>();

    public UISpaceCategory() throws Exception {
        initOneTaxonomySelector();
        uiSpaceCategoriesAddedList.init();

    }

    public void initOneTaxonomySelector() throws Exception {
        UIOneTaxonomySelector uiOneTaxonomySelector = addChild(UIOneTaxonomySelector.class, "uiOneTaxonomySelector", "uiOneTaxonomySelector");
        uiSpaceCategoriesAddedList = addChild(UISpaceCategoriesAddedList.class, "uiSpaceCategoriesAddedList", "uiSpaceCategoriesAddedList");
        NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
        String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
        Session session = WCMCoreUtils.getUserSessionProvider()
                .getSession(getWorkspaceName(), getRepository());
        Node rootTree = (Node) session.getItem(rootTreePath);
        NodeIterator childrenIterator = rootTree.getNodes();
        while (childrenIterator.hasNext()) {
            Node childNode = childrenIterator.nextNode();
            rootTreePath = childNode.getPath();
            break;
        }
        uiOneTaxonomySelector.setRootNodeLocation(getRepository().getConfiguration().getName(), getWorkspaceName(), rootTreePath);
        uiOneTaxonomySelector.setExceptedNodeTypesInPathPanel(new String[] { "exo:symlink" });
        uiOneTaxonomySelector.init(WCMCoreUtils.getUserSessionProvider());
        String param = "returnField=" + FIELD_TAXONOMY;
        uiOneTaxonomySelector.setSourceComponent(this, new String[] { param });
    }

    public Space getCurrentSpace()
    {
        String spaceUrl = SpaceUtils.getSpaceUrlByContext();
        return getApplicationComponent(SpaceService.class).getSpaceByUrl(spaceUrl);
    }

    public Node getCurrentNode() throws Exception {
         return getSocialSession().getRootNode().getNode(SPACE_PATH + getCurrentSpace().getPrettyName());
    }

    public ManageableRepository getRepository() throws Exception {
        RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
        return   repositoryService.getCurrentRepository();
    }

    public String getWorkspaceName() throws Exception {
        DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
        return   dmsConfiguration.getConfig().getSystemWorkspace();
    }

    public Session getSocialSession() throws Exception {
        SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
        Session session = sessionProvider.getSession("social", getRepository());
        return session;
    }

    public List<Node> getCategories() throws Exception {
        List<Node> listCategories = new ArrayList<Node>();
        TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
        List<Node> listNode = getAllTaxonomyTrees();
        for(Node itemNode : listNode) {
            listCategories.addAll(taxonomyService.getCategories(getCurrentNode(), itemNode.getName()));
        }
        return listCategories;
    }

    List<Node> getAllTaxonomyTrees() throws RepositoryException {
        TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
        return taxonomyService.getAllTaxonomyTrees();
    }

    @Override
    public void doSelect(String selectField, Object value) throws Exception {
        uiSpaceCategoriesAddedList.doSelect(selectField, value);
    }
}

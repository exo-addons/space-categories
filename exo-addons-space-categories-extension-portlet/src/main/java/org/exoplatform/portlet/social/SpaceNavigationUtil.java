package org.exoplatform.portlet.social;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.Utils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by gregorysebert on 20/06/14.
 */
public class SpaceNavigationUtil {

    private static final String categoriesPath = "repository:social:/SpacesCategories/spaces-categories";
    private static final String symlinkNodeType = "exo:symlink";
    private static final String taxonomyNodeType = "exo:taxonomy";
    private static final String portalContainerName =    PortalContainer.getCurrentPortalContainerName();

    public SpaceNavigationUtil(){
    }

    private static TaxonomyService getTaxonomyService()
    {
        TaxonomyService taxonomyService = (TaxonomyService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(TaxonomyService.class);
        return taxonomyService;
    }


    private static SpaceService getSpaceService()
    {
        SpaceService spaceService = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
        return spaceService;
    }

    private static String getSpaceUrl(Space space)
    {

        String spaceURL = Utils.getSpaceHomeURL(space);

        if (spaceURL.contains(portalContainerName)) {
            spaceURL = spaceURL.substring(portalContainerName.length() + 2);
        }
        String fullUrl = Util.getPortalRequestContext().getRequest().getRequestURL().toString();
        String subUrl = StringUtils.substringBefore(fullUrl, Util.getPortalRequestContext().getRequest().getRequestURI());
        subUrl = subUrl + "/" + portalContainerName;
        String applicationDisplayed = "";
        String constructURL = fullUrl.substring(subUrl.length() + 1);
        if (fullUrl.contains(":spaces")) {
            int count = StringUtils.countMatches(constructURL, "/");
            if (count == 2) {
                subUrl = subUrl + "/" + spaceURL;
            } else {
                applicationDisplayed = constructURL.substring(constructURL.lastIndexOf("/"));
                subUrl = subUrl + "/" + spaceURL + applicationDisplayed;
            }
        }
        else {
            subUrl = subUrl + "/" + spaceURL;
        }
        return subUrl;
    }



    private static Session getSession() throws RepositoryException {
        Node rootNode = NodeLocation.getNodeByLocation(NodeLocation.getNodeLocationByExpression(categoriesPath));
        RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
        ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
        SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
        Session session = sessionProvider.getSession("social", manageableRepository);
        return session;
    }

    private static Space retrieveSpace(String spacePrettyName, String remoteUser)
    {
        Space currentSpace = getSpaceService().getSpaceByPrettyName(spacePrettyName);
        if (getSpaceService().hasAccessPermission(currentSpace,remoteUser))
        {
            return currentSpace;
        }
        return null;
     }


    private static List<SpaceNavigationBean> getSpaces(Node categoryNode) {
        List<SpaceNavigationBean> spaces = new LinkedList<SpaceNavigationBean>();
        try {
        QueryManager manager = getSession().getWorkspace().getQueryManager();
        Query query = manager.createQuery("SELECT * FROM " + symlinkNodeType + " WHERE jcr:path LIKE '" + categoryNode.getPath() + "/%'", Query.SQL);
        NodeIterator nodes = query.execute().getNodes();

         while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            Space space = retrieveSpace(node.getName().replace("soc:",""),Util.getPortalRequestContext().getRemoteUser());

            if (space != null) {
                String spaceImageSource = space.getAvatarUrl();
                String link = getSpaceUrl(space);
                if (spaceImageSource == null){
                    spaceImageSource=  LinkProvider.SPACE_DEFAULT_AVATAR_URL;
                }
                SpaceNavigationBean spaceBean = new SpaceNavigationBean(space.getPrettyName(), space.getDisplayName(), link , spaceImageSource);
                spaces.add(spaceBean);
            }
        }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return spaces;
    }

    public static List<CategoryBean> getCategories(String tree) {
        List<CategoryBean> categories = new LinkedList<CategoryBean>();
        try {
            Node taxonomyTree = getTaxonomyService().getTaxonomyTree(tree);
            NodeIterator catNodes = taxonomyTree.getNodes();

            if (catNodes != null) {
                while (catNodes.hasNext()){
                    Node node = catNodes.nextNode();
                    if (node.isNodeType(taxonomyNodeType)) {
                        CategoryBean cat = new CategoryBean(node.getName(), node.getName(), node.getPath(), "");
                        List<SpaceNavigationBean> spaces = getSpaces(node);
                        if (spaces != null && spaces.size() > 0) {
                            cat.setChilds(spaces);
                        }
                        categories.add(cat);
                    }
                }
            }
        }catch(RepositoryException e){
                e.printStackTrace();
            }
        return categories;
    }

    public static List<Node> getTaxonomyTreeList() {
        try {
            return getTaxonomyService().getAllTaxonomyTrees();
        } catch (RepositoryException e) {
            e.printStackTrace();
            return null;
        }
    }

}

/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exoplatform.portlet.social;

import juzu.Action;
import juzu.Path;
import juzu.View;
import juzu.bridge.portlet.JuzuPortlet;
import juzu.request.RenderContext;
import juzu.template.Template;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;
import java.io.IOException;
import java.util.List;

public class SpaceCategoryNavigation {

    @Inject
    PortletPreferences portletPreferences;

    @Inject
    @Path("edit.gtmpl")
    Template editTemplate;

    @Inject
    @Path("index.gtmpl")
    Template index;

    @Inject
    SpaceNavigationUtil SpaceNavigationUtil;

    private List<CategoryBean> categories;
    private List<Node> categoryTreeList;
    private String categoryTree;

    @View
    public void index(RenderContext renderContext) throws IOException
    {
        PortletMode portletMode = renderContext.getProperty(JuzuPortlet.PORTLET_MODE);
        if (portletMode.equals(PortletMode.VIEW))
        {
            categoryTree = portletPreferences.getValue("categoryTree", "SpacesCategories");
            categories = SpaceNavigationUtil.getCategories(categoryTree);
            index.with().set("categoryTree", categoryTree).set("categories", categories).render();
        }
        else
        {
            categoryTree = portletPreferences.getValue("categoryTree", "SpacesCategories");
            categoryTreeList = SpaceNavigationUtil.getTaxonomyTreeList();
            editTemplate.with().set("categoryTree", categoryTree).set("categoryTreeList", categoryTreeList).render();
        }
    }

    @Action
    public void save(String categoryTree)
    {
        try {
            portletPreferences.setValue("categoryTree", categoryTree);
            portletPreferences.store();
        } catch (ReadOnlyException e) {
        } catch (ValidatorException e) {
        } catch (IOException e) {
        }
    }

}

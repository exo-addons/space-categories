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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import juzu.Action;
import juzu.Path;
import juzu.Response;
import juzu.View;
import juzu.template.Template;

public class SpaceCategoryNavigation {

  @Inject
  PortletPreferences         portletPreferences;

  @Inject
  @Path("edit.gtmpl")
  Template                   editTemplate;

  @Inject
  @Path("index.gtmpl")
  Template                   index;

  @Inject
  SpaceNavigationUtil        SpaceNavigationUtil;

  private List<CategoryBean> categories;

  private List<Node>         categoryTreeList;

  private String             categoryTree;

  @View
  public Response index() throws IOException {
    // FIXME: Upgrade the code from Juzu 0.6 to Juzu 1.1
    PortletMode portletMode = PortletMode.VIEW;
    if (portletMode.equals(PortletMode.VIEW)) {
      categoryTree = portletPreferences.getValue("categoryTree", "SpacesCategories");
      categories = SpaceNavigationUtil.getCategories(categoryTree);
      return index.with().set("categoryTree", categoryTree).set("categories", categories).ok();
    } else {
      categoryTree = portletPreferences.getValue("categoryTree", "SpacesCategories");
      categoryTreeList = SpaceNavigationUtil.getTaxonomyTreeList();
      return editTemplate.with().set("categoryTree", categoryTree).set("categoryTreeList", categoryTreeList).ok();
    }
  }

  @Action
  public void save(String categoryTree) {
    try {
      portletPreferences.setValue("categoryTree", categoryTree);
      portletPreferences.store();
    } catch (ReadOnlyException e) {
    } catch (ValidatorException e) {
    } catch (IOException e) {
    }
  }

}

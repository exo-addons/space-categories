/*
 * Copyright 2013 eXo Platform SAS Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

@Application(defaultController = SpaceCategoryNavigation.class)
@Portlet(name = "SocialNavigationPortlet")
@Stylesheets(location = AssetLocation.APPLICATION, value = {
    @Stylesheet(value="/org/exoplatform/portlet/social/assets/UISpaceNavigationPortlet.css", location=juzu.asset.AssetLocation.APPLICATION, id="SpaceCategoryNavigation")})
@Less(value = { "UISpaceNavigationPortlet.less" }, minify = true)
@Assets({"*"})

package org.exoplatform.portlet.social;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.asset.Stylesheets;
import juzu.plugin.less.Less;
import juzu.plugin.portlet.Portlet;

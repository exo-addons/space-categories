package org.exoplatform.portlet.social;

import java.util.List;

public class CategoryBean {
  String                    name;

  String                    path;

  String                    title;

  String                    url;

  String                    avatarURL;

  List<SpaceNavigationBean> childs;

  public CategoryBean(String name, String title, String url, String avatarURL) {
    this.name = name;
    this.title = title;
    this.url = url;
    this.avatarURL = avatarURL;
    this.childs = null;
  }

  public String getTitle() {
    return this.title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAvatarURL() {
    return this.avatarURL;
  }

  public void setAvatarURL(String avatarURL) {
    this.avatarURL = avatarURL;
  }

  public String getUrl() {
    return this.url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<SpaceNavigationBean> getChilds() {
    return this.childs;
  }

  public void setChilds(List<SpaceNavigationBean> childs) {
    this.childs = childs;
  }

  public boolean hasChilds() {
    return (this.childs != null) && (this.childs.size() > 0);
  }
}

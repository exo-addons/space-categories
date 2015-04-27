package org.exoplatform.portlet.social;

import java.util.List;

public class SpaceNavigationBean
{
    String name;
    String title;
    String url;
    String avatarURL;

    List<CategoryBean> childs;

public SpaceNavigationBean(String name, String title, String url, String avatarURL)
    {
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
}

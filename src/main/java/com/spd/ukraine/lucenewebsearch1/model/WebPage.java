/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spd.ukraine.lucenewebsearch1.model;

import com.spd.ukraine.lucenewebsearch1.web.validators.WebPageValidatorAnnotation;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author sf
 */
@WebPageValidatorAnnotation
@Entity
@Table(name = "web_page")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "WebPage.findAll", query = "SELECT w FROM WebPage w"),
    @NamedQuery(name = "WebPage.findByUrl", query = "SELECT w FROM WebPage w WHERE w.url = :url")})
public class WebPage implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "url")
    private String url;
    private String title;
    @Lob
    @Column(name = "content")
    private String content;

    public WebPage() {
    }

    public WebPage(String url) {
        this.url = url;
    }

    public String getPlainUrl() {
        return url.replace("\\", "");
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (url != null ? url.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof WebPage)) {
            return false;
        }
        WebPage other = (WebPage) object;
        if ((this.url == null && other.url != null) 
                || (this.url != null && !this.url.equals(other.url))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.spd.ukraine.lucenewebsearch1.model.WebPage[ url=" + url + " ]";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
}

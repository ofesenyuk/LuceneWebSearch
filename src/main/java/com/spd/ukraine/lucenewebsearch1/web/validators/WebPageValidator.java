/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spd.ukraine.lucenewebsearch1.web.validators;

import com.spd.ukraine.lucenewebsearch1.model.WebPage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 *
 * @author sf
 */
public class WebPageValidator 
    implements ConstraintValidator<WebPageValidatorAnnotation, Object> {

    @Override
    public void initialize(WebPageValidatorAnnotation a) {
        
    }

    @Override
    public boolean isValid(Object t, ConstraintValidatorContext cvc) {
        WebPage webPage = (WebPage) t;
        String url = webPage.getUrl();
        if (null == url || url.trim().isEmpty()) {
            System.out.println("void url");
            return false;
        }
        try {
            BufferedReader in
                    = new BufferedReader(new InputStreamReader((new URL(url))
                            .openStream()));
        } catch (IOException ex) {
            System.out.println("IOException in isValid " + ex.getMessage());
            return false;
        }
        return true;
    }
    
}

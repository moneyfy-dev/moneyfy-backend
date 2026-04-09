package com.referidos.app.segurosref.helpers;

import java.util.HashMap;
import java.util.Map;

// Clase implementada para imitar comportamiento de BindingResult, en caso de que la data,
// no pueda recuperarse como: Content-Type: "application/json"
public class BindingHelper {

    private Map<String, Object> errors;
    
    public BindingHelper() {
        this.errors = new HashMap<>();
    }

    public Map<String, Object> getData() {
        return errors;
    }
    public void setData(Map<String, Object> errors) {
        this.errors = errors;
    }
    public void addError(String field, Object message) {
        this.errors.put(field, "The field " + field + " " + message);
    }
    public boolean findErrors() {
        return (this.errors.size() != 0);
    }

}

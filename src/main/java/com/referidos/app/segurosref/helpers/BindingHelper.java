package com.referidos.app.segurosref.helpers;

import java.util.HashMap;
import java.util.Map;

// Clase implementada para imitar comportamiento de BindingResult, en caso de que la data,
// no pueda recuperarse como: Content-Type: "application/json"
public class BindingHelper {

    private Map<String, Object> data;
    private boolean error;
    
    public BindingHelper() {
        this.data = new HashMap<>();
    }

    public Map<String, Object> getData() {
        return data;
    }
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    public boolean isError() {
        return error;
    }

    public void addError(String field, Object message) {
        this.data.put(field, "The field " + field + " " + message);
    }
    public void validateData() {
        this.error = (this.data.size() != 0); // No hay error, si no hay data. Si hay data, se agrego un error
    }

}

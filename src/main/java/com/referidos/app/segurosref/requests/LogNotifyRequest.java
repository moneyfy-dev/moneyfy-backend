package com.referidos.app.segurosref.requests;

import java.util.List;

public record LogNotifyRequest(
    String key,
    List<String> logs
) {

}

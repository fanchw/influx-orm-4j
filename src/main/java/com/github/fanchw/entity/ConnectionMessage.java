package com.github.fanchw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ConnectionMessage {
    private String userName;

    private String password;

    private String url;

    private String database;

    private String retentionPolicy;
}

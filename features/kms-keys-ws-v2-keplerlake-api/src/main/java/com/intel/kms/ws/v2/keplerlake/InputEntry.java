/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.kms.ws.v2.keplerlake;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author nallux
 */
public class InputEntry {
   /*file_id and key_id removed in m8*/ 
  /*@JsonProperty("file_id")
  private String fileId;
  
   @JsonProperty("key_id")
  private String keyId;*/
   @JsonProperty("path")
  private String path;

   /* public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }*/
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
 
    
}
